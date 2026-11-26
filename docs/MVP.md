# Fase 1: prodotto e confini

Stato: bootstrap dell'incremento 1 implementato; funzioni contabili degli incrementi successivi non implementate. Le proposte tecniche e contabili residue restano riconoscibili negli altri documenti.

## Utenti e uso

La famiglia consulta gli stessi dati con accessi individuali. Inizialmente un membro carica tutti gli estratti; questo non impone un importatore unico nel modello. Nessuna registrazione pubblica. UI in italiano, responsive per desktop e smartphone.

**Confermato durante la revisione:** tutti i membri possono importare estratti, correggere categorie e gestire la contabilità condivisa della propria famiglia. Solo l'amministratore gestisce gli accessi degli utenti; ha anche le normali capacità contabili. Nessun ruolo di sola consultazione nella fase 1.

Il percorso principale è: configurare un conto → caricare un estratto → controllare anteprima e anomalie → confermare → consultare movimenti e grafici → correggere classificazioni e, se desiderato, salvare una regola.

## Funzioni confermate

| Area | Risultato richiesto |
| --- | --- |
| Conti | Conti correnti, prepagate, wallet digitali; nessuna fonte futura obbligatoria |
| Gruppi | Nomi e composizione personalizzabili; più gruppi per conto; nessun annidamento; unione dei conti senza duplicare movimenti. Appartenenza valida per tutto lo storico, senza date di decorrenza nei report; data di inserimento nella relazione gruppo–conto come metadato |
| Ambiti | Personale, comune e famiglia sono gruppi dell'utente; nessuna attribuzione personale/comune sulla spesa |
| Import | Priorità XLSX MPS e Quickard, adapter modulari; anteprima, validazione, provenienza, idempotenza, sovrapposizioni, annullamento |
| Qualità | Mese iniziale di monitoraggio configurabile per conto; copertura temporale, periodi mancanti, anomalie, non classificati e riconciliazione dei saldi distinti e visibili |
| Trasferimenti | Giroconti e ricariche separati da redditi/spese; collegamento automatico solo con corrispondenza univoca e riferimenti affidabili ai conti, casi dubbi da verificare; commissioni come spese e lati mancanti espliciti |
| Categorie | Categorie/sottocategorie modificabili, esercenti normalizzati, regole persistenti, split; classificazioni manuali protette |
| Report | Per conto o gruppo, mese, anno e intervallo personalizzato; data di riferimento selezionabile fra quelle disponibili, categorie, andamento mensile, confronto entrate/uscite e accesso ai movimenti sottostanti |
| IA | Se abilitata, suggerimenti automatici dopo ogni importazione confermata sui casi rimasti irrisolti dopo le regole; conferma/correzione umana anche su più movimenti selezionati, creazione regole separata; provider sostituibile e operatività completa senza IA |

Importare un file non equivale a dichiarare completo il periodo o riconciliato il conto. Un movimento identico a un altro può essere legittimo. Un bonifico non è automaticamente un giroconto. Un esercente come Amazon non basta a determinare la categoria.

**Riepilogo mensile confermato:** entrate, uscite complessive comprensive di mutuo e PAC, differenza entrate meno uscite; dettaglio separato di spese ordinarie, mutuo e versamenti per investimenti. Giroconti interni esclusi. Esempio: entrate 3.000 euro, spese ordinarie 1.000, mutuo 800, PAC 200 → uscite 2.000 e differenza 1.000. Non è una misura di variazione del patrimonio o un saldo bancario.

Le entrate complessive includono redditi e altre entrate, mantenendo le due voci distinte: stipendio 3.000 euro e disinvestimento 500 euro → entrate 3.500, senza calcolo del rendimento. Anche un prestito ricevuto è altra entrata, senza monitoraggio del debito. Rimborsi a riduzione delle spese e trasferimenti separati.

## Criteri di completamento della fase

Gli otto incrementi di [BACKLOG.md](BACKLOG.md) sono utilizzabili e verificati con evidenze; le formule di [DOMAIN.md](DOMAIN.md) sono state revisionate. Si può ripetere un'importazione, risolvere un'ambiguità, annullare un batch e spiegare ogni totale tramite i movimenti e la loro provenienza. I report segnalano dati incompleti invece di rappresentarli come completi.

Accessi individuali, isolamento per famiglia, protezione dei dati e test di autorizzazione sono presenti dal primo incremento. Uso locale su questo PC Windows, con Qwen sulla stessa macchina. Alla consegna sono richiesti build riproducibile, avvio locale del pacchetto completo e semplice backup manuale del DB e degli originali, con istruzioni per reimportarlo; nessun piano di disaster recovery o backup automatico. Linux resta portabilità futura/CI, senza server di produzione separato richiesto.

**Confermato durante la revisione:** per ora è sufficiente condividere il link locale, anche per aprire la UI responsive dallo smartphone nella rete domestica, con login individuale, PC acceso e applicazione avviata. Nessuna app mobile dedicata o pubblicazione Internet richiesta. L'utente potrà valutare in futuro un hosting online; servizio e modalità non sono scelti e non fanno parte dell'implementazione attuale.

## Fuori fase

Salvadanai virtuali, budgeting avanzato, monitoraggio patrimoniale, broker, Open Banking, MCP e assistente conversazionale. Si preservano confini estendibili senza implementare queste funzioni, endpoint o tabelle speculative.

## Assunzioni proposte

**Confermato durante la revisione:** conti, movimenti canonici e report soltanto in EUR nella fase 1; niente conversioni o aggregazioni multivaluta. Un acquisto effettuato in valuta estera è rappresentato dall'importo effettivamente contabilizzato in euro sul conto.

Restano proposte: una sola famiglia operativa iniziale, ma controlli backend su appartenenza al nucleo; movimenti contabilizzati come base dei report. Queste assunzioni richiedono revisione prima delle rispettive funzioni, non impediscono la stesura delle specifiche o un bootstrap neutro.
