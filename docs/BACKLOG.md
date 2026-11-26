# Backlog per incrementi utilizzabili

Stato al 27 novembre 2026: incremento 1 completato e verificato su Windows sul branch `chore/bootstrap`. Incrementi 2–8 progettati, non implementati. Campioni locali preservati e non usati dal bootstrap. Commit, integrazione su master e push del solo master autorizzati dall'utente; nessun incremento successivo autorizzato.

Ogni incremento richiede branch dedicato, ambito circoscritto e aggiornamento di questo documento e di [STATUS.md](STATUS.md). «Completato» significa criteri soddisfatti e prove eseguite con esito registrato; mantenere separati progettazione, implementazione e verifica. Per tutte le nuove API: casi autenticato, non autenticato, permesso insufficiente e risorsa di altra famiglia. Le prove bancarie usano fixture sintetiche conformi a layout realmente osservati.

## 1. Bootstrap eseguibile

**Stato:** completato e verificato su Windows: 8 test backend, 4 frontend, smoke browser del JAR e del proxy di sviluppo, build da copia pulita dei sorgenti con dipendenze bloccate. **Dipendenze:** autorizzazione ricevuta; JDK 25, Node e Docker disponibili, versioni verificate su registri ufficiali e nella build; nessun campione bancario necessario. [Evidenze e limiti](verification/bootstrap.md): CI remota/Linux e click IntelliJ non verificati, configurazioni condivise presenti e task corrispondenti provati da CLI.

**Risultato:** un utente accede e vede una pagina italiana che verifica il collegamento al backend; sviluppo e build riproducibili. Incarico operativo in [BOOTSTRAP_TASK.md](BOOTSTRAP_TASK.md).

**Accettazione:**

- Un progetto IntelliJ, `backend/`, `frontend/`, Wrapper radice e PostgreSQL locale tramite Compose; migrazioni Flyway all'avvio.
- Avvii backend/frontend separati da IntelliJ con configurazioni `.run/` condivisibili, senza segreti o percorsi personali; proxy `/api` funzionante.
- Accessi individuali persistenti, provisioning controllato senza signup pubblico, controlli di famiglia e CSRF, pagina protetta e logout; due famiglie sintetiche nei test.
- Ruoli membro/amministratore: gestione contabile condivisa per entrambi, gestione degli accessi riservata all'amministratore; nessun ruolo di sola consultazione. Il ruolo non limita la visibilità dei conti nella famiglia.
- Un comando radice esegue verifiche e produce il pacchetto Boot con React; avvio del pacchetto con UI/API allo stesso indirizzo senza dev server Node.
- Test essenziali e CI automatica; guida da ambiente pulito Windows e procedura Linux/CI, prerequisiti e versioni effettive registrati.

**Prove:** integrazione PostgreSQL reale e Flyway su DB vuoto e al riavvio; login/logout e autorizzazioni/CSRF, accesso del membro alle funzioni condivise implementate e diniego della gestione accessi ai non amministratori; frontend con backend disponibile/indisponibile; smoke browser del flusso autenticato; build e smoke del pacchetto completo, inclusi errori `/api` e rotta SPA. Nessuna prova può essere dichiarata eseguita solo perché è stata scritta in CI.

## 2. Un conto MPS → importazione → movimenti → primo grafico

**Punto registrato, non bloccante per il bootstrap — DATE-MIX (completamento in 6):** definire il comportamento predefinito quando un report comprende conti con tipi di data diversi. Mostrare chiaramente conti/date non disponibili, evitare fallback silenziosi o esclusioni nascoste, preservare coerenza tra filtro, grafico e drill-down. Prove: gruppo con data contabile/valuta su un conto e sola data operazione su un altro; cambiare data non altera copertura o riconciliazione.

**Stato:** progettato, completamento dipendente dalla validazione del tracciato MPS sui campioni disponibili ed eventuali integrazioni. **Dipendenze:** 1; fonte/tracciato MPS confermati e fixture revisionata; revisione delle convenzioni minime data/valuta/segni in DOMAIN.md.

**Risultato:** un primo conto produce dati consultabili dall'export reale supportato, senza IA.

**Accettazione:**

- Creazione conto; adapter MPS per il layout documentato, upload protetto, anteprima con righe valide/escluse/errori e conferma atomica.
- Errori bloccanti su una o più righe impediscono la conferma dell'intero batch, con righe e motivi visibili; nessuna modalità di importazione delle sole righe valide. Ripetere la validazione dopo la risoluzione; distinguere errori da classificazioni mancanti e righe informative del layout.
- Conservazione protetta e persistente dell'originale collegato al batch, recuperabile con autorizzazione sulla famiglia; esclusione da Git, log e richieste IA. Documentare un'esportazione manuale locale del DB e degli originali per l'uso con dati reali; nessun sistema automatico di backup. Rifinitura e verifica funzionale essenziale nell'incremento 8.
- Conto e movimenti in EUR; acquisti in altra valuta importati per l'addebito bancario in euro. Nessuna conversione automatica o interpretazione di importi non EUR come euro.
- Provenienza riga→movimento e idempotenza del file esatto già presenti; nessuna unicità data/descrizione/importo. Per sovrapposizioni ancora non risolvibili bloccare la conferma con spiegazione.
- Elenco movimenti paginato, filtri conto/periodo e scelta della data disponibile (inizialmente contabile, dove presente), categoria manuale iniziale e indicazione non classificati; nessuna regola creata implicitamente.
- Import confermabile con classificazione mancante se i dati bancari sono validi; movimenti nei totali di cassa e in «Da classificare» con quantità/importi, completabili in seguito. Nessuna attribuzione automatica a consumi o redditi per il solo segno.
- Primo grafico mensile entrate/uscite con distinzione cassa/economico, natura da chiarire visibile e drill-down; trasferimenti sospetti non nascosti. Copertura e saldi non ancora verificabili mostrati come tali.

**Prove:** parsing numerico/date in locale Windows e CI Linux; import con due righe identiche, reupload e retry della conferma; file con righe valide e almeno una riga bancaria invalida che non produce alcun movimento attivo, poi conferma completa dopo risoluzione e nuova validazione; errori senza scritture parziali; import confermato con non classificati presenti nei totali e nella relativa sezione senza doppio conteggio, successiva classificazione senza variazione dei totali di cassa; cambio data con coerenza fra filtro, grafico e drill-down; nessun payload bancario nei log. I test di layout non precedono l'evidenza del tracciato.

## 3. Robustezza importazioni e riconciliazione

**Stato:** progettato. **Dipendenze:** 2; esempi sovrapposti e semantica dei saldi MPS disponibili o assenze documentate.

**Risultato:** aggiornare e correggere uno storico mantenendo identità e saldi spiegabili.

**Accettazione:**

- Sovrapposizioni, occorrenze identiche e matching con evidenze; ambiguità risolte dall'utente, nessuna deduplicazione distruttiva.
- Copertura per conto/intervallo con evidenza, criterio temporale e lacune; cambio data della vista senza estendere la completezza alle altre date. Riconciliazione separata, con riferimento contabile, tipo saldo e scarto esatto oppure «non verificabile» motivato.
- Mese iniziale di monitoraggio configurabile per conto: segnalare periodi mancanti da quel mese, senza richieste anteriori. Non filtrare i movimenti né attribuire completezza in base alla configurazione; nei gruppi distinguere periodi non monitorati, mancanti e copertura dimostrata per conto.
- Annullamento come eliminazione completa del batch, originale, righe e movimenti/dati derivati esclusivi; nessun soft delete o batch «Annullato» conservato. Preservare movimenti sostenuti da altre importazioni e regole riutilizzabili, ricalcolare report/copertura/saldi. Un successivo upload segue il normale flusso di nuova importazione.
- Concorrenza per conto gestita e preview obsolete rifiutate; limiti XLSX, gestione dei file e retention documentate prima di dati reali persistenti.
- Originali e collegamenti integri per le importazioni presenti; rimozione dell'originale quando si elimina il batch. Nessuna cancellazione implicita o modifica automatica dello storico per un aggiornamento dell'adapter; riprocessamento distinto con confronto e conferma.

**Prove:** sovrapposizioni totali/parziali, tre occorrenze contro due, righe riordinate/modificate, ID fonte non univoci; due conferme concorrenti; rollback per errore simulato. Eliminazione che rimuove batch, originale e dati esclusivi senza residui applicativi nascosti; movimenti condivisi e relative classificazioni conservati; nuovo upload dopo eliminazione, retry e risposta IA tardivi senza ricreazione del batch. Periodo senza movimenti, scarto nullo/non nullo e saldo disponibile non usato nella riconciliazione contabile; file malformati/espansione eccessiva.

Per il monitoraggio: conto da gennaio 2025 senza avvisi per mesi precedenti, lacune da gennaio segnalate, modifica del mese senza perdita o esclusione di movimenti, gruppo con mesi iniziali diversi senza falsa attestazione di completezza.

## 4. Quickard, gruppi e trasferimenti

**Stato:** progettato, adapter dipendente dalla validazione del campione denominato Quickard ed eventuali integrazioni. **Dipendenze:** 3; fonte/tracciato e significato di `DATA OP.` verificati; revisione delle convenzioni di perimetro e matching.

**Risultato:** consultare insieme conto e prepagata, riconoscendo le ricariche senza duplicare le spese.

**Accettazione:**

- Adapter Quickard modulare, con gestione delle operazioni del mese precedente; conti wallet configurabili anche senza adapter automatico dedicato.
- Gruppi senza annidamento, stesso conto in più gruppi, unione per ID distinta. Appartenenza applicata a tutto lo storico, senza decorrenza nei report; data di inserimento nella relazione n:n come metadato, senza filtro temporale o campo UI di decorrenza. Personale/comune/famiglia configurabili dall'utente, senza flag aggiuntivi sui movimenti.
- Collegamento automatico dei giroconti solo se univoco e sostenuto da riferimenti affidabili ai conti; data/importo da soli insufficienti. Casi dubbi «Da verificare» con conferma manuale, provenienza automatica visibile e correggibile; conto esterno al report separato, singolo lato/controconto ignoto espliciti.
- Commissioni separate, anche tramite split documentato di un addebito aggregato; collegamenti mai ottenuti creando movimenti sintetici.

**Prove:** conto in due gruppi non duplicato; conto aggiunto al gruppo incluso anche nei periodi precedenti alla data di inserimento della relazione, conto rimosso escluso dall'intero report del gruppo, senza modificare i movimenti del conto; data di inserimento persistita ma priva di effetti sui totali. Giroconto univoco con riferimenti affidabili collegato automaticamente; stessa data/importo senza riferimenti o candidati multipli lasciati da verificare; correzione del collegamento automatico. Ricarica, bonifico a fornitore, un solo lato, mese/anno a cavallo, commissione documentata/non documentata; annullamento di un lato; Quickard con righe precedenti che non attestano copertura dell'intero mese precedente.

## 5. Regole ed esercenti riutilizzabili

**Punto registrato, non bloccante per il bootstrap — CATEGORY-NATURE:** categoria con natura economica predefinita, ereditata quando si assegna la categoria per evitare una doppia classificazione manuale. Definire precedenza di sottocategoria/override e protezione delle scelte già confermate; variazioni del default sullo storico con anteprima. Prove: una sola assegnazione imposta categoria e natura, override manuale/split non sovrascritti.

**Stato:** progettato. **Dipendenze:** 4; revisione delle precedenze e della tassonomia; categorie manuali minime da 2 e split tecnici da 4.

**Risultato:** ridurre il lavoro ripetitivo mantenendo il controllo sulle classificazioni.

**Accettazione:**

- Categorie/sottocategorie modificabili, normalizzazione esercenti e alias persistenti; storico preservato quando una categoria è disattivata.
- Correzione singola e salvataggio regola come azioni distinte; esempio Dolci Fate riusato senza IA; Amazon non categorizzato con certezza dal solo nome.
- Regole con precedenze e conflitti espliciti; applicazione allo storico con preview, controllo versione e conferma, escludendo manuali confermati.
- Split generali con importi esatti e somma invariata; stato/provenienza di classificazione visibili; riavvio dell'app non perde regole.

**Prove:** persistenza dopo riavvio, precedenze e conflitti, cambio alias senza fusioni indebite, manuale non sovrascritto (inclusi split), applicazione su preview obsoleta, arrotondamenti/ripartizioni invalidi rifiutati, edit singolo che non genera regole, categorie disattivate.

## 6. Report completi

**Punto registrato, non bloccante per il bootstrap — NEGATIVE-CHARTS:** scegliere e verificare grafici compatibili con categorie nette negative per rimborsi. Nessuna trasformazione in valore assoluto o azzeramento; metriche e drill-down devono coincidere. Prove: categoria a -30 euro, periodo con soli rimborsi e confronto con categorie positive; evitare rappresentazioni che richiedono valori tutti positivi.

**Stato:** progettato. **Dipendenze:** 5; revisione delle proposte residue di ADR 0003, formule e dettagli di collegamento dei rimborsi/rettifiche; qualità dati da 3 e trasferimenti da 4. EUR esclusivo, PAC separato dai consumi, rata mutuo intera e rimborsi degli acquisti nel periodo del rimborso già confermati.

**Risultato:** leggere i flussi familiari e risalire a ogni movimento e alla fonte.

**Accettazione:**

- Report per conto/gruppo, mese/anno/intervallo personalizzato; categorie, andamento mensile, confronto entrate/uscite con consumi e altre uscite separati.
- Riepilogo mensile con entrate, uscite comprensive di rata mutuo e PAC e differenza, con dettaglio separato; giroconti interni esclusi. Caso approvato 3.000 entrate, 1.000 spese ordinarie, 800 mutuo e 200 PAC → 2.000 uscite e 1.000 differenza. Non confondere la differenza con saldo bancario o patrimonio.
- Entrate complessive comprensive di redditi e altre entrate, con dettaglio distinto: disinvestimenti o prestiti ricevuti inclusi nel totale, senza calcolo di rendimenti/plusvalenze o debiti residui. Formule `I=R+A`, `Uᵣ=C+O`, `D=I-Uᵣ`; rimborsi a riduzione delle spese e trasferimenti separati. Verificare stipendio 3.000 e disinvestimento 500 → totale 3.500, senza doppio conteggio del dettaglio.
- Viste economiche e di cassa esplicitamente etichettate; trasferimenti di perimetro/incompleti, non classificati, anomalie e copertura visibili.
- Data di riferimento selezionabile fra quelle disponibili, indicata nella vista e preservata nel drill-down con gli stessi filtri/quote della misura; nessuna duplicazione per gruppi o split. Segnalare date assenti, completezza non dimostrata sulla data selezionata e mesi zero distinti da mesi non noti; nessun fallback silenzioso fra date.
- PAC in «Investimenti / Versamenti PAC», incluso nelle uscite di cassa e separato dai consumi, senza quote, rendimenti o valorizzazione. Mutuo come unica uscita «Mutuo / Rata» per l'importo totale, senza distinzione capitale/interessi, stato provvisorio o richiesta di scomposizione. Rimborsi degli acquisti a riduzione della categoria originaria nel periodo della propria data selezionata, senza modificarne il periodo d'acquisto né conteggiarli come reddito; ammesse categorie nette negative.

**Prove:** identità `Δ = R - C - O + A + Tᵢ + Tₚ + T? + N`; esempi numerici di DOMAIN.md; rata mutuo conteggiata una sola volta per intero senza generare componenti o avvisi di scomposizione; trasferimenti a cavallo mese, rimborsi maggiori dei consumi del mese, split e categorie mancanti, cambio perimetro, estremi intervallo/anno bisestile, assenza conversioni valutarie, uguaglianza tra grafici e drill-down dopo import/annullamento. Movimento contabilizzato in febbraio con valuta in gennaio visibile nel mese corretto per ciascuna vista; date non disponibili e gruppo con disponibilità eterogenea segnalati; riconciliazione contabile invariata al cambio della visualizzazione.

## 7. Suggerimenti IA

**Punto registrato, non bloccante per il bootstrap — AI-BACKGROUND:** generazione dei suggerimenti in background dopo la conferma persistita dell'importazione, con stato visibile; import e risposta di conferma non attendono l'inferenza. Definire esecuzione locale, retry limitati e idempotenza senza broker; cancellazione del batch e risposte tardive non ricreano dati eliminati. Prove: Qwen lento/spento, retry, batch eliminato durante l'inferenza e nessuna chiamata duplicata per casi risolti. Nessuna infrastruttura IA da implementare nell'incremento 1.

**Stato:** progettato; Qwen su questo PC Windows, insieme a Musina, confermato; runtime/modello da scegliere. **Dipendenze:** 5 e 6; verifica delle risorse della macchina, runtime, modello/versione/quantizzazione, trattamento dati locali, abilitazione e limiti di risorse; contratto della porta e dati minimi revisionati. Fonti e limiti in ARCHITECTURE.md, decisione in ADR 0004.

**Risultato:** assistenza opzionale per gli irrisolti senza perdita di operatività o controllo.

**Accettazione:**

- Regole prima dei suggerimenti; riuso persistente delle associazioni e nessuna nuova chiamata per Dolci Fate già risolto.
- Quando l'IA è abilitata, generazione automatica dopo ogni importazione confermata sui soli casi rimasti irrisolti, senza pulsante obbligatorio per avviarla. Nessuna chiamata su sola anteprima, batch fallito o reupload senza nuovi casi; errore IA senza effetti sulla conferma bancaria. La generazione automatica non equivale all'accettazione dei suggerimenti.
- Adapter sostituibile fuori dal dominio; modalità disabilitata senza credenziali; errori del provider non fermano import/report.
- Adapter iniziale verso Qwen locale, endpoint raggiungibile dal solo backend in rete controllata; nessun fallback cloud. Installazione e configurazione runtime nell'incremento 7 dopo verifica hardware e modello, senza renderli prerequisiti del bootstrap o delle funzioni contabili.
- Payload minimo ripulito, nessun estratto/IBAN/identificativo, nessuna descrizione reale integrale; testo non affidabile separato dalle istruzioni, risposta validata con categorie ammesse.
- Conferma/correzione o rifiuto utente, anche in blocco su più movimenti selezionati, senza applicare suggerimenti obsoleti; nessuna modifica autonoma di importi/date/saldi, manuali protetti. La conferma delle categorie non crea regole o associazioni riutilizzabili: «Ricorda questa associazione» come azione separata; limiti di chiamate/costo.

**Prove:** provider finto locale per timeout, indisponibilità, schema/categoria invalida e injection nella descrizione; verifica dei dati effettivamente inviati e dei log; avvio automatico dopo conferma solo se abilitata e su irrisolti, nessuna chiamata in anteprima o su batch fallito; niente duplicazione di chiamate su retry/reupload con suggerimento valido, errore IA che preserva import e consultazione. Nessuna chiamata se regola applicabile; nessuna sovrascrittura manuale; applicazione usabile senza IA. Provare anche il runtime Qwen effettivo con dati sintetici per qualità delle categorie italiane, risposta strutturata, latenza e memoria sull'hardware scelto; distinguere questi risultati dai test simulati. Runtime spento/modello assente senza blocco dell'applicazione e senza richieste cloud.

Verificare inoltre conferma/correzione multipla limitata ai movimenti selezionati, nessuna regola creata implicitamente, manuali e split protetti, suggerimenti obsoleti su movimenti modificati/eliminati non applicati e accessi fuori famiglia negati.

## 8. Rifinitura mobile, deployment, backup e ripristino

**Stato:** progettato. **Dipendenze:** 1–7 (7 completo anche nella modalità IA disabilitata); avvio per uso ordinario su questo PC Windows e politiche operative scelte prima dell'uso con dati reali. Linux è portabilità futura/CI, non una destinazione di deployment richiesta.

**Risultato:** prima versione familiare utilizzabile e recuperabile sulla macchina locale.

**Accettazione:**

- UI già responsive dai primi incrementi, ora verificata sui percorsi import/correzione/report da smartphone e desktop, con tastiera, focus, contrasto ed errori leggibili.
- Pacchetto Boot con React e PostgreSQL avviabili sul PC locale, con possibilità di container applicativo e DB locali, versioni bloccate, DB privato, health minimale, segreti esterni, limiti upload/login e diagnostica priva di dati bancari. Link locale condivisibile nella rete domestica per desktop/smartphone, accesso autenticato e ingresso LAN configurato esplicitamente; nessuna esposizione Internet o pubblicazione di DB/runtime IA. Hosting online futuro fuori dall'incarico locale.
- Se IA abilitata, runtime Qwen su questo stesso PC, processo/container secondo scelta da completare; endpoint non pubblico, risorse e modello documentati. Provare anche l'applicazione con runtime spento, senza invii a servizi cloud.
- Procedura riproducibile di avvio, aggiornamento e gestione migrazioni/rollback applicativo sulla macchina locale; nessun server remoto da predisporre o downgrade distruttivo dello schema dato per scontato.
- Semplice backup manuale del DB e degli originali in un percorso scelto dall'utente, con istruzioni essenziali di esportazione/reimportazione. Nessun backup giornaliero automatico, rotazione a 30 copie, piano di disaster recovery o obiettivo RPO/RTO; gestione delle copie manuale. Coerenza fra DB e originali ed esclusione da Git.

**Prove:** smoke end-to-end su layout mobile e desktop, apertura del link da un secondo dispositivo nella LAN con login, sessione, navigazione e richieste API corrette, senza indirizzi localhost codificati nel frontend; avvio locale del pacchetto completo, eventuale container locale, riavvio e persistenza, DB/runtime IA non esposti. Verifica funzionale minima di esportazione e reimportazione su un piccolo insieme sintetico con un originale collegato, senza simulare perdita dell'ambiente. Eventuali prove Linux in CI attestano portabilità, non un deployment richiesto.
