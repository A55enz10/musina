package it.musina.identity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class IdentityRepository {
    private final JdbcClient jdbc;
    private static final String PRINCIPAL_QUERY = """
            SELECT u.id, u.username, u.password_hash, u.enabled,
                   m.family_id, m.role, f.name AS family_name
            FROM app_users u JOIN family_memberships m ON m.user_id = u.id
            JOIN families f ON f.id = m.family_id
            """;

    public IdentityRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Optional<MusinaPrincipal> findByUsername(String username) {
        return jdbc.sql(PRINCIPAL_QUERY + " WHERE u.username = :username")
                .param("username", username).query(IdentityRepository::principal).optional();
    }

    public Optional<MusinaPrincipal> findById(UUID id) {
        return jdbc.sql(PRINCIPAL_QUERY + " WHERE u.id = :id")
                .param("id", id).query(IdentityRepository::principal).optional();
    }

    public boolean hasUsers() {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM app_users)").query(Boolean.class).single();
    }

    public void lockBootstrap() {
        jdbc.sql("SELECT pg_advisory_xact_lock(4102026)").query((rs, row) -> true).single();
    }

    public void createFamily(UUID id, String name) {
        jdbc.sql("INSERT INTO families(id, name) VALUES (:id, :name)")
                .param("id", id).param("name", name).update();
    }

    public void createUser(UUID id, String username, String hash, UUID familyId, String role) {
        jdbc.sql("INSERT INTO app_users(id, username, password_hash) VALUES (:id, :name, :hash)")
                .param("id", id).param("name", username).param("hash", hash).update();
        jdbc.sql("INSERT INTO family_memberships(user_id, family_id, role) VALUES (:id, :family, :role)")
                .param("id", id).param("family", familyId).param("role", role).update();
    }

    public List<UserView> listUsers(UUID familyId) {
        return jdbc.sql("""
                SELECT u.id, u.username, u.enabled, m.role FROM app_users u
                JOIN family_memberships m ON m.user_id = u.id
                WHERE m.family_id = :family ORDER BY u.username
                """).param("family", familyId).query((rs, row) -> new UserView(
                        rs.getObject("id", UUID.class), rs.getString("username"),
                        rs.getString("role"), rs.getBoolean("enabled"))).list();
    }

    public boolean disableUser(UUID target, UUID familyId) {
        return jdbc.sql("""
                UPDATE app_users SET enabled = FALSE WHERE id = :id
                AND EXISTS(SELECT 1 FROM family_memberships m WHERE m.user_id = app_users.id
                AND m.family_id = :family)
                """).param("id", target).param("family", familyId).update() == 1;
    }

    public Optional<FamilyView> familyForUser(UUID familyId, UUID userId) {
        return jdbc.sql("""
                SELECT f.id, f.name FROM families f JOIN family_memberships m ON m.family_id = f.id
                WHERE f.id = :family AND m.user_id = :user
                """).param("family", familyId).param("user", userId)
                .query((rs, row) -> new FamilyView(rs.getObject("id", UUID.class), rs.getString("name")))
                .optional();
    }

    public void audit(UUID actor, UUID target, String action) {
        jdbc.sql("INSERT INTO identity_audit(actor_id, target_id, event_type) VALUES (:actor,:target,:action)")
                .param("actor", actor).param("target", target).param("action", action).update();
    }

    private static MusinaPrincipal principal(ResultSet rs, int row) throws SQLException {
        return new MusinaPrincipal(rs.getObject("id", UUID.class), rs.getString("username"),
                rs.getString("password_hash"), rs.getObject("family_id", UUID.class),
                rs.getString("family_name"), rs.getString("role"), rs.getBoolean("enabled"));
    }

    public record UserView(UUID id, String username, String role, boolean enabled) { }
    public record FamilyView(UUID id, String name) { }
}
