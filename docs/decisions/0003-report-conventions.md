# ADR 0003 — Cassa, consumi e altre uscite

Data: 26 novembre 2026. **Stato: date di visualizzazione, EUR esclusivo, gruppi senza decorrenza, PAC separato dai consumi, rata mutuo intera, rimborsi degli acquisti e trattamento dei non classificati confermati nelle sezioni seguenti; altre convenzioni proposte, non implementate.**

## Contesto

Ogni addebito modifica la cassa; la fase 1 distingue i versamenti PAC dai consumi e tratta il mutuo come una rata intera, senza analizzarne le componenti. I trasferimenti devono dipendere dal perimetro del report e possono attraversare mesi diversi. I requisiti confermati di esclusione dei trasferimenti da redditi/spese e di inclusione delle commissioni tra le spese rimangono vincolanti.

## Decisione confermata sulle date

La data di riferimento è selezionabile in visualizzazione fra quelle effettivamente disponibili. Filtri, aggregazioni e drill-down usano la stessa scelta. Distinguere contabile, valuta ed eventuale operazione; non inventare date assenti. L'utente riferisce che gli estremi dell'estrazione mensile seguono la data contabile: la copertura dell'export non cambia al cambiare della vista e non prova completezza sulle altre date.

L'ispezione iniziale dei campioni mostra `DATA CONT.` / `DATA VAL.` in un XLSX e `DATA OP.` nell'XLSX denominato Quickard; semantica e fonte da validare completamente. Confermata anche la data contabile come selezione iniziale dove disponibile. Il comportamento delle viste con date non disponibili resta da definire dopo la validazione della fonte, senza sostituzioni silenziose. La riconciliazione dei saldi mantiene i propri confini contabili, indipendenti dal selettore.

## Decisione confermata sulla valuta

Solo conti, movimenti canonici e report in EUR nella fase 1, senza conversioni o aggregazioni multivaluta. Gli acquisti in valuta estera entrano per l'importo effettivamente contabilizzato in euro dalla banca; nessun cambio calcolato da Musina. Non interpretare valori in altre divise come euro.

## Decisione confermata sui gruppi

L'appartenenza di un conto a un gruppo si applica all'intero storico, senza intervalli di validità o date di decorrenza nei report. La relazione molti-a-molti prevede una data di inserimento come metadato, senza effetti sui totali e senza una decorrenza selezionabile dall'utente. Nome tecnico proposto `inserted_at`, istante UTC. Non rappresenta un cambio di proprietario del conto. Un'eventuale futura gestione temporale resta una possibilità da revisionare, non una funzione della fase 1.

## Decisione confermata sui versamenti PAC

I versamenti PAC sono uscite di cassa per investimenti, separate dai consumi sotto «Investimenti / Versamenti PAC». Esempio: acquisti per 1.000 euro e PAC per 200 euro comportano 1.200 euro di uscite, suddivise in 1.000 euro di consumi e 200 euro di versamenti per investimenti. Nessun monitoraggio di quote, rendimenti o valore degli investimenti nella fase 1.

## Decisione confermata sulla rata mutuo

Una sola uscita per l'intero importo in «Mutuo / Rata». Nessuna distinzione tra capitale e interessi, nessuna scomposizione richiesta o stato provvisorio per mancanza di dettaglio; nessun piano di ammortamento o debito residuo. La proposta iniziale di separare le componenti della rata è stata respinta dall'utente e sostituita da questa decisione. Eventuali commissioni bancarie con addebito separato mantengono il trattamento generale delle spese.

## Decisione confermata sui rimborsi degli acquisti

Il rimborso riduce la categoria di spesa dell'acquisto nel periodo della propria data selezionata nella vista. Non riscrive il periodo dell'acquisto e non conta come reddito. Acquisto di abbigliamento per 100 euro a gennaio e rimborso di 30 euro a febbraio: gennaio resta a 100, febbraio ha spesa netta -30 e i due mesi totalizzano 70. Le categorie possono quindi avere importi netti negativi nel periodo del rimborso.

## Decisione confermata sui non classificati

L'importazione può essere confermata anche con movimenti non classificati, se i dati bancari sono validi. I movimenti restano sempre nei totali di cassa e compaiono in «Da classificare» con conteggio e importi, correggibili successivamente. Non diventano automaticamente consumi o redditi finché il loro significato non è noto. La classificazione mancante è distinta da errori di parsing, validazione bancaria o identità: questi ultimi possono ancora bloccare l'importazione.

## Decisione confermata sul riepilogo mensile

Entrate, uscite complessive comprensive di rata mutuo e PAC, differenza entrate meno uscite, con dettaglio separato di spese ordinarie, mutuo e versamenti per investimenti. Escludere i giroconti interni. Esempio approvato: 3.000 euro di entrate, 1.000 di spese ordinarie, 800 di mutuo e 200 di PAC → 2.000 di uscite e 1.000 di differenza. La differenza non è un saldo bancario o una variazione patrimoniale.

## Decisione confermata sul collegamento dei giroconti

Collegamento automatico soltanto nei casi univoci con riferimenti affidabili ai conti. Data e importo coincidenti da soli non sono sufficienti; non presumere che ogni bonifico sia interno. Casi dubbi «Da verificare» e revisione manuale, senza inventare movimenti per chiudere un lato mancante. I criteri di matching specifici per fonte restano da definire e verificare sui campioni.

## Decisione confermata sulle altre entrate

Il totale delle entrate del riepilogo include redditi e altre entrate, con dettaglio separato. Esempi di altre entrate: disinvestimenti e prestiti ricevuti, senza rendimenti, plusvalenze o debiti residui. Stipendio 3.000 euro e disinvestimento 500 → totale 3.500, redditi 3.000 e altre entrate 500. Rimborsi degli acquisti riducono le spese, trasferimenti separati. Nella notazione di dominio, entrate `R + A`, uscite `C + O`, differenza `R + A - C - O`.

## Altre proposte

Usare importi esatti firmati e perimetro come insieme distinto di conti. Separare viste di cassa da redditi, consumi, altre uscite, altri afflussi e trasferimenti; evidenziare natura ignota e categoria mancante. Dettagli di collegamento dei rimborsi, rettifiche e restituzioni di altre uscite da definire senza cambiare la convenzione confermata sui rimborsi degli acquisti.

La classificazione tecnica delle quote deve rispettare le formule del riepilogo: entrate `R + A`, uscite `C + O`, differenza `R + A - C - O`. `A` è già compreso nelle entrate e non va aggiunto una seconda volta. Trasferimenti di perimetro e non risolti sono sezioni separate, senza nascondere gli importi o attribuire natura economica in base al solo segno.

Non creare lati mancanti di un trasferimento e non spostare date per far quadrare un mese. Mostrare i conti inclusi nel perimetro del report.

## Conseguenze e revisione

Grafici, drill-down e saldo di cassa devono soddisfare la medesima identità di controllo. Le metriche possono essere negative per rimborsi o trasferimenti in transito; la UI deve spiegarlo. Richiede revisione prima di implementare le relative convenzioni: [DOMAIN.md](../DOMAIN.md) è la specifica delle formule e degli esempi. L'approvazione di questa proposta non aggiunge monitoraggio patrimoniale alla fase 1.
