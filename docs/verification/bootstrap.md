# Verifica del bootstrap — 27 novembre 2026

Ambito: incremento 1, Windows 11, branch `chore/bootstrap`, nessuna funzione contabile o IA. Versioni controllate su registri/fonti ufficiali prima di fissarle, quindi compilate e provate insieme. Nessuna operazione su database estranei a Musina.

## Versioni effettive

| Componente | Versione e controllo |
| --- | --- |
| JDK | Corretto 25.0.3, installato; versione e compilazione/toolchain reali |
| Gradle | 9.9.2; Wrapper/JAR ufficiali, checksum JAR verificato e SHA-256 distribuzione fissato; dipendenze Java transitive in `backend/gradle.lockfile` |
| Spring Boot | 4.2.0; disponibile in Maven Central e compilato/eseguito |
| Spring Security | 7.2.0, gestito dal BOM Boot; verificato nel JAR e con prove di sicurezza |
| Flyway / PostgreSQL JDBC | 12.11.0 / 42.7.14, gestiti dal BOM; migrazione reale su PostgreSQL 18.7 |
| PostgreSQL | Immagine ufficiale `postgres:18.7`, avviata e interrogata; Testcontainers separato per i test |
| Springdoc | 3.2.1, matrice Boot 4.2; OpenAPI servita e testata |
| Node / npm | 22.19.0 / 10.9.3 già installati; versioni effettivamente usate, `.node-version` e lockfile |
| React / React DOM | 19.3.1 / 19.3.1, metadati npm e build/test reali |
| Vite / plugin React | 8.4.0 / 6.3.0, engine compatibile con Node presente e build riuscita |
| TypeScript / Vitest | 7.0.2 / 5.1.2, typecheck e test reali |
| jsdom | 26.1.0, requisito Node >=18 verificato; compatibile con Node 22.19, mentre la release corrente richiede patch Node superiori |
| Playwright | 1.65.1, Chromium isolato scaricato; smoke headless reale |
| Docker / Compose | Engine 29.2.1, Desktop 4.64.0, Compose 5.1.0 |
| IntelliJ | `product-info.json` identifica 2026.1.1; configurazioni condivise XML, nessun percorso o segreto personale |

Fonti: [Boot](https://docs.spring.io/spring-boot/system-requirements.html), [Gradle/Java](https://docs.gradle.org/current/userguide/compatibility.html), [Springdoc](https://springdoc.org/), [Vite](https://vite.dev/guide/), [Maven Central Boot](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot/maven-metadata.xml), [registro npm React](https://registry.npmjs.org/react/19.3.1), [registro npm jsdom](https://registry.npmjs.org/jsdom/26.1.0), [IntelliJ/Java 25](https://www.jetbrains.com/idea/whatsnew/2025-2/), [Gradle in IntelliJ](https://www.jetbrains.com/help/idea/gradle.html). Verificati anche i metadati npm delle altre dipendenze fissate. La documentazione non è stata usata come sostituto della build.

## Prove eseguite

- `gradlew.bat build --console=plain`: riuscito, otto test backend e quattro frontend, frontend incluso nel Boot JAR.
- HTTP backend su PostgreSQL Testcontainers: migrazione vuota e riavvio/idempotenza, provisioning, login/CSRF/logout, due famiglie isolate, permessi amministrativi, validazione senza password nelle risposte, disattivazione/revoca sessioni, limiti login, OpenAPI protetta, errori API senza SPA HTML.
- Frontend: caricamento, collegamento, indisponibilità/retry, login/logout. API reali negli smoke; risposte controllate nei test unitari degli stati di errore.
- `node scripts/smoke.mjs`: JAR locale senza Vite, React incluso; browser reale login → verifica → logout, cookie HttpOnly e rotte API/SPA.
- `:backend:bootRun` più `npm run dev`, poi `node scripts/smoke.mjs --existing`: proxy `/api`, sessione e flusso browser riusciti; controllato anche layout a 390 px.
- Postman CLI 1.82.0: due assert HTTP sull'endpoint locale CSRF, token/header e cache `no-store`; token e cookie non stampati. Nessun workspace creato o contenuto pubblicato.
- DB sviluppo dopo riavvio: `musina_dev`, una famiglia, un amministratore e una migrazione riuscita; password casuali solo in `.env` ignorata. I test non modificano questo DB.
- Copia pulita dei sorgenti in `.tools/clean-bootstrap-20261127`, senza `.env`, campioni, `node_modules`, output o metadati Git: build completa riuscita con 8 test backend e 4 frontend; cache delle dipendenze condivisa, PostgreSQL di test nuovo e isolato. Ripetizione conclusiva con sorgenti/lockfile finali in `.tools/clean-bootstrap-final-20261127`: riuscita, ancora 8 backend e 4 frontend, nessun test saltato.

Report locali ignorati: `backend/build/reports/tests/test/index.html`, `frontend/playwright-report/index.html`, log dello smoke in `backend/build/smoke-application.log`. Tracing e screenshot browser disabilitati, credenziali esterne agli artefatti di test.

## Limiti

Configurazioni IntelliJ condivise e task corrispondenti eseguiti da CLI; click nell'IDE non automatizzato. Le prove qui riportate sono state eseguite localmente prima della pubblicazione: l'esito della CI GitHub successiva al push va verificato separatamente. Portabilità Linux non provata nella sessione Windows. LAN, hosting, importazioni, grafici, regole e Qwen appartengono a incrementi successivi.

I tag esatti delle action CI sono stati risolti nei repository ufficiali: checkout 7.0.1, setup-java 6.3.0, setup-node 7.1.0. Questo controllo non equivale all'esecuzione della pipeline.

Un tentativo di smoke senza selezione del JDK ha ereditato Java 8 dal PATH e non ha avviato il JAR; ripetuto nella sessione impostata da `scripts/use-java25.ps1`, è passato. Nessun downgrade del progetto a Java 8 e nessuna modifica del PATH di sistema.
