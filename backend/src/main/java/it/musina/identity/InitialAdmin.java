package it.musina.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class InitialAdmin implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(InitialAdmin.class);
    private final IdentityService identity;
    private final Environment environment;

    InitialAdmin(IdentityService identity, Environment environment) {
        this.identity = identity;
        this.environment = environment;
    }

    @Override public void run(ApplicationArguments args) {
        String username = environment.getProperty("MUSINA_BOOTSTRAP_USERNAME");
        String password = environment.getProperty("MUSINA_BOOTSTRAP_PASSWORD");
        if (username == null && password == null) { return; }
        if (username == null || password == null || password.startsWith("REPLACE_")) {
            throw new IllegalStateException("Configurare entrambi i valori di provisioning locale, senza placeholder");
        }
        if (identity.bootstrap(username, password, environment.getProperty("MUSINA_BOOTSTRAP_FAMILY", "Famiglia"))) {
            LOG.info("Provisioning iniziale Musina completato; credenziali disponibili soltanto nella configurazione locale");
        }
    }
}
