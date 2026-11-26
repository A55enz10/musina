# ADR 0004 — Qwen locale come prima integrazione IA

Data: 26 novembre 2026. **Stato: Qwen su questo PC Windows, insieme a Musina, confermato dall'utente; runtime e modello da definire; non implementato.**

## Contesto

La fase 1 prevede assistenza opzionale alla categorizzazione dopo le regole deterministiche, con dominio indipendente dai provider. L'utente ha indicato Qwen e scelto di eseguirlo localmente anziché tramite un servizio API cloud.

## Decisione

Primo adapter verso un runtime Qwen su questo stesso PC Windows, dove Musina viene sviluppata e usata, senza fallback automatico al cloud. Nessun server Linux separato da predisporre. Modello/versione, quantizzazione e runtime saranno scelti dopo la verifica delle risorse della macchina e prima dell'incremento 7. Nessun download, installazione o infrastruttura IA nel bootstrap.

Il backend è l'unico chiamante del runtime, tramite endpoint in rete controllata; il dominio non dipende da SDK o protocollo di uno specifico runtime. Minimizzazione dei dati e validazione rimangono obbligatorie. Suggerimenti automatici dopo import confermato se IA abilitata, accettazione/correzione umana anche in blocco e creazione regole separata, come già deciso.

## Conseguenze

La base rimane un'applicazione Spring Boot con React incluso più PostgreSQL. Per abilitare l'IA serve anche il runtime locale, come processo o container da definire; non è un nuovo microservizio di dominio. L'applicazione deve funzionare anche con runtime spento o modello non disponibile e non deve inviare dati a servizi cloud come ripiego.

La collocazione su questo PC non è più aperta. La lettura hardware via CIM ha restituito accesso negato: CPU/RAM/GPU non ancora accertate, nessuna prova del modello eseguita. Non fissare tag o requisiti hardware senza verificarli sul runtime scelto. Fonti e limiti della ricognizione in [ARCHITECTURE.md](../ARCHITECTURE.md#ia-opzionale).
