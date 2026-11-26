# Importazioni, copertura e riconciliazione

Confermati: priorità agli XLSX MPS e Quickard, adapter modulari, parsing deterministico, anteprima, validazione, provenienza, idempotenza, sovrapposizioni, riconciliazione e annullamento. **Campioni locali disponibili in `docs/examples/`, posizione indicata esplicitamente dall'utente ed esclusa interamente da Git:** due XLSX e un QIF. Nessun adapter implementato; prima ispezione delle intestazioni, senza validazione completa dei tracciati o divulgazione dei dati bancari.

Intestazioni data osservate: `DATA CONT.` e `DATA VAL.` in un XLSX; `DATA OP.` nell'XLSX denominato Quickard. Nel primo XLSX non sono state rilevate intestazioni di data operazione; ciò non dimostra l'assenza di qualunque informazione equivalente. Il nome del secondo file è un indizio di fonte, non una verifica semantica. L'associazione del primo XLSX a MPS, la semantica precisa delle date Quickard e gli altri dettagli di layout restano da confermare. Il QIF è aggiuntivo e non modifica la priorità XLSX della fase 1; nessun supporto QIF deciso.

## Prima di implementare un adapter

Analizzare i campioni nel percorso locale protetto autorizzato, senza inviarli ai provider IA; preferire percorsi esterni al repository per acquisizioni future. Documentare versione del layout, fogli, intestazioni reali, tipi celle, date contabili/valuta/operazione disponibili e loro significato, segni, separatori numerici, valuta, eventuali identificativi, intervallo dichiarato e tipi di saldo. Verificare esportazioni ripetute e sovrapposte, righe identiche legittime, casi senza movimenti, eventuali storni e formule. Creare fixture sintetiche equivalenti alla struttura osservata e revisionate, senza copiare dati personali.

La validazione del tracciato MPS precede il completamento dell'incremento 2; quella di Quickard precede il relativo adapter nell'incremento 4. I campioni presenti non costituiscono ancora una validazione e potrebbero richiedere esportazioni aggiuntive. Questo lavoro non blocca il bootstrap. Non costruire un finto «adapter MPS» su colonne ipotetiche per dichiarare completo l'import.

## Contratto concettuale dell'adapter — proposto

Rilevazione della fonte/versione, lettura sicura del workbook, estrazione di righe osservate e metadati con evidenze, conversione deterministica nel modello intermedio e diagnostica localizzabile. L'adapter non scrive direttamente movimenti, non sceglie categorie e non consulta IA. Il servizio applicativo gestisce confronto, conferma e persistenza. Layout sconosciuti o più adapter compatibili richiedono selezione/risoluzione esplicita, non tentativi silenziosi.

Ogni riga conserva batch, posizione (foglio/riga, come localizzatore e non identità bancaria), dati necessari della fonte, valori interpretati, versione parser e diagnostica. Ogni movimento canonico può avere più collegamenti a righe sorgenti di diversi batch; una riga può contribuire anche a osservazioni saldo o essere esclusa con motivo. Le quote di split restano derivate dal movimento, senza moltiplicare righe sorgenti.

Date seriali o testuali, sistema date del workbook e importi devono essere interpretati solo secondo il layout verificato; non affidarsi alla lingua del sistema operativo. Valori ambigui o scale inattese producono errori, mai tentativi IA o arrotondamenti. Non eseguire macro, formule o link esterni; se una formula contiene dati necessari senza un valore attendibile, rifiutare o richiedere un export supportato. Limiti su dimensione compressa/decompressa, numero di fogli/righe, tempo e tipo di file proteggono da file ostili. Mostrare descrizioni come testo, senza HTML attivo.

## Flusso e stati — proposto

1. Utente autenticato sceglie conto/fonte e carica il file. Verifica autorizzazioni, limiti, formato e appartenenza del conto; il nome del file non prova l'identità del conto.
2. Stato bozza/analisi: crea batch e righe in staging, senza effetti sui report. Valida mapping del conto, tipi, date, importi, saldo se presente, numero di righe e anomalie.
3. Anteprima: nuovi movimenti, osservazioni già note, candidati duplicati, differenze, righe escluse e motivi; mostra copertura e riconciliazione come indicatori separati. **Confermato durante la revisione:** errori bloccanti anche su una sola riga impediscono la conferma dell'intero batch; nessuna importazione parziale delle sole righe valide. Mostrare le righe e i motivi, risolvere gli errori e ripetere la validazione prima di confermare. Categoria o natura ancora ignote non bloccano la conferma se i dati bancari sono validi; i movimenti entrano nei totali di cassa e nella sezione «Da classificare» con quantità e importi, completabili in seguito. Non confondere classificazione incompleta con parsing errato o ambiguità di identità.
4. Risoluzione: per ogni sovrapposizione ambigua distinguere «stesso movimento» da «movimento distinto» con evidenza/decisione tracciata. Un periodo sovrapposto non è automaticamente un errore.
5. Conferma esplicita atomica: ricontrolla versione dell'anteprima, idempotenza e concorrenza per conto; salva righe, collegamenti e movimenti una volta sola. La conferma ripetuta restituisce lo stesso risultato. Report leggono solo dati confermati.
6. Batch confermato o fallito con autore/istante/esito. Un errore tecnico non deve lasciare mezzo batch; retry sicuro. **Confermato durante la revisione:** annullare significa eliminare l'importazione e i suoi dati esclusivi, non conservarla con stato «Annullata»; vedere le dipendenze sotto.

**Confermato durante la revisione:** l'applicazione conserva anche gli estratti originali caricati, collegati al relativo batch, con accesso protetto e inclusione nei backup. Restano fuori da Git, log e richieste IA. La conservazione permette controlli sulla fonte e un eventuale riprocessamento con adapter migliorato; non autorizza modifiche automatiche dello storico, che richiedono confronto e conferma. Conservare l'originale non sostituisce righe sorgenti, movimenti canonici e provenienza.

Archiviazione protetta e persistente, senza esposizione via static resources; ogni accesso verifica famiglia e permessi. Modalità fisica di archiviazione ancora da scegliere, senza servizi aggiuntivi obbligatori. La conservazione riguarda le importazioni presenti: eliminare un'importazione elimina anche l'originale dall'archivio applicativo. Per anteprime non confermate e caricamenti falliti definire una semplice pulizia dei temporanei nell'implementazione. Nessuna scadenza automatica degli originali delle importazioni presenti è stata decisa. Backup manuali gestiti dall'utente, senza pianificazione o rotazione automatica; il ripristino di una copia precedente riflette i dati che essa contiene, senza sincronizzazione retroattiva delle cancellazioni.

## Idempotenza e identità

Il digest crittografico del file può riconoscere il ricaricamento esatto **per famiglia e conto**, non identificare un movimento. Identità della richiesta e vincoli database impediscono doppie conferme concorrenti. Dopo l'eliminazione, caricare lo stesso file avvia una nuova importazione con normale anteprima e conferma, senza ripristinare un batch nascosto; confrontare comunque i movimenti rimasti da altre importazioni. Un retry tardivo della precedente conferma non deve ricreare il batch eliminato.

Se esistono ID bancari attendibili, usarli solo dopo aver verificato unicità, stabilità e ambito (conto/fonte). In assenza di ID affidabili, data/descrizione/importo sono indizi per il confronto, non una chiave univoca. Confrontare insiemi di occorrenze, sequenza e contesto disponibili; associare una riga a un canonico con corrispondenze uno-a-uno per ciascuna osservazione del file. Non collassare due occorrenze identiche nello stesso estratto.

Esempio sintetico: primo export contiene due addebiti identici da 10 euro; devono esistere due movimenti. Un export sovrapposto che ne mostra due può osservare gli stessi due eventi. Se ne mostra tre, non sopprimere la terza occorrenza: proporre un movimento nuovo solo con evidenza sufficiente, altrimenti richiedere revisione. Se l'export cambia una descrizione o riordina le righe, non creare automaticamente nuovi eventi né sovrascrivere quelli esistenti.

Nei casi indistinguibili non esiste una deduplicazione infallibile: rendere visibile l'incertezza e registrare la risoluzione. Conteggi e saldi aiutano, ma non provano da soli l'identità di ogni riga. Vincoli su hash di data/descrizione/importo sono vietati.

## Copertura temporale

Proposta: per conto e intervallo registrare stato **non noto**, **parziale** o **completo dichiarato**, fonte dell'evidenza e possibili lacune. Distinguere intervallo dichiarato dall'estratto, estremi osservati dei movimenti e intervallo atteso scelto dall'utente. La data minima/massima dei movimenti non dimostra la copertura completa; neppure un mese senza righe dimostra assenza di spese.

**Confermato durante la revisione:** mese iniziale di monitoraggio configurabile per ciascun conto. Esempio: da gennaio 2025 segnalare i periodi mancanti a partire da quel mese, senza chiedere estratti precedenti. Non inferire il mese iniziale dalla prima o ultima riga e non usarlo come filtro sui movimenti o come prova di copertura. È distinto sia dall'apertura effettiva del conto sia dall'appartenenza ai gruppi, che non ha decorrenza nei report.

L'utente riferisce che l'estrazione mensile è delimitata per data contabile e include due date: conservarle separatamente senza attribuire alla seconda un significato non verificato. Il cambio della data in visualizzazione non cambia il criterio di copertura dell'estratto. Esempio ipotetico: un movimento con valuta 31 gennaio e contabilizzazione 2 febbraio può comparire nella vista gennaio per valuta solo dopo aver importato anche l'export che include la contabilizzazione di febbraio. La copertura contabile completa di gennaio non dimostra la completezza di gennaio per valuta o per data operazione; niente margine fisso di giorni assunto sufficiente per completarla.

Quickard può includere operazioni del mese precedente: conservare le date effettivamente presenti e usare in visualizzazione quella selezionata, mai il nome/mese del file. Non interpretare `DATA OP.` come data contabile senza verifica. Non estendere la completezza al mese precedente solo perché compaiono alcune righe di quel mese. Un export parziale o sovrapposto può aggiungere evidenza senza completare un periodo.

Nei gruppi, mostrare il dettaglio di copertura e del mese iniziale per ciascun conto. I periodi anteriori al monitoraggio di un conto sono «Non monitorati», non «Completi» o «Mancanti». Non dichiarare completo tutto il gruppo se il periodo non è dimostrato per tutti i conti inclusi; l'assenza di avvisi per un conto non monitorato non certifica la sua completezza. Eliminare un'importazione ricalcola la copertura usando solo evidenze rimaste.

## Riconciliazione dei saldi

Conservare il tipo di saldo della fonte: **contabile**, **disponibile** o **non determinato**. Il disponibile può incorporare vincoli e operazioni non contabilizzate: non è un saldo contabile e non si usa nella stessa equazione. Se tipo o convenzione non sono verificati, stato «non verificabile», senza convertire il valore.

Per due osservazioni contabili coerenti con valuta e confini noti: `saldo finale atteso = saldo iniziale + Σ importi canonici attivi nell'intervallo di riconciliazione`; `scarto = saldo finale osservato - saldo finale atteso`. La somma usa i movimenti bancari una sola volta, inclusi trasferimenti/commissioni, senza aggiungere anche gli split. Scarto zero con precisione monetaria esatta; nessuna tolleranza inventata.

Prima di applicare la formula, verificare se le osservazioni sono di apertura/chiusura e quali movimenti del giorno includono. Se la semantica della fonte non è nota o mancano saldi/intervallo attendibili, non dichiarare riconciliato. Un saldo iniziale inserito dall'utente è un'osservazione manuale etichettata, non un accredito o reddito; non generare movimenti per azzerare uno scarto.

Stati proposti: non verificabile, da verificare, riconciliato, scarto presente. Copertura completa e riconciliazione non sono equivalenti: saldi compatibili possono nascondere omissioni compensate; un periodo coperto può comunque avere uno scarto. Ricalcolare esito dopo import, annullamento e correzione dei collegamenti. Saldi previsionali/disponibili non vanno presentati come prova del saldo contabile.

## Eliminazione dell'importazione e dipendenze

**Confermato:** eliminazione completa dell'importazione, senza soft delete o batch «Annullato» conservato nell'applicazione. Eliminare batch, originale, righe sorgenti, evidenze di copertura/saldo e collegamenti esclusivi. Eliminare anche i movimenti che perdono l'ultima provenienza e i relativi split, classificazioni e suggerimenti; non mantenerli nascosti per un recupero successivo. La proposta precedente di conservare l'operazione annullata è stata respinta.

I movimenti sostenuti anche da altre importazioni restano, con i loro dati e la provenienza residua: non sono esclusivi del batch eliminato. Regole ed esercenti riutilizzabili non vanno cancellati a cascata. Ricalcolare report, copertura e riconciliazioni. Un trasferimento il cui lato viene eliminato diventa incompleto, senza contropartite fittizie; conservare il lato bancario ancora valido.

**Proposta tecnica:** operazione autorizzata e ripetibile, con gestione coerente della cancellazione DB e del file secondo lo storage scelto; nessun esito di completamento prima della rimozione dell'originale. Retry o risposte IA tardive non devono ricreare i dati eliminati. La correzione di un parser resta un distinto riprocessamento con confronto e conferma, non una cancellazione automatica dello storico.

## Scenari minimi di prova

File ripetuto; doppia conferma concorrente; due righe identiche legittime; sovrapposizioni parziali e totali; ordine cambiato; ID mancanti/non univoci; layout sconosciuto; date/importi ambigui; file ostile; Quickard con mese precedente; periodo senza righe; saldo disponibile distinto; scarto e confini giornalieri; rollback a metà conferma; annullamento con provenienza condivisa; lato trasferimento perso; report ricalcolati. Le fixture bancarie dipendono da tracciati osservati, non da intestazioni ipotizzate.
