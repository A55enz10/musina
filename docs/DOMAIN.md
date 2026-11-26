# Dominio e convenzioni dei report

**Confermato** indica requisiti dell'utente. **Proposto** indica convenzioni da revisionare, non autorizzazione a implementarle. Questo documento definisce un modello concettuale, non uno schema SQL definitivo. Le convenzioni proposte sono raccolte in [ADR 0003](decisions/0003-report-conventions.md).

## Entità e invarianti

| Concetto | Significato e vincoli |
| --- | --- |
| Famiglia, membro, accesso | Credenziali individuali, appartenenza alla famiglia, visibilità e gestione contabile condivise fra tutti i membri; solo l'amministratore gestisce gli accessi; autore delle operazioni distinto dal titolare del conto |
| Conto | Conto corrente, prepagata o wallet; valuta esplicita; mese iniziale di monitoraggio della copertura scelto dall'utente; archiviazione senza perdita dello storico |
| Gruppo, appartenenza | Relazione molti-a-molti con i conti, con data/istante di inserimento come metadato; nessun gruppo contiene altri gruppi; personale/comune/famiglia sono solo nomi configurabili; nessuna decorrenza temporale applicata ai report |
| Batch, riga sorgente | Caricamento e osservazione della banca, con posizione e dati originari protetti; non equivalgono al movimento economico |
| Movimento canonico | Evento bancario contabilizzato su un conto; importo con segno, date, descrizione e provenienza; più osservazioni possono riferirsi allo stesso movimento |
| Copertura, osservazione saldo | Evidenza della completezza di un intervallo e saldo bancario con tipo/data/fonte; dimensioni distinte |
| Trasferimento | Collegamento confermato o candidato fra movimenti/quote reali; stato esplicito per ambiguità e lato mancante |
| Categoria, sottocategoria | Tassonomia modificabile, proposta a due livelli; disattivare categorie usate preservando lo storico o riclassificare con anteprima |
| Allocazione (split) | Quota monetaria di un movimento con categoria, natura e provenienza della classificazione; somma esatta delle quote uguale al movimento |
| Esercente, alias, regola | Normalizzazione conservando la descrizione originale; associazioni e condizioni persistenti, versionate quanto basta per audit |
| Suggerimento IA | Proposta separata dalla classificazione, con stato, motivazione breve, modello/versione e accettazione o rifiuto |

Confermato: gli split non creano righe bancarie. Proposto: uno split ordinario ha quote dello stesso segno del movimento; vietare compensazioni artificiali. Quote tecniche per commissioni aggregate sono ammesse solo con evidenza o conferma esplicita, conservando il totale originale.

Le modifiche di categorie, split, regole e trasferimenti registrano autore e istante. Non alterano silenziosamente date/importi bancari. Correzioni del parsing richiedono una procedura di reimportazione con confronto e provenienza, non una modifica tramite IA.

## Perimetro, segni e date — proposto

- Sia `S` l'insieme degli ID di conto selezionati, ottenuto dall'unione distinta dei gruppi/conti. Ogni ID di movimento e ogni quota sono contati una sola volta; mai sommare i subtotali di gruppi sovrapposti.
- **Confermato durante la revisione:** appartenenza al gruppo senza decorrenza temporale; la composizione attuale si applica all'intero storico, mostrando i conti effettivamente inclusi. Nella relazione n:n gruppo–conto prevedere la data di inserimento come metadato (nome tecnico proposto `inserted_at`, istante UTC), senza usarla per filtrare movimenti o periodi e senza un controllo UI per scegliere la decorrenza. Il metadato registra l'inserimento della relazione, non un cambio di proprietario del conto. Un'eventuale futura validità temporale richiederebbe una decisione e un modello dedicati; non è implementata implicitamente dal timestamp.
- Entrata sul conto positiva, uscita negativa. Java `BigDecimal` e SQL `NUMERIC`, scala EUR 2 e nessun arrotondamento silenzioso dei dati importati; valuta sempre esplicita. Nessuna somma fra valute diverse.
- **Confermato durante la revisione:** conti, movimenti canonici e report solo in EUR nella fase 1, senza cambio valuta o saldi in altre divise. Gli acquisti in valuta estera usano l'importo effettivamente contabilizzato in euro dalla banca; Musina non ricalcola il cambio. Valori non EUR non sono convertiti né trattati implicitamente come EUR: segnalarli come non supportati.
- **Confermato durante la revisione:** scegliere in visualizzazione la data di riferimento fra quelle effettivamente disponibili nella fonte; mantenere separate data contabile, valuta ed eventuale operazione, come `LocalDate`. La scelta governa filtro del periodo, aggregazioni, ordinamento per data e drill-down, senza modificare le date importate. L'utente riferisce estrazioni mensili delimitate per data contabile; questo criterio di estrazione rimane distinto dal criterio di visualizzazione.
- **Confermato durante la revisione:** data contabile come selezione iniziale, dove disponibile. Le intestazioni osservate sono `DATA CONT.` e `DATA VAL.` in un XLSX, `DATA OP.` nell'XLSX denominato Quickard; non equivalgono a una verifica completa della semantica. Non inventare una data mancante né sostituirla con un'altra senza segnalarlo. Una vista su conti con date non disponibili deve evidenziare conti/righe non rappresentabili, senza esclusione silenziosa o totali dichiarati completi. Il comportamento preciso del selettore e della modalità iniziale sulle fonti prive di data contabile resta da definire dopo la validazione della fonte.
- L'intervallo visualizzato inclusivo dal/al diventa internamente `[inizio, fine esclusa)`. Mesi e anni di calendario; timezone `Europe/Rome` per eventi applicativi, senza traslare le date bancarie. Audit in UTC.
- Solo movimenti presenti, contabilizzati e confermati; anteprime escluse. Eliminare un'importazione elimina anche i movimenti privi di altra provenienza, mentre quelli sostenuti da altri batch restano; nessun batch annullato conservato tramite soft delete. Eventuali operazioni pendenti non entrano nei saldi contabili o report consolidati.

**Confermato durante la revisione:** ogni conto ha un mese iniziale di monitoraggio della copertura, configurabile dall'utente. Le segnalazioni dei periodi mancanti partono da quel mese, senza richiedere estratti anteriori. Non è data di apertura del conto o di appartenenza al gruppo e non filtra i movimenti nei report: eventuali movimenti precedenti già importati restano consultabili. Impostare o cambiare il mese non rende completo un periodo privo di evidenza bancaria.

## Natura economica e formule — proposto

La natura è distinta dalla categoria e non introduce un'attribuzione personale/comune. Ogni quota `q` ha importo firmato `a(q)` e una natura: reddito, consumo, altra uscita, altro afflusso, trasferimento o natura da chiarire. Il rimborso collegato eredita natura e categoria della quota originaria con segno opposto. Le commissioni hanno natura consumo (spesa per servizi finanziari).

Sul periodo `P`, per le quote reali dei movimenti nei conti `S`:

| Misura | Formula/convenzione |
| --- | --- |
| Entrate bancarie lorde `E` | `Σ max(a, 0)` su tutte le quote |
| Uscite bancarie lorde `U` | `Σ max(-a, 0)` su tutte le quote; sempre mostrate positive |
| Variazione di cassa `Δ` | `E - U = Σ a`; comprende anche trasferimenti |
| Redditi netti `R` | `Σ a` delle quote reddito; restituzioni di reddito negative |
| Consumi netti `C` | `-Σ a` delle quote consumo; rimborsi riducono i consumi |
| Altre uscite nette `O` | `-Σ a` delle quote altra uscita; restituzioni riducono questa misura |
| Altre entrate nette `A` | `Σ a` delle quote altro afflusso; ad esempio disinvestimenti e prestiti ricevuti, senza calcolo di plusvalenze, rendimenti o debito residuo |
| Trasferimenti interni `Tᵢ` | `Σ a` delle quote trasferimento con entrambi i conti in `S` |
| Trasferimenti di perimetro `Tₚ` | `Σ a` delle quote trasferimento verso/da conto noto fuori `S`; mostrare anche entrate e uscite separate |
| Trasferimenti da verificare `T?` | `Σ a` dei trasferimenti confermati come tali, ma con controconto/perimetro ancora ignoto |
| Natura da chiarire `N` | `Σ a` delle quote senza natura; mostrare anche positivi/negativi lordi e quantità |

Tutte le somme sul periodo usano la data selezionata, indicata nella vista. `Δ` è una variazione di cassa contabile confrontabile con i saldi solo nella vista per data contabile e con confini coerenti con le osservazioni di saldo; nelle altre viste è il flusso netto attribuito al periodo secondo quella data e va etichettato di conseguenza. La riconciliazione bancaria conserva sempre il proprio riferimento contabile.

Identità di controllo: **`Δ = R - C - O + A + Tᵢ + Tₚ + T? + N`**. Le partizioni sono esclusive. Per il riepilogo: entrate `I = R + A`, uscite `Uᵣ = C + O`, differenza `D = I - Uᵣ = R + A - C - O`. Quindi `Δ = D + Tᵢ + Tₚ + T? + N`. La differenza non rappresenta un incremento del patrimonio: include ad esempio disinvestimenti e prestiti ricevuti. Eventuali margini basati sui soli redditi (es. `R - C - O`) devono dichiararlo esplicitamente e non sostituire la differenza del riepilogo. Evitare il generico «risparmio» senza definizione.

**Riepilogo mensile confermato durante la revisione:** mostrare entrate, uscite complessive comprensive di mutuo e PAC, differenza fra entrate e uscite; dettaglio separato di spese ordinarie, mutuo e investimenti. Escludere i giroconti interni da entrate e uscite. Esempio approvato: entrate `3000`, spese ordinarie `1000`, rata mutuo `800`, PAC `200` → uscite `2000`, differenza `1000`. Non etichettare la differenza come saldo bancario o incremento del patrimonio. I rimborsi riducono le rispettive spese secondo la convenzione confermata.

**Confermato durante la revisione:** anche gli accrediti classificati come altre entrate (es. disinvestimenti, prestiti ricevuti) contribuiscono alle entrate complessive, con dettaglio distinto dai redditi. Esempio: stipendio `3000` e disinvestimento `500` → entrate `3500`, di cui redditi `3000` e altre entrate `500`. Nessun rendimento, plusvalenza o debito residuo calcolato. Rimborsi degli acquisti a riduzione delle spese e trasferimenti separati, come già deciso.

**Mappatura delle convenzioni approvate:** confronto `R + A` contro `C + O`, differenza `R + A - C - O`, dettaglio redditi/altre entrate e consumi/altre uscite, con PAC e rata mutuo inclusi per intero in `O`. Non sommare di nuovo il dettaglio di `A` al totale delle entrate, che già lo include. Trasferimenti e non risolti rimangono sezioni separate visibili. La vista di cassa mostra `E`, `U`, `Δ` e include tutte le quote, compresi non classificati e trasferimenti. Titoli e tooltip dichiarano la metrica e i totali eventualmente incompleti; gli importi di natura ignota restano nella cassa e in «Da classificare», senza essere assegnati automaticamente a redditi o consumi. L'andamento mensile usa le stesse formule; i grafici per categoria mostrano valori netti e ammettono categorie negative per rimborsi superiori alle spese del periodo. Totali del grafico e drill-down devono coincidere esattamente.

**Confermato durante la revisione:** movimenti non classificati importabili se i dati bancari sono validi, sempre inclusi nei totali di cassa e visibili in «Da classificare» con conteggio e importi, correggibili successivamente. Finché il significato economico non è noto, non considerarli automaticamente consumi o redditi. La mancanza di categoria o natura non è un errore bloccante dell'importazione; errori di parsing, integrità o identità rimangono distinti.

**Modello proposto:** non classificato può significare categoria assente, natura assente o entrambe: mostrare i due stati separatamente. Una spesa con natura consumo ma categoria ignota entra in `C` sotto «Categoria da assegnare». Una quota di natura ignota entra in `N`, senza essere chiamata consumo. Nessuna esclusione silenziosa: la sezione «Da classificare» non è una quota aggiuntiva da sommare di nuovo ai totali di cassa. Su dati incompleti i totali restano consultabili con avviso e perimetro della copertura.

## Rimborsi acquisti confermati; dettagli e rettifiche proposti

**Confermato:** il rimborso di un acquisto riduce la stessa categoria di spesa nel periodo della propria data selezionata nella vista (contabile nella vista contabile), senza riscrivere il periodo dell'acquisto e senza essere conteggiato come reddito. Nelle formule proposte è consumo con importo positivo e riduce `C`. Esempio: abbigliamento `100` a gennaio e rimborso `30` a febbraio → consumi di gennaio `100`, febbraio `-30`, totale dei due mesi `70` se non ci sono altre operazioni.

**Proposte applicative:** se non è noto l'acquisto, proporre il collegamento o la natura, senza classificare ogni accredito come reddito. Segnalare un'anomalia se il totale dei rimborsi collegati supera l'importo originario allocato. Una rettifica bancaria collegata neutralizza l'evento secondo la propria data; non è un comando per cancellare la riga originale. Restituzioni di versamenti PAC o rimborsi della rata mutuo riducono la rispettiva uscita (in `O` nelle formule proposte), senza scomporre il mutuo o inventare una posizione patrimoniale.

## Trasferimenti — requisiti confermati e convenzione proposta

Confermato: escludere i trasferimenti fra conti del report da redditi e spese; mostrare separatamente quelli da/verso conti fuori report; conteggiare le commissioni come spese. Non assumere che qualsiasi bonifico sia interno. Un pagamento a un fornitore rimane una spesa, anche se eseguito tramite bonifico.

**Confermato durante la revisione:** collegare automaticamente i giroconti soltanto con riferimenti affidabili ai conti e una corrispondenza univoca. Data e importo coincidenti, da soli, non bastano. I casi ambigui o non sufficientemente documentati restano «Da verificare», con revisione manuale; nessuna contropartita fittizia. Rendere riconoscibile la provenienza automatica del collegamento, consultabile e correggibile.

**Proposta tecnica:** matching su segni opposti, importo/valuta, conti noti, riferimenti attendibili verificati sui layout e finestra temporale specifica per fonte. Candidati multipli o informazioni insufficienti non raggiungono i requisiti del collegamento automatico. Le quote principali di una coppia confermata si compensano sull'intero intervallo che le contiene, non necessariamente nel singolo mese. Criteri operativi e finestre devono essere definiti e provati sui campioni, senza soglie inventate o inferenza che ogni bonifico sia interno.

Esempio nella vista per data contabile: conto A `-100` il 31 gennaio e conto B `+100` il 1 febbraio. Con A e B in `S`, gennaio ha `Tᵢ=-100`, febbraio `Tᵢ=+100`, entrambi con redditi e spese zero. Su gennaio-febbraio `Tᵢ=0`. Queste differenze di cassa sono visibili come trasferimenti in transito, senza cambiare le date o aggiungere movimenti. Con solo A in `S`, gennaio ha `Tₚ=-100`.

Con un solo lato importato, se il controconto è noto mantenere il trasferimento confermato incompleto, classificabile in `Tᵢ` o `Tₚ` secondo `S`; altrimenti usare `T?`. Un semplice candidato non confermato non viene escluso automaticamente dai consumi: conserva la classificazione esistente, oppure resta in `N`, con avviso. Il report distingue «controconto fuori perimetro» da «lato mancante».

Esempio commissione: addebito A `-102`, accredito B `+100`, costo documentato `2`: A si divide in trasferimento `-100` e commissione consumo `-2`. Su entrambi i conti e date incluse `Δ=-2`, `C=2`, trasferimento netto zero. Se il costo non è documentato, la differenza non è automaticamente una commissione: matching da chiarire.

## PAC e rata mutuo — trattamento confermato

| Caso | Trattamento nella fase 1 |
| --- | --- |
| Versamento PAC — confermato | Uscita di cassa per investimenti «Investimenti / Versamenti PAC», separata dai consumi (in `O` nelle formule proposte); nessuna posizione, quota fondo, rendimento o valorizzazione |
| PAC tramite conto di liquidità già gestito | Il giroconto reale verso quel conto è trasferimento; l'eventuale successivo acquisto osservato è altra uscita. Mai conteggiare anche un versamento sintetico |
| Rata mutuo — confermato | Intera rata come unica uscita nella categoria «Mutuo / Rata» (in `O` nelle formule proposte), senza distinzione capitale/interessi. Classificazione definitiva, senza stato «da scomporre» o avviso per assenza del dettaglio |

Esempio della distinzione PAC confermata: acquisti per `1000` e versamento PAC per `200` danno uscite di cassa `1200`, consumi `1000` e versamenti per investimenti `200`.

Un PAC `-200` e una rata `-800` danno uscite di cassa `1000`, suddivise in versamenti per investimenti `200` e rata mutuo `800`, senza scomposizione della rata. Nelle formule proposte entrambe le quote rientrano in `O`: `O=1000`, `C=0`, `Δ=-1000`. Non si calcolano capitale, interessi o commissioni incorporati nella rata; eventuali commissioni bancarie addebitate come movimenti separati restano spese secondo la regola generale. Non si gestiscono debito residuo, piano di ammortamento, patrimonio netto o rendimenti. Gli split generali rimangono disponibili per altri casi d'uso, senza un flusso dedicato o un obbligo di split del mutuo.

## Classificazioni, esercenti e regole

Confermato: modifica del singolo movimento distinta da creazione di una regola. «Dolci Fate → Pasticceria» confermato come regola persistente si riutilizza senza nuove chiamate IA; una correzione singola non genera automaticamente la regola. Amazon rimane ambiguo se manca informazione sull'acquisto.

**Confermato durante la revisione:** quando l'IA è abilitata, generare automaticamente suggerimenti dopo ogni importazione confermata, sui movimenti rimasti irrisolti dopo le regole e le associazioni deterministiche. L'automatismo riguarda la richiesta di suggerimenti, non la loro accettazione. Riusare regole e suggerimenti ancora validi senza chiamate duplicate; un reupload idempotente senza nuovi casi irrisolti non richiede nuove chiamate. In assenza di IA, o in caso di errore/limite del provider, il batch resta importato e i casi rimangono consultabili in «Da classificare».

**Confermato:** l'utente controlla e accetta o corregge i suggerimenti, anche in blocco selezionando più movimenti. La conferma applica le categorie ai movimenti selezionati, senza creare automaticamente una regola o un'associazione persistente da riusare su altri movimenti. «Ricorda questa associazione» resta un'azione distinta ed esplicita. Le classificazioni manuali già confermate rimangono protette.

Proposto: precedenza manuale confermata (anche split) → regole esplicite ordinate → associazione esercente non ambigua → suggerimento IA → irrisolto. Conflitti tra regole pari priorità vanno segnalati. Aliasi e normalizzazione non devono fondere esercenti solo per somiglianza. Le regole retroattive richiedono anteprima con conteggio/diff ed esclusione delle classificazioni manuali confermate; conferma su versione dello storico ancora valida. Per la conferma multipla dei suggerimenti, verificare versione e stato corrente dei movimenti selezionati, senza applicare risultati obsoleti a movimenti nel frattempo modificati o eliminati.

## Questioni aperte

Revisione richiesta per gestione delle date non disponibili, formule/nature e dettagli di collegamento dei rimborsi/rettifiche. Sono confermati: rimborsi degli acquisti a riduzione della categoria originaria nel periodo del rimborso senza riscrivere l'acquisto né creare reddito, rata mutuo come unica uscita per l'intero importo senza scomposizione, versamenti PAC come uscite per investimenti separate dai consumi, appartenenza ai gruppi senza decorrenza nei report e con data di inserimento come metadato, EUR esclusivo nella fase 1, scelta della data in visualizzazione e data contabile come selezione iniziale dove disponibile. Le date esatte da offrire vanno distinte per fonte e verificate semanticamente. Campioni locali ora disponibili: prima ispezione delle intestazioni eseguita, scale e identificativi bancari ancora da validare. Per la riconciliazione e la copertura vedere [IMPORTS.md](IMPORTS.md).
