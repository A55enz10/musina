# Incarico pronto: bootstrap eseguibile

**Incremento 1 autorizzato dall'utente e completato su Windows il 27 novembre 2026.** Autorizzati branch dedicato, file del progetto, database locale Musina, migrazioni e dati sintetici di test; nessun altro database o incremento successivo. Commit, integrazione su master e push del solo master successivamente autorizzati dall'utente. Questo documento conserva l'incarico e i suoi criteri; stato corrente in STATUS.md, evidenze in docs/verification/bootstrap.md.

## Obiettivo

Realizzare esclusivamente l'incremento 1 di [BACKLOG.md](BACKLOG.md): base eseguibile e sicura, avviabile da IntelliJ su Windows, con frontend React collegato al backend, PostgreSQL locale e un pacchetto Boot comprensivo di frontend. Nessuna funzione di contabilità o parsing bancario in questo incarico.

Chiarimento dell'utente: Musina sarà sviluppata e usata su questo stesso PC Windows; Qwen verrà eseguito qui nell'incremento 7. Nessun server di produzione separato da predisporre. Linux rimane controllo di portabilità in CI o destinazione futura, non prerequisito dell'uso locale.

## Istruzioni per il prossimo agente/sviluppatore

1. Leggi [AGENTS.md](../AGENTS.md), [STATUS.md](STATUS.md), [MVP.md](MVP.md), [ARCHITECTURE.md](ARCHITECTURE.md) e ADR 0001. Ispeziona Git e istruzioni applicabili; preserva modifiche presenti e `.idea/`. Lavora su branch dedicato (nome proposto `chore/bootstrap`), senza commit/push/merge non autorizzati e senza modifiche concorrenti nella working tree.
2. Verifica decisioni revisionate e ambiente reale: JDK, IntelliJ con supporto Java scelto, Docker Engine/Desktop e Compose, Node/npm, rete necessaria alle dipendenze. Rileggi fonti ufficiali e blocca versioni stabili compatibili. Java 25 è la preferenza; non cambiare stack silenziosamente per superare una difficoltà. Verifica anche BOM Boot, JDBC/Flyway e Springdoc. Registra ciò che non puoi eseguire.
3. Crea Wrapper radice (script Windows e Unix, JAR e checksum distribuzione), progetto `backend/` e `frontend/`. Proposta: un sottoprogetto Java backend, frontend npm/Vite con TypeScript, lockfile e `npm ci`; Gradle coordina frontend e backend. Scegli versioni bloccate per Node/tooling e una procedura di provisioning che funzioni su Windows/Linux, senza richiedere Gradle globale dopo il Wrapper.
4. Crea Compose locale con solo PostgreSQL, volume nominato e healthcheck; porta limitata a localhost. Fornisci un file esempio di ambiente con placeholder e istruzioni per valori locali, nessuna credenziale fissa condivisa. Le migrazioni Flyway devono creare soltanto la base necessaria a identità e appartenenza, non tabelle speculative di dominio; niente aggiornamento schema ORM automatico.
5. Implementa autenticazione individuale persistente, provisioning controllato del primo amministratore e utenti senza registrazione pubblica, autorizzazioni di famiglia e ruoli confermati: tutti i membri leggono e gestiscono la contabilità condivisa; solo l'amministratore gestisce gli accessi. Nessun ruolo di sola consultazione nella fase 1. Proteggi la sessione, CSRF, logout e tentativi di login; segreti da configurazione esterna. Non stampare password o dati sensibili e non aggiungere scorciatoie pubbliche di sviluppo attive in produzione.
6. Aggiungi una pagina React italiana responsive per login e verifica collegamento, con stati caricamento/successo/errore e logout. La verifica chiama un endpoint protetto sotto `/api` con un contesto di famiglia corrente controllato dal backend, privo di dati bancari; definisci e documenta in OpenAPI il contratto minimo realmente implementato. Nessun endpoint demo di contabilità o signup pubblico.
7. Predisponi avvii condivisi in `.run/` senza segreti o percorsi personali: backend tramite Gradle/Java supportato dall'IDE, frontend tramite npm e, se utile, compound. Documenta avvio PostgreSQL e poi dei due server, configurazione JDK e variabili locali; fornisci una via tramite task Gradle/terminale IntelliJ che non richieda funzionalità a pagamento. Proxy `/api` con sessione/CSRF funzionanti; non disabilitare la sicurezza per lo sviluppo.
8. Coordina la build unica: il frontend compilato entra nelle risorse generate e nel pacchetto Boot; nessun artefatto generato in Git. Scegli un comando radice riproducibile (interfaccia candidata `gradlew.bat clean build` / `./gradlew clean build`) che includa controlli TypeScript, test frontend e backend e pacchettizzazione. Avvia il pacchetto localmente senza server frontend e verifica UI/API sulla stessa origine, rotte SPA e errori API. La procedura completa di uso locale, aggiornamento e ripristino è nell'incremento 8; nessun deployment remoto in questo incarico.
9. Aggiungi test essenziali e CI coerente con l'hosting effettivo del repository, senza pubblicazione o deployment automatico: backend con PostgreSQL reale e Flyway, sicurezza e isolamento, frontend sui tre stati di collegamento e smoke browser del flusso login→verifica→logout. La CI Linux esegue build coordinata e smoke del pacchetto con DB di test. Nessuna credenziale reale o estratto bancario.
10. Riscrivi la sezione avvio di README con comandi **realmente esistenti e provati**, prerequisiti, variabili, porte, preparazione utente, esecuzione test, build e avvio pacchetto. Prova da checkout/ambiente pulito quanto disponibile su Windows e Linux/CI; documenta limiti e dipendenze di rete. Aggiorna BACKLOG e STATUS distinguendo implementato/verificato, senza presentare una CI non eseguita come superata.

## Accettazione e consegna

Valgono tutti i criteri dell'incremento 1. Devono esserci evidenze riproducibili di:

- Avvio PostgreSQL e migrazioni su DB pulito; riavvio senza perdita utenti o nuove migrazioni inattese.
- Backend e frontend avviati da configurazioni condivise; login utente, verifica protetta, errore di collegamento leggibile e logout.
- Richieste anonime negate, richieste mutanti prive di CSRF negate, risorse di altra famiglia negate, provisioning riservato; due famiglie sintetiche nei test.
- Build radice e pacchetto eseguibile con React incluso, test essenziali e pipeline CI; smoke UI/API da stesso indirizzo senza Vite.
- Nessuna dipendenza da percorsi personali o segreti versionati; istruzioni riproducibili da ambiente pulito e versioni effettive registrate.

Alla consegna elenca file, scelte rese concrete, comandi e risultati, verifiche mancanti e prossimo incremento. Nessun import MPS/Quickard, regola, grafico contabile, chiamata IA o infrastruttura futura. Nessun commit, push o merge senza autorizzazione esplicita.

## Dipendenze realmente bloccanti

Revisione e autorizzazione dell'incremento 1 sono state ricevute. La validazione incompleta dei campioni e la scelta del runtime/modello Qwen **non bloccano questo bootstrap**. Docker e JDK 25 sono stati resi disponibili senza cambiare stack; le verifiche locali sono passate. CI remota/Linux e click nell'IDE rimangono dichiarati come non verificati, senza presentarli come test superati.
