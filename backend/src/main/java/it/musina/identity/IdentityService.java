package it.musina.identity;

import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IdentityService {
    private final IdentityRepository repository;
    private final PasswordEncoder passwords;

    public IdentityService(IdentityRepository repository, PasswordEncoder passwords) {
        this.repository = repository;
        this.passwords = passwords;
    }

    @Transactional
    public boolean bootstrap(String username, String password, String familyName) {
        // Serializes first provisioning across application processes without touching other databases.
        repository.lockBootstrap();
        if (!repository.hasUsers()) {
            UUID family = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            validatePassword(password);
            repository.createFamily(family, familyName);
            repository.createUser(user, normalize(username), passwords.encode(password), family, "ADMIN");
            repository.audit(user, user, "INITIAL_ADMIN_CREATED");
            return true;
        }
        return false;
    }

    @Transactional
    public IdentityRepository.UserView createUser(MusinaPrincipal actor, String username, String password, String role) {
        if (!role.equals("MEMBER") && !role.equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruolo non valido");
        }
        validatePassword(password);
        UUID id = UUID.randomUUID();
        String normalized = normalize(username);
        try {
            repository.createUser(id, normalized, passwords.encode(password), actor.familyId(), role);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome utente non disponibile");
        }
        repository.audit(actor.id(), id, "USER_CREATED");
        return new IdentityRepository.UserView(id, normalized, role, true);
    }

    @Transactional
    public void disable(MusinaPrincipal actor, UUID id) {
        if (actor.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Non puoi disattivare il tuo accesso");
        }
        if (!repository.disableUser(id, actor.familyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non trovato");
        }
        repository.audit(actor.id(), id, "USER_DISABLED");
    }

    private static String normalize(String username) {
        String result = username.trim().toLowerCase(Locale.ROOT);
        if (!result.matches("[a-z0-9._@-]{3,80}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome utente non valido");
        }
        return result;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La password deve contenere da 12 a 128 caratteri");
        }
    }
}
