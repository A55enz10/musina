# Architettura della fase 1

Stato: solo bootstrap implementato; monolite Java 25/Spring Boot, React, PostgreSQL/Flyway e sicurezza di base provati insieme. Risultati e versioni effettive in [verifica bootstrap](verification/bootstrap.md). Le altre aree di dominio sono progettate, non implementate. [ADR 0001](decisions/0001-architecture.md) registra le scelte confermate; le opzioni future marcate come proposte rimangono tali.

## Struttura e distribuzione confermate

Un repository e un progetto IntelliJ, con `backend/` Java/Spring Boot e `frontend/` React/TypeScript. Gradle Wrapper nella radice e build coordinata; PostgreSQL e migrazioni Flyway. Monolite modulare inizialmente per package, API REST con OpenAPI. Nessun microservizio, Redis, broker, Kubernetes o Event Sourcing.

**Ambiente chiarito dall'utente:** sviluppo e uso ordinario di Musina su questo PC Windows, insieme a Qwen. Nessun server di produzione separato da predisporre; Linux resta una possibile destinazione futura e un ambiente di CI. In sviluppo: backend e server frontend avviabili separatamente da IntelliJ, proxy `/api`. Per l'uso ordinario locale: React incluso nel pacchetto Spring Boot, UI e API sullo stesso indirizzo; eventuale container applicativo e PostgreSQL sulla macchina locale. Il runtime Qwen è un processo/container opzionale sullo stesso PC, necessario soltanto con IA abilitata; modalità da scegliere. Node serve alla build, non all'esecuzione del pacchetto completo.

Bootstrap implementato: radice Gradle di coordinamento e un solo sottoprogetto Java `backend`; `frontend` come progetto npm/Vite coordinato da task Gradle. `build` esegue typecheck, test e pacchettizzazione; `bootJar` dipende dalla build React e ne copia l'output nelle risorse generate, senza file compilati in `src/` o in Git. Wrapper e lockfile Java/npm fissano le dipendenze; comandi Windows e Linux/CI nel README. Nessun sottoprogetto per ciascun package.

## Confini proposti dei package

| Area | Responsabilità |
| --- | --- |
| identity | Utenti, famiglia, appartenenze, autenticazione e autorizzazioni |
| accounts | Conti, gruppi e composizione del perimetro |
| imports | Batch, righe sorgenti, adapter, anteprime, copertura, riconciliazione e annullamento |
| transactions | Movimenti canonici, quote e collegamenti di trasferimento |
| categorization | Categorie, esercenti, alias, regole e precedenze |
| reporting | Query, formule e drill-down coerenti con DOMAIN.md |
| ai | Orchestrazione dei suggerimenti e adapter del provider opzionale |

Creare solo i package effettivamente usati dall'incremento. In ogni area distinguere dominio, servizi applicativi e adapter di infrastruttura/API secondo necessità; evitare strutture vuote. Il dominio non dipende dagli SDK IA o dalle classi di parsing XLSX. REST e persistenza richiamano i casi d'uso; un'area accede alle altre tramite interfacce applicative definite, senza accedere liberamente ai repository altrui. Un'eventuale libreria comune deve rimanere piccola (es. Money), non contenere logica di prodotto indistinta.

PostgreSQL è la fonte autorevole di movimenti, provenienza e regole. Usare transazioni per conferma import, annullamento, split e applicazione retroattiva; vincoli univoci e controllo di versione per operazioni concorrenti. Nessuna esigenza iniziale di elaborazione distribuita: il caricamento limitato e i casi d'uso sincroni bastano finché misure concrete non dimostrano il contrario.

## Sicurezza del bootstrap

- Spring Security con sessione server, cookie HttpOnly e SameSite, Secure quando si usa HTTPS, CSRF per operazioni mutanti. **Accesso locale confermato:** link del PC, utilizzabile anche dalla rete domestica, con login individuale e applicazione avviata. Configurare esplicitamente il solo ingresso applicativo per la LAN e la protezione del trasporto; DB e runtime IA restano privati sulla macchina. Nessuna esposizione Internet o configurazione di hosting online nella fase attuale. Proxy di sviluppo mantiene lo stesso flusso di sessione/CSRF; nessun CORS permissivo. Non conservare token in localStorage.
- Credenziali individuali in database con password hash adattivo; provisioning amministrativo locale/documentato, idempotente e senza password nel repository. Nessun endpoint di signup pubblico; nessuna password iniziale fissa. Logout invalida la sessione; errori di login generici e limitazione dei tentativi dal bootstrap, anche senza Redis.
- **Ruoli confermati durante la revisione:** membro con lettura e gestione contabile condivisa (inclusi import e correzioni), amministratore con anche gestione degli accessi, provisioning e disattivazione membri. Le operazioni di gestione utenti sono riservate all'amministratore; nessun ruolo di sola consultazione nella fase 1. Il caricatore iniziale è una scelta operativa, non un vincolo di autorizzazione. Non aggiungere privacy per conto fra membri della stessa famiglia.
- Ogni query e comando che usa un ID deve verificare appartenenza alla famiglia sul backend, inclusi batch, file, regole e report. Il bootstrap prova il confine con una seconda famiglia sintetica anche se il prodotto iniziale ne usa una sola. UUID o ID non sequenziali non sostituiscono questi controlli.
- Solo login, risorse statiche e un eventuale health minimale senza dati sensibili possono essere non autenticati. La verifica React del backend usa un endpoint protetto relativo alla famiglia corrente. Documentazione API/diagnostica protetta o disabilitata nella configurazione di uso ordinario; niente dettagli DB o stack trace nelle risposte.
- Segreti via ambiente o file esterni con accesso limitato; `.run/` condivisa priva di valori segreti e percorsi personali. PostgreSQL vincolato a localhost per l'accesso dal backend sull'host, oppure rete privata fra container locali; credenziali dedicate e container senza privilegi elevati.
- Log tecnici con ID di correlazione e stato, senza corpi di estratti, descrizioni, cookie o credenziali. Audit applicativo protetto con attore, azione, oggetto e istante; eventuali dettagli contabili rimangono nel database autorizzato. Separare audit da log diagnostici.

Implementati nel bootstrap: sessione server, CSRF, provisioning del primo amministratore su DB vuoto da configurazione locale, creazione/disattivazione accessi via API amministrative, password PBKDF2 e limitazione dei tentativi. Revoca delle sessioni degli utenti disattivati e isolamento della famiglia verificati con test HTTP. I vincoli su conti, import, regole e report si applicheranno ai relativi incrementi, ancora non implementati.

## API e denaro

OpenAPI descriverà endpoint implementati, autenticazione, errori, paginazione e importi come stringhe decimali con valuta. Date bancarie ISO senza fuso, audit con istante UTC. Nessun contratto di colonna bancaria viene deciso nell'API. Validazione server autorevole; frontend formatta in italiano senza calcoli contabili in virgola mobile. Totali e drill-down condividono criteri e aggregazioni backend, evitando formule divergenti nel browser.

Le schermate devono gestire caricamento, assenza di dati, errori, copertura incompleta e suggerimenti non confermati. Le rotte SPA non devono intercettare errori `/api` restituendo HTML al posto di risposte API.

Il browser usa indirizzi API relativi sullo stesso ingresso applicativo, senza `localhost` del PC codificato nel frontend: un link aperto dallo smartphone deve raggiungere il backend sul PC, non sul telefono. Un futuro hosting online sarà una decisione separata, da verificare rispetto a backend, PostgreSQL e modalità IA; nessun provider scelto ora e nessuna modifica anticipata al pacchetto unico.

## IA opzionale

**Confermato dall'utente:** iniziare con Qwen eseguito su questo stesso PC Windows, dove viene sviluppata e usata Musina, senza servizio di inferenza cloud. Modello/versione, quantizzazione e runtime restano da scegliere; la collocazione non è più una questione aperta. Il dominio conserva la porta sostituibile prevista; la compatibilità di un protocollo non impone l'uso di uno specifico SDK. Decisione in [ADR 0004](decisions/0004-local-qwen.md).

Verifica ufficiale del 26 novembre 2026: Qwen documenta esecuzione locale tramite [Ollama](https://qwen.readthedocs.io/en/latest/run_locally/ollama.html) e [vLLM](https://qwen.readthedocs.io/en/latest/deployment/vllm.html). La pagina Ollama consultata presenta esempi riferiti a una generazione precedente e segnala aggiornamenti necessari: non è una verifica del supporto di un modello recente. Non sono stati scelti o installati runtime o modelli. Tentata lettura di CPU/RAM/GPU tramite CIM Windows: accesso negato, quindi caratteristiche hardware non accertate; la sola presenza dell'eseguibile `nvidia-smi` non prova modello GPU o memoria disponibile. Adeguatezza sulla categorizzazione italiana, prestazioni e compatibilità restano da verificare prima della scelta del modello, con dati sintetici e categorie ammesse.

Il backend contatterà l'endpoint del runtime locale in rete controllata; niente esposizione diretta al browser o pubblica e nessun fallback automatico verso provider cloud. Minimizzazione del payload, protezione dei log, validazione delle risposte e conferma umana restano applicabili anche in locale. Definire limiti di concorrenza, memoria e tempo senza compromettere import e report; runtime spento o modello non caricato equivalgono a IA indisponibile. Nessuna installazione IA nel bootstrap e nessuna dipendenza del funzionamento contabile dal runtime.

Pipeline: classificazioni manuali protette → regole e associazioni deterministiche → casi irrisolti → suggerimenti IA soltanto se abilitata. Porta applicativa indipendente dal provider, con adapter sostituibile; nessuna dipendenza del dominio dagli SDK. Modalità disabilitata sempre disponibile; timeout, indisponibilità, limiti di costo e risposte invalide lasciano il movimento irrisolto, senza impedire importazioni e report.

**Confermato durante la revisione:** avvio automatico della generazione dei suggerimenti dopo ogni importazione confermata, quando l'IA è abilitata, limitato ai casi ancora irrisolti dopo le regole. Nessuna chiamata durante una semplice anteprima o per un batch fallito. Conferma bancaria persistita prima della richiesta esterna; l'esito IA non ne fa parte e non può annullare o duplicare l'importazione. Riutilizzare i suggerimenti validi evitando chiamate duplicate sui retry e sui reupload idempotenti. La scelta di esecuzione tecnica sarà definita nell'incremento 7 senza introdurre broker o servizi distribuiti; suggerimenti generati e classificazioni accettate restano stati distinti.

Inviare solo un testo derivato minimo e ripulito della descrizione, eventuale esercente e categorie ammesse; escludere estratti completi, identificativi bancari, nomi di persone, riferimenti di pagamento e dati non necessari, inclusi di norma importi/date/saldi. Se non si riesce a minimizzare in modo affidabile, non inviare. Trattare la descrizione come dato non affidabile separato dalle istruzioni; validare la risposta contro schema e categorie consentite, mai eseguire istruzioni o strumenti richiesti dal testo. Non inviare descrizioni reali integrali.

Le risposte producono proposte, mai modifiche autonome. **Confermata** la possibilità di controllare e accettare o correggere i suggerimenti anche su più movimenti selezionati; creazione di regole e associazioni riutilizzabili come azione esplicita separata. Validare permessi, versione e stato dei movimenti alla conferma multipla, senza sovrascrivere manuali o applicare suggerimenti obsoleti. Audit minimo con versione modello/prompt, esito e decisione dell'utente, senza log del payload sensibile. Runtime Qwen locale, modello/versione, politica dei log/cache locali, abilitazione e limiti di risorse sono da definire nell'incremento 7; esecuzione locale e avvio automatico dopo import quando abilitata sono già confermati. Prima di una chiamata, riusare regole persistenti e suggerimenti validi ancora applicabili; confermare Dolci Fate come regola evita nuove chiamate per lo stesso caso. Non generalizzare automaticamente Amazon.

## Versioni e compatibilità

Ricognizione su fonti ufficiali del **26 novembre 2026**. Versioni candidate per la successiva attività, non installate né bloccate qui. Prima del bootstrap rileggere le fonti, scegliere patch supportate e registrare versioni effettive; niente snapshot/RC, `latest` o intervalli dinamici nei file finali.

| Componente | Candidata / evidenza ufficiale | Limite della verifica |
| --- | --- | --- |
| Java | Preferenza Java 25; compatibile secondo i requisiti Spring Boot e la matrice Gradle sotto | Distribuzione JDK, patch e IntelliJ installato da verificare; pagina OpenJDK 25 non accessibile durante la ricognizione (HTTP 403) |
| Spring Boot | 4.2.0 elencata Stable; Java da 17 a 27 e Gradle 8.14+ oppure 9.x nei [requisiti ufficiali](https://docs.spring.io/spring-boot/system-requirements.html) | Compatibilità documentata, nessuna build eseguita |
| Gradle | 9.9.2 nella [matrice corrente](https://docs.gradle.org/current/userguide/compatibility.html); esecuzione e toolchain Java 25 supportate da 9.1.0 | Candidata coerente con Boot; plugin e Wrapper da provare nel bootstrap |
| React | 19.3.0 indicata tra le release nella [pagina versioni](https://react.dev/versions) | Integrazione con toolchain e librerie UI da testare |
| Node.js | Linea 24 LTS proposta; 24 e 26 risultano LTS nel [calendario ufficiale](https://nodejs.org/en/about/previous-releases) | Patch da fissare; Node è richiesto solo per sviluppo/build |
| Vite, TypeScript | Vite proposto per React SPA; la [guida](https://vite.dev/guide/) indica Node 20.19+ / 22.12+ | Nessuna versione esatta proposta: release/patch di Vite, TypeScript e plugin React non verificate; compatibilità combinata da provare |
| PostgreSQL | Linea 18 supportata nella [documentazione delle release](https://www.postgresql.org/docs/18/release.html) | Patch e tag/digest container da fissare; non si sceglie automaticamente la major più recente |
| Flyway e JDBC PostgreSQL | Usare versioni gestite dal BOM Boot se compatibili; [Flyway PostgreSQL](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database) elenca 18 fra le versioni verificate e richiede il modulo PostgreSQL separato | Versioni esatte del BOM e combinazione runtime non verificate: provarle con DB reale |
| OpenAPI | Springdoc 3.2.1: [matrice ufficiale](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot) associa Boot 4.2.x a 3.2.1 | Candidato, verificare generazione del contratto; non attivare moduli MCP |

Non si dichiara verificato l'intero stack sulla sola base di queste matrici. Per Java 25 il requisito Gradle effettivo è almeno 9.1.0, anche se Boot accetta anche Gradle 8.14: non usare Gradle 8 per eseguirlo su JDK 25. Versioni di librerie XLSX, grafici, test, immagini base e tool CI saranno scelte e verificate negli incrementi pertinenti.

## Operatività prevista

Migrazioni Flyway versionate e immutabili una volta applicate; niente aggiornamento automatico dello schema tramite ORM nell'uso ordinario. Configurazioni di sviluppo e uso locale del pacchetto separate senza divergere nel dominio. **Confermato:** conservare gli estratti originali nell'applicazione con accesso protetto e includerli nei backup insieme al DB e alle informazioni necessarie al ripristino; nessun file in Git o nelle richieste IA. L'archiviazione deve essere persistente e preservare i collegamenti fra batch e originali; modalità fisica da scegliere senza aggiungere infrastrutture non necessarie.

**Semplificazione richiesta dall'utente:** nessun piano di disaster recovery, backup programmato, rotazione a 30 copie o obiettivo di tempo/perdita dati. Soluzione minima: esportazione manuale del database e degli originali in un percorso scelto dall'utente, con istruzioni essenziali per reimportarli quando serve. Gestione delle copie manuale; nessuna cancellazione automatica. Proteggere il percorso e mantenere coerenti DB e originali senza introdurre servizi di backup o gestione dedicata di chiavi. La verifica prevista è soltanto un controllo funzionale dell'esportazione/reimportazione su dati sintetici, non una simulazione di disastro.
