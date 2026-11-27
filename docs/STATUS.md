# Stato attuale

Aggiornato al **27 novembre 2026**. Incremento 1 completato e verificato su Windows sul branch **chore/bootstrap**. L'utente ha autorizzato il commit del bootstrap, l'integrazione su master e il push del solo master.

- Backend Java 25/Spring Boot, React/TypeScript, PostgreSQL Musina dedicato e migrazione Flyway identità.
- Sessione, CSRF, password PBKDF2, limiti login, ruoli membro/amministratore, isolamento famiglia e revoca accessi.
- Avvii IntelliJ condivisi, proxy /api, Gradle Wrapper e pacchetto Boot con React incluso; CI GitHub predisposta.
- Passati 8 test backend, 4 frontend, smoke browser del JAR e dei server separati, build da sorgenti puliti con lockfile e senza .env/node_modules/output. [Evidenze](verification/bootstrap.md).
- Database sviluppo musina_dev su localhost:15432 con un amministratore locale; segreti soltanto in .env ignorata. Altri database e campioni bancari non modificati.
- I quattro punti richiesti sono nel backlog: DATE-MIX, NEGATIVE-CHARTS, CATEGORY-NATURE e AI-BACKGROUND. Incrementi 2–8 non implementati.

## Blocchi e limiti

Prima CI remota fallita nel setup di Corretto sul commit `d0f7e40`. Correzione implementata: selettore Java `25` e cleanup condizionato alla presenza di `.env` (provato localmente con Docker simulato, con e senza `.env`). Pubblicata la correzione su master (`9904a6e`): setup Java e cleanup superati in remoto. La pipeline si ferma al checksum del Wrapper: il servizio Gradle risponde HTTP 301 e curl non seguiva il reindirizzamento. Aggiunta opzione `--location`; nuova esecuzione da verificare.

Nessun impedimento attuale alla build Windows. Il PATH punta a Java 8: selezionare JDK 25 in IntelliJ/Gradle, oppure usare lo script di sessione indicato nel README. Docker Desktop è stato avviato.

Click delle configurazioni IntelliJ non automatizzato; eseguiti i task corrispondenti da CLI. CI remota e portabilità Linux non verificate in questa sessione. Nessuna esposizione LAN/Internet o installazione Qwen.

## Prossimo passo

Verificare su GitHub Actions la nuova esecuzione della correzione CI. Avviare il bootstrap da IntelliJ seguendo il README. Per iniziare l'incremento 2 serve un nuovo incarico; i tracciati bancari e i quattro punti differiti restano nel backlog. Nessun lavoro applicativo oltre l'incremento 1 autorizzato.
