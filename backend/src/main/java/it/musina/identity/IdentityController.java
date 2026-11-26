package it.musina.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class IdentityController {
    private final IdentityRepository repository;
    private final IdentityService identity;

    public IdentityController(IdentityRepository repository, IdentityService identity) {
        this.repository = repository;
        this.identity = identity;
    }

    @GetMapping("/auth/csrf")
    @SecurityRequirements
    @Operation(summary = "Recupera il token CSRF, anche prima del login")
    public CsrfView csrf(CsrfToken token) { return new CsrfView(token.getToken(), token.getHeaderName()); }

    @GetMapping("/connection")
    @Operation(summary = "Verifica il collegamento e il contesto familiare autenticato")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Collegamento verificato"),
            @ApiResponse(responseCode = "401", description = "Accesso richiesto")})
    public ConnectionView connection(@AuthenticationPrincipal MusinaPrincipal user) {
        return new ConnectionView("Collegamento al backend verificato", user.username(),
                user.familyId(), user.familyName(), user.role());
    }

    @GetMapping("/families/{id}")
    @Operation(summary = "Legge solo la famiglia alla quale appartiene l'utente")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Famiglia corrente"),
            @ApiResponse(responseCode = "401", description = "Accesso richiesto"),
            @ApiResponse(responseCode = "404", description = "Famiglia non trovata o non appartenente")})
    public IdentityRepository.FamilyView family(@PathVariable UUID id, @AuthenticationPrincipal MusinaPrincipal user) {
        return repository.familyForUser(id, user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Famiglia non trovata"));
    }

    @GetMapping("/admin/users")
    @Operation(summary = "Elenca gli utenti della propria famiglia, solo amministratore")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Utenti della famiglia"),
            @ApiResponse(responseCode = "401", description = "Accesso richiesto"),
            @ApiResponse(responseCode = "403", description = "Permesso amministratore richiesto")})
    public List<IdentityRepository.UserView> users(@AuthenticationPrincipal MusinaPrincipal user) {
        return repository.listUsers(user.familyId());
    }

    @PostMapping("/admin/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un accesso nella propria famiglia, senza registrazione pubblica")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Accesso creato"),
            @ApiResponse(responseCode = "400", description = "Dati non validi"),
            @ApiResponse(responseCode = "401", description = "Accesso richiesto"),
            @ApiResponse(responseCode = "403", description = "Permesso amministratore o CSRF mancanti"),
            @ApiResponse(responseCode = "409", description = "Nome utente non disponibile")})
    public ResponseEntity<IdentityRepository.UserView> create(@Valid @RequestBody CreateUser request,
                                                             @AuthenticationPrincipal MusinaPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(identity.createUser(user, request.username(), request.password(), request.role()));
    }

    @PostMapping("/admin/users/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disattiva un altro utente della propria famiglia")
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Accesso disattivato"),
            @ApiResponse(responseCode = "401", description = "Accesso richiesto"),
            @ApiResponse(responseCode = "403", description = "Permesso amministratore o CSRF mancanti"),
            @ApiResponse(responseCode = "404", description = "Utente non trovato nella famiglia"),
            @ApiResponse(responseCode = "409", description = "Disattivazione della propria utenza non consentita")})
    public void disable(@PathVariable UUID id, @AuthenticationPrincipal MusinaPrincipal user) {
        identity.disable(user, id);
    }

    public record CsrfView(String token, String headerName) { }
    public record ConnectionView(String message, String username, UUID familyId, String familyName, String role) { }
    public record CreateUser(@NotBlank @Size(min = 3, max = 80) String username,
                             @NotBlank @Size(min = 12, max = 128)
                             @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String password,
                             @NotBlank @Pattern(regexp = "MEMBER|ADMIN") String role) { }
}
