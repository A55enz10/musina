# Musina

Contabilità familiare in italiano, locale su Windows, con interfaccia responsive. **Implementato soltanto l'incremento 1:** login individuale, famiglia condivisa, ruoli, verifica React → backend, PostgreSQL/Flyway, build unica e test. Conti, estratti, report e Qwen non sono ancora implementati.

## Avvio da IntelliJ IDEA

Prerequisiti: JDK 25, Node (provato 22.19.0 con npm 10.9.3), Docker Desktop avviato con engine Linux. Nessun Gradle globale richiesto. Versioni ed evidenze: [verifica del bootstrap](docs/verification/bootstrap.md).

1. Apri la **radice** come progetto Gradle (settings.gradle). Seleziona JDK 25 sia come Project SDK sia in **Settings → Build Tools → Gradle → Gradle JVM**; usa il Wrapper.
2. Nel terminale PowerShell di IntelliJ, dalla radice:

   ~~~powershell
   . .\scripts\use-java25.ps1
   node scripts/setup-local.mjs
   .\gradlew.bat dbUp
   ~~~

   Il primo script seleziona un JDK 25 già installato solo per la sessione; il PATH di questa macchina punta ancora a Java 8. In alternativa imposta JAVA_HOME sul tuo JDK 25. Il setup crea .env con password casuali senza stamparle e non sovrascrive una configurazione esistente.
3. Avvia **Musina Backend**, poi **Musina Frontend** dalle configurazioni condivise in .run/. Senza supporto npm nell'IDE usa **Musina Frontend Gradle**, equivalente e disponibile tramite Gradle.
4. Apri **http://127.0.0.1:5173**. Le credenziali del primo amministratore sono nella tua .env locale (MUSINA_BOOTSTRAP_USERNAME e MUSINA_BOOTSTRAP_PASSWORD): leggile nell'editor e non condividerle.

Alternative nel terminale, in due schede distinte:

~~~powershell
. .\scripts\use-java25.ps1
.\gradlew.bat :backend:bootRun
~~~

~~~powershell
cd frontend
npm ci
npm run dev
~~~

Backend 127.0.0.1:8080, frontend 127.0.0.1:5173 con proxy /api; PostgreSQL solo 127.0.0.1:15432, database e ruolo musina_dev, volume Compose dedicato musina_musina_pgdata. Il provisioning avviene solo se il database Musina è vuoto; il riavvio non ricrea utenti o cambia password. Dopo il primo avvio puoi rimuovere da .env le tre proprietà MUSINA_BOOTSTRAP_*, conservando privatamente le credenziali. Non eliminare il volume per ripetere il setup.

Fermare Docker o usare docker compose down conserva il database nel volume nominato. Per riavviarlo usa la configurazione **Musina PostgreSQL** o il task dbUp. Eliminare esplicitamente il volume (anche tramite down con opzione -v) cancella i dati: non è un normale comando di arresto.

## Build e verifiche

Con Docker avviato, dalla radice e nella sessione JDK 25:

~~~powershell
.\gradlew.bat clean build
~~~

Esegue TypeScript, quattro test frontend e otto test backend, quindi genera **backend/build/libs/musina.jar con React incluso**. I test backend avviano un PostgreSQL effimero dedicato tramite Testcontainers: non usano il database di sviluppo o altri database esistenti. Su Linux/CI, con JDK 25 e Node configurati: bash gradlew clean build.

Per usare il pacchetto completo senza Vite, dalla radice:

~~~powershell
& "$env:JAVA_HOME/bin/java.exe" -jar backend/build/libs/musina.jar
~~~

Apri **http://127.0.0.1:8080** dopo aver fermato il backend di sviluppo, che usa la stessa porta.

Per lo smoke browser, installa una volta Chromium con npx playwright install chromium dalla directory frontend, poi torna alla radice:

~~~powershell
node scripts/smoke.mjs
~~~

Avvia temporaneamente il JAR su 127.0.0.1:18080, prova login/collegamento/logout e lo ferma; legge le credenziali dalla .env senza stamparle. Con entrambi i server di sviluppo già avviati: node scripts/smoke.mjs --existing verifica invece il proxy su 5173. **Solo credenziali sintetiche negli smoke**; sono disponibili MUSINA_E2E_USERNAME e MUSINA_E2E_PASSWORD come override da ambiente. Tracing e screenshot con credenziali disabilitati.

La [CI GitHub](.github/workflows/ci.yml) esegue build, test e smoke su Linux a ogni push e pull request, senza pubblicare l'applicazione. Le verifiche locali e i loro limiti sono documentati separatamente; l'esito della pipeline remota va controllato su GitHub Actions.

## Accessi e confini

Sessione server con cookie HttpOnly, CSRF, password derivate con PBKDF2, limiti ai tentativi di login. Ogni membro legge e gestisce i dati della propria famiglia; solo l'amministratore crea/disattiva accessi tramite API. La disattivazione revoca le sessioni già aperte. Nessuna registrazione pubblica. [Contratto API bootstrap](docs/api/CONTRACT.md), OpenAPI autenticata su /api/openapi.

L'avvio predefinito è sul solo PC. L'accesso LAN previsto sarà rifinito nell'incremento 8; nessun firewall modificato o porta Internet aperta. Qwen girerà sullo stesso PC nell'incremento 7; ora non serve installare un runtime IA.

## Documentazione

- [Stato attuale e limiti](docs/STATUS.md), [backlog](docs/BACKLOG.md), [incarico bootstrap](docs/BOOTSTRAP_TASK.md).
- [MVP](docs/MVP.md), [dominio e formule](docs/DOMAIN.md), [architettura](docs/ARCHITECTURE.md), [importazioni](docs/IMPORTS.md).
- Decisioni: [architettura](docs/decisions/0001-architecture.md), [provenienza](docs/decisions/0002-import-provenance.md), [report](docs/decisions/0003-report-conventions.md), [Qwen locale](docs/decisions/0004-local-qwen.md).
- [Istruzioni di lavoro](AGENTS.md); CLAUDE.md rimanda alla stessa fonte.

Credenziali, estratti e backup fuori da Git; campioni in docs/examples/ ignorati e preservati. Fixture solo sintetiche o adeguatamente anonimizzate. Nessun commit, push o merge senza autorizzazione esplicita. Salvadanai virtuali fuori dalla fase 1.
