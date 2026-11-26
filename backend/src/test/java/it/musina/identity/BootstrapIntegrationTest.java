package it.musina.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.import=", "server.address=127.0.0.1",
        "MUSINA_BOOTSTRAP_USERNAME=bootstrap.test",
        "MUSINA_BOOTSTRAP_PASSWORD=synthetic-bootstrap-password",
        "MUSINA_BOOTSTRAP_FAMILY=Famiglia sintetica bootstrap"
})
class BootstrapIntegrationTest {
    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18.7")
            .withDatabaseName("musina_bootstrap_test")
            .withUsername("musina_test").withPassword("synthetic-test-only-password")
            .withLabel("it.musina.purpose", "bootstrap-tests");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
    }

    @LocalServerPort int port;
    @Autowired IdentityRepository repository;
    @Autowired IdentityService service;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcClient jdbc;
    @Autowired Flyway flyway;
    @Autowired ObjectMapper json;
    @Autowired LoginThrottle throttle;

    @AfterEach void clearThrottle() { throttle.succeeded("127.0.0.1"); }

    @Test void migrationsAndProvisioningAreRepeatableWithoutResettingCredentials() throws Exception {
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        long usersBefore = jdbc.sql("SELECT count(*) FROM app_users").query(Long.class).single();
        assertThat(service.bootstrap("different.admin", "different-synthetic-password", "Non creare")).isFalse();
        assertThat(jdbc.sql("SELECT count(*) FROM app_users").query(Long.class).single()).isEqualTo(usersBefore);
        assertThat(repository.findByUsername("different.admin")).isEmpty();
        Browser browser = new Browser();
        assertThat(browser.login("bootstrap.test", "synthetic-bootstrap-password").statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/connection").statusCode()).isEqualTo(200);
    }

    @Test void loginRequiresCsrfAndLogoutInvalidatesTheSession() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.get("/api/connection").statusCode()).isEqualTo(401);
        assertThat(browser.postForm("/api/auth/logout", "", true).statusCode()).isEqualTo(401);
        assertThat(browser.postForm("/api/auth/login", "username=bootstrap.test&password=synthetic-bootstrap-password", false)
                .statusCode()).isEqualTo(403);
        var login = browser.login("bootstrap.test", "synthetic-bootstrap-password");
        assertThat(login.statusCode()).isEqualTo(204);
        assertThat(login.headers().allValues("set-cookie").toString()).contains("HttpOnly", "SameSite=Lax");
        JsonNode connected = json.readTree(browser.get("/api/connection").body());
        assertThat(connected.get("username").asText()).isEqualTo("bootstrap.test");
        assertThat(connected.toString()).doesNotContain("password", "hash");
        assertThat(browser.postForm("/api/auth/logout", "", false).statusCode()).isEqualTo(403);
        assertThat(browser.postForm("/api/auth/logout", "", true).statusCode()).isEqualTo(204);
        assertThat(browser.get("/api/connection").statusCode()).isEqualTo(401);
    }

    @Test void membersCannotManageAccessAndFamiliesAreIsolated() throws Exception {
        Fixture own = fixture("MEMBER");
        Fixture other = fixture("ADMIN");
        Browser member = new Browser();
        assertThat(member.login(own.username(), own.password()).statusCode()).isEqualTo(204);
        assertThat(member.get("/api/connection").statusCode()).isEqualTo(200);
        assertThat(member.get("/api/families/" + own.family()).statusCode()).isEqualTo(200);
        var foreign = member.get("/api/families/" + other.family());
        assertThat(foreign.statusCode()).isEqualTo(404);
        assertThat(foreign.body()).doesNotContain(other.username(), other.family().toString());
        assertThat(member.get("/api/admin/users").statusCode()).isEqualTo(403);
        assertThat(member.postJson("/api/admin/users", Map.of("username", "forbidden.user", "password",
                "synthetic-password", "role", "MEMBER"), true).statusCode()).isEqualTo(403);
    }

    @Test void administratorCreatesOnlyInOwnFamilyAndValidationDoesNotLeakPasswords() throws Exception {
        Fixture admin = fixture("ADMIN");
        Fixture other = fixture("ADMIN");
        Browser browser = new Browser();
        browser.login(admin.username(), admin.password());
        String username = "created." + UUID.randomUUID();
        Map<String, String> payload = Map.of("username", username, "password", "synthetic-created-password", "role", "MEMBER");
        assertThat(browser.postJson("/api/admin/users", payload, false).statusCode()).isEqualTo(403);
        var created = browser.postJson("/api/admin/users", payload, true);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).doesNotContain("password", "hash");
        assertThat(repository.findByUsername(username).orElseThrow().familyId()).isEqualTo(admin.family());
        assertThat(passwords.matches("synthetic-created-password",
                repository.findByUsername(username).orElseThrow().passwordHash())).isTrue();
        assertThat(browser.postJson("/api/admin/users", payload, true).statusCode()).isEqualTo(409);
        var invalid = browser.postJson("/api/admin/users", Map.of("username", "invalid.test", "password", "bad", "role", "MEMBER"), true);
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.body()).doesNotContain("bad", "rejectedValue", "stackTrace");
        assertThat(browser.get("/api/admin/users").body()).doesNotContain(other.username());
        assertThat(browser.postForm("/api/admin/users/" + other.user() + "/disable", "", true).statusCode()).isEqualTo(404);
    }

    @Test void disablingAnAccessRevokesAnExistingSessionAndCannotDisableSelf() throws Exception {
        Fixture admin = fixture("ADMIN");
        UUID userId = UUID.randomUUID();
        String memberName = "member." + UUID.randomUUID();
        repository.createUser(userId, memberName, passwords.encode("synthetic-member-password"), admin.family(), "MEMBER");
        Browser member = new Browser();
        member.login(memberName, "synthetic-member-password");
        assertThat(member.get("/api/connection").statusCode()).isEqualTo(200);
        Browser browser = new Browser();
        browser.login(admin.username(), admin.password());
        assertThat(browser.postForm("/api/admin/users/" + admin.user() + "/disable", "", true).statusCode()).isEqualTo(409);
        assertThat(browser.postForm("/api/admin/users/" + userId + "/disable", "", true).statusCode()).isEqualTo(204);
        assertThat(member.get("/api/connection").statusCode()).isEqualTo(401);
        assertThat(new Browser().login(memberName, "synthetic-member-password").statusCode()).isEqualTo(401);
    }

    @Test void repeatedFailedLoginsAreLimitedWithoutRevealingAccountExistence() throws Exception {
        Browser browser = new Browser();
        for (int i = 0; i < 10; i++) {
            var failed = browser.login("does.not.exist", "synthetic-wrong-password");
            assertThat(failed.statusCode()).isEqualTo(401);
            assertThat(failed.body()).contains("Credenziali non valide").doesNotContain("does.not.exist");
        }
        assertThat(browser.login("bootstrap.test", "synthetic-bootstrap-password").statusCode()).isEqualTo(429);
    }

    @Test void openApiIsProtectedAndUnknownApiDoesNotReturnSpaHtml() throws Exception {
        Browser browser = new Browser();
        assertThat(browser.get("/api/openapi").statusCode()).isEqualTo(401);
        browser.login("bootstrap.test", "synthetic-bootstrap-password");
        var contract = browser.get("/api/openapi");
        assertThat(contract.statusCode()).isEqualTo(200);
        JsonNode paths = json.readTree(contract.body()).get("paths");
        assertThat(paths.has("/api/auth/login")).isTrue();
        assertThat(paths.has("/api/auth/logout")).isTrue();
        assertThat(paths.has("/api/connection")).isTrue();
        assertThat(paths.get("/api/admin/users").get("post").get("responses").has("201")).isTrue();
        assertThat(paths.get("/api/admin/users").get("post").get("responses").has("403")).isTrue();
        assertThat(paths.get("/api/auth/csrf").get("get").get("security").isEmpty()).isTrue();
        var unknown = browser.get("/api/does-not-exist");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(unknown.body()).doesNotContain("<!doctype", "<html");
        var htmlAccept = browser.client.send(browser.request("/api/does-not-exist")
                .header("Accept", "text/html").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(htmlAccept.statusCode()).isEqualTo(404);
        assertThat(htmlAccept.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        Browser anonymous = new Browser();
        assertThat(anonymous.postJson("/api/auth/register", Map.of(), true).statusCode()).isEqualTo(401);
    }

    private Fixture fixture(String role) {
        UUID family = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        String username = "synthetic." + UUID.randomUUID();
        String password = "synthetic-fixture-password";
        repository.createFamily(family, "Famiglia sintetica " + family);
        repository.createUser(user, username, passwords.encode(password), family, role);
        return new Fixture(family, user, username, password);
    }

    private record Fixture(UUID family, UUID user, String username, String password) { }

    private class Browser {
        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER).build();

        HttpResponse<String> get(String path) throws Exception {
            return client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> login(String username, String password) throws Exception {
            return postForm("/api/auth/login", "username=" + encode(username) + "&password=" + encode(password), true);
        }

        HttpResponse<String> postForm(String path, String body, boolean includeCsrf) throws Exception {
            return send(path, body, "application/x-www-form-urlencoded", includeCsrf);
        }

        HttpResponse<String> postJson(String path, Object body, boolean includeCsrf) throws Exception {
            return send(path, json.writeValueAsString(body), "application/json", includeCsrf);
        }

        private HttpResponse<String> send(String path, String body, String type, boolean includeCsrf) throws Exception {
            var builder = request(path).header("Content-Type", type);
            if (includeCsrf) {
                JsonNode token = json.readTree(get("/api/auth/csrf").body());
                builder.header(token.get("headerName").asText(), token.get("token").asText());
            }
            return client.send(builder.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }

        private HttpRequest.Builder request(String path) {
            return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(20));
        }

        private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    }
}
