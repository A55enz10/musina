# ADR 0002 — Osservazioni sorgenti distinte dai movimenti

Data: 26 novembre 2026. **Stato: vincoli confermati dall'utente; meccanismi proposti, non implementati.**

## Contesto

Gli export possono sovrapporsi; righe identiche possono rappresentare eventi distinti. Quickard può riportare operazioni del mese precedente. Alla stesura iniziale i campioni non erano disponibili; successivamente l'utente ha fornito due XLSX e un QIF locali, con prima ispezione delle intestazioni ma senza validazione completa dei tracciati. Non si possono presumere ID univoci o nomi di colonna non osservati.

## Decisione confermata

Distinguere righe sorgenti e movimenti canonici, conservando provenienza; assicurare idempotenza e annullamento, senza deduplicazione basata soltanto su data/descrizione/importo. Distinguere copertura temporale e riconciliazione del saldo; il disponibile non sostituisce il contabile. Parsing deterministico per adapter, senza IA.

Confermato durante la revisione: conservare anche gli estratti originali nell'applicazione, collegati ai batch e accessibili soltanto agli utenti autorizzati della famiglia; includerli nei backup, escluderli da Git e dalle richieste IA. Utilizzabili per ricontrollo e riprocessamento con anteprima, senza modifiche automatiche dello storico. Modalità fisica di archiviazione da definire. L'utente ha escluso un piano di disaster recovery: backup minimo manuale di DB e originali, con istruzioni di reimportazione, senza pianificazione o rotazione automatica delle copie.

Confermata anche la conferma dell'intero batch senza importazioni parziali: errori bloccanti in alcune righe impediscono di importare anche le altre, fino alla loro risoluzione e nuova validazione. Le classificazioni mancanti non sono errori bloccanti; righe informative o di intestazione riconosciute dall'adapter non sono movimenti da importare e non costituiscono importazione parziale.

L'utente ha respinto la conservazione di un batch annullato: l'annullamento è eliminazione completa di importazione, originale, righe sorgenti e movimenti/dati derivati esclusivi. Nessun soft delete o recupero dall'operazione annullata nell'applicazione. I movimenti sostenuti da altre importazioni rimangono, con i collegamenti residui. Ricalcolare report, copertura, riconciliazione e trasferimenti incompleti; un nuovo upload è una nuova importazione ordinaria.

Confermato un mese iniziale di monitoraggio della copertura, scelto dall'utente per ciascun conto. Segnalazioni di periodi mancanti da quel mese, senza richiedere estratti precedenti; nessun effetto sui movimenti consultabili o sull'appartenenza ai gruppi. Il periodo anteriore non monitorato non equivale a una copertura completa.

## Meccanismo proposto

Più osservazioni possono collegarsi a un canonico. Digest del file per riconoscere il reupload esatto nel conto; eventuali ID bancari solo se verificati; matching per occorrenze e contesto, con revisione delle ambiguità. Conferma atomica e controllo concorrenza. La cancellazione completa deve coordinare DB e archivio degli originali senza ricreazioni da retry o risposte IA tardive.

## Conseguenze

Più dati e stati rispetto a un semplice elenco di righe, ma ogni totale rimane spiegabile e reversibile. Nessuna contropartita bancaria sintetica per chiudere trasferimenti o scarti. L'assenza di evidenze può impedire una conferma automatica o una riconciliazione: va rappresentata, non risolta inventando dati. Dettagli in [IMPORTS.md](../IMPORTS.md).
