package it.musina.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfiguration {
    @Bean OpenAPI musinaOpenApi() {
        var csrf = new Parameter().in("header").name("X-CSRF-TOKEN").required(true)
                .description("Token recuperato da GET /api/auth/csrf").schema(new StringSchema());
        var login = new Operation().summary("Autentica l'utente e ruota la sessione/CSRF")
                .security(List.of()).addParametersItem(csrf)
                .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                        "application/x-www-form-urlencoded", new MediaType().schema(new ObjectSchema()
                                .addProperty("username", new StringSchema())
                                .addProperty("password", new StringSchema().format("password"))
                                .required(List.of("username", "password"))))))
                .responses(new ApiResponses().addApiResponse("204", response("Login riuscito"))
                        .addApiResponse("401", response("Credenziali non valide"))
                        .addApiResponse("403", response("CSRF assente o non valido"))
                        .addApiResponse("429", response("Troppi tentativi")));
        var logout = new Operation().summary("Invalida la sessione corrente").addParametersItem(csrf)
                .responses(new ApiResponses().addApiResponse("204", response("Logout completato"))
                        .addApiResponse("403", response("CSRF assente o non valido")));
        return new OpenAPI().info(new Info().title("Musina — bootstrap").version("0.1.0"))
                .components(new Components().addSecuritySchemes("session", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")))
                .addSecurityItem(new SecurityRequirement().addList("session"))
                .paths(new Paths().addPathItem("/api/auth/login", new PathItem().post(login))
                        .addPathItem("/api/auth/logout", new PathItem().post(logout)));
    }

    private static ApiResponse response(String description) { return new ApiResponse().description(description); }
}
