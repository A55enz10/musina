# ADR 0001 — Monolite modulare e distribuzione unica

Data: 26 novembre 2026. **Stato: confermato dall'utente per i vincoli elencati; non implementato.**

## Contesto

Musina parte da un repository nuovo, sviluppato su Windows con IntelliJ. Durante la revisione l'utente ha chiarito che il progetto viene anche usato localmente su questo PC: il riferimento iniziale a Linux non richiede un server separato; resta portabilità futura e ambiente di CI. La fase 1 non richiede servizi distribuiti.

## Decisione

Un repository e un progetto IntelliJ, Java (preferibilmente 25), Spring Boot, Gradle, React/TypeScript, PostgreSQL/Flyway e API REST/OpenAPI. Backend e frontend in directory sorelle, Wrapper nella radice e build coordinata. Monolite modulare per package, evitando molti moduli Gradle.

Server frontend separato in sviluppo con proxy `/api`; build React inclusa in Spring Boot per l'uso ordinario locale, stessa origine e possibilità di avvio tramite container applicativo più PostgreSQL locale. Nessun server di produzione separato richiesto. Nessun microservizio, Redis, broker, Kubernetes o Event Sourcing senza nuova necessità concordata.

Revisione successiva: l'utente ha scelto Qwen locale per l'IA opzionale. La base applicazione + DB resta valida senza IA; quando abilitata, occorre anche un runtime locale come descritto in [ADR 0004](0004-local-qwen.md), senza scomporre il dominio in microservizi.

Confermato l'uso tramite link locale, anche sulla rete domestica per smartphone/altri dispositivi, con login individuale e PC acceso. La pubblicazione online potrà essere valutata in futuro, senza scelta attuale del servizio o vincolo a uno specifico hosting; non richiede infrastruttura Internet nella fase locale.

## Conseguenze

Deployment e autorizzazioni centralizzati; attenzione a separare confini dei package e adapter dei provider dal dominio. Il task di pacchettizzazione deve includere il frontend senza dipendere dal dev server. Le versioni candidate e i limiti delle verifiche sono in [ARCHITECTURE.md](../ARCHITECTURE.md#versioni-e-compatibilità).

Confermato durante la revisione: tutti i membri possono leggere e gestire la contabilità della famiglia, inclusi import e correzioni; solo l'amministratore gestisce gli accessi degli utenti. Nessun ruolo di sola consultazione nella fase 1. Autorizzazioni applicate dal backend fin dal bootstrap, senza isolamento di conti fra membri della stessa famiglia.

Concretizzati nell'incremento 1: radice Gradle con un sottoprogetto backend, frontend npm/Vite, sessioni server e provisioning iniziale su database vuoto tramite configurazione locale. Nessun componente dei successivi incrementi implementato. Evidenze e limiti della verifica in [bootstrap](../verification/bootstrap.md).
