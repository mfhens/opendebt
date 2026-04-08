# Begrebsmodel for inddrivelse af offentlige fordringer

**Version:** 1.0  
**Kilde:** Gældsstyrelsen / UFST (Udviklings- og Forenklingsstyrelsen)  
**Kildesprog:** Dansk  
**Godkendt af:** *Ikke godkendt endnu — afventer domæneekspertgodkendelse*  
**Godkendelsesdato:** —

> Dette dokument er det primære review-artefakt for domæneeksperter og jurister.
> Det er genereret fra `domain/concept-model.yaml` og må ikke redigeres direkte.
> Send ændringer som forslag til YAML-filen.

---

## Indholdsfortegnelse

1. [Aktører og roller](#aktører-og-roller)
2. [Krav og status](#krav-og-status)
3. [Kravstruktur](#kravstruktur)
4. [Livscyklus](#livscyklus)
5. [Finansiel](#finansiel)
6. [Kommunikation](#kommunikation)
7. [Reaktion og tvist](#reaktion-og-tvist)
8. [Myndighedsudøvelse](#myndighedsudøvelse)
9. [Oversigtstabel](#oversigtstabel)

---

## Aktører og roller

### Part *(abstrakt)*

| Felt | Værdi |
|---|---|
| **ID** | `part` |
| **Dansk term** | Part |
| **Engelsk ækvivalent** | Party |
| **Abstrakt** | Ja |
| **Implementeringsstatus** | `partial` — person-registry |

**Definition:**
Abstrakt aktørbegreb, der dækker en identificerbar enhed, som kan indgå i relationer om fordringer, kommunikation og inddrivelse.

**Note:** Part anvendes som fælles overbegreb og forventes normalt ikke at forekomme alene i forretningen.

**Specialiseringer:** Fordringshaver, Skyldner, Restanceinddrivelsesmyndighed

---

### Fordringshaver

| Felt | Værdi |
|---|---|
| **ID** | `fordringshaver` |
| **Dansk term** | Fordringshaver |
| **Engelsk ækvivalent** | Creditor |
| **Specialisering af** | Part |
| **Implementeringsstatus** | `partial` — debt-service |

**Definition:**
Part, der har eller administrerer en fordring mod en skyldner og som kan fremsende skyldnerunderretning, foretage overdragelse til inddrivelse, regulere overdragne fordringer, tilbagekalde fordringer og modtage underretningsmeddelelser fra restanceinddrivelsesmyndigheden.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| har | Fordring |
| giver | Skyldnerunderretning |
| foretager | Overdragelse til inddrivelse |
| foretager | Regulering |
| foretager | Tilbagekald |
| foretager | Genindsendelse |

---

### Skyldner

| Felt | Værdi |
|---|---|
| **ID** | `skyldner` |
| **Dansk term** | Skyldner |
| **Engelsk ækvivalent** | Debtor |
| **Specialisering af** | Part |
| **Implementeringsstatus** | `partial` — debt-service |

**Definition:**
Part, der hæfter helt eller delvist for en fordring og som kan modtage skyldnerunderretning og fremsætte indsigelse.

**Note:** Skyldner identificeres via CPR, CVR/SE eller AKR-nummer. For I/S skal alle personligt hæftende interessenter indberettes. For PEF linkes ejerens CPR automatisk.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| fremsætter | Indsigelse |

---

### Restanceinddrivelsesmyndighed

| Felt | Værdi |
|---|---|
| **ID** | `restanceinddrivelsesmyndighed` |
| **Dansk term** | Restanceinddrivelsesmyndighed |
| **Engelsk ækvivalent** | Public Debt Collection Authority |
| **Specialisering af** | Part |
| **Implementeringsstatus** | `missing` — case-service |

**Definition:**
Part, der efter overdragelse forestår inddrivelse af offentlige fordringer efter gældende regler og kan sende underretningsmeddelelser til fordringshaver om handlinger på restancer.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| iværksætter | Inddrivelsesskridt |
| opnår | Dækning |
| sender | Underretningsmeddelelse |

---

## Krav og status

### Kravgrundlag

| Felt | Værdi |
|---|---|
| **ID** | `kravgrundlag` |
| **Dansk term** | Kravgrundlag |
| **Engelsk ækvivalent** | Basis of Claim |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Det retlige eller faktiske grundlag, der begrunder en fordring.

**Note:** Kravgrundlaget kan eksempelvis være afgørelse, hjemmel, beregning eller anden retsskabende begivenhed. Det retlige grundlag udtrykkes operationelt gennem Fordringstype.

---

### Fordring

| Felt | Værdi |
|---|---|
| **ID** | `fordring` |
| **Dansk term** | Fordring |
| **Engelsk ækvivalent** | Claim |
| **Implementeringsstatus** | `partial` — debt-service (`DebtEntity`, `/api/debts`) |

**Definition:**
Et krav på betaling, som en fordringshaver har mod en eller flere skyldnere.

**Note:** Relationen til skyldner går via Hæftelse. En fordring kan være en hovedfordring i et fordringskompleks og kan have underfordringer.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| er begrundet i | Kravgrundlag |
| har | Betalingsfrist |
| har | Forældelse |
| er klassificeret som | Fordringsart |
| har | Fordringstype |
| er underfordring af | Fordring *(selvreference)* |

---

### Restance

| Felt | Værdi |
|---|---|
| **ID** | `restance` |
| **Dansk term** | Restance |
| **Engelsk ækvivalent** | Overdue Claim |
| **Specialisering af** | Fordring |
| **Implementeringsstatus** | `partial` — debt-service |

**Definition:**
Fordring, hvis betalingsfrist er overskredet uden rettidig betaling.

**Note:** Restance er den specialisering af fordring, som kan danne genstand for overdragelse til inddrivelse, regulering, tilbagekald og efterfølgende inddrivelsesskridt.

---

### Betalingsfrist

| Felt | Værdi |
|---|---|
| **ID** | `betalingsfrist` |
| **Dansk term** | Betalingsfrist |
| **Engelsk ækvivalent** | Payment Deadline |
| **Implementeringsstatus** | `partial` — debt-service |

**Definition:**
Det tidspunkt eller den periode, inden for hvilken fordringen skal være betalt.

**Note:** Sidste rettidige betalingsdato (SRB) er den operative udmøntning. Henstand eller betalingsordning ændrer SRB. Rykkerskrivelser udskyder ikke SRB.

---

### Forældelse

| Felt | Værdi |
|---|---|
| **ID** | `foraeldelse` |
| **Dansk term** | Forældelse |
| **Engelsk ækvivalent** | Limitation |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Det tidspunkt, hvor fordringen mister retskraft og ikke længere kan gøres gældende.

**Note:** Forældelsesdato er obligatorisk stamdata ved overdragelse og skal afspejle den aktuelle dato. Forældelse kan afbrydes ved skyldserkendelse, betalingspåkrav, fogedretudlæg eller meddelelse om modregning.

---

### Fordringsart

| Felt | Værdi |
|---|---|
| **ID** | `fordringsart` |
| **Dansk term** | Fordringsart |
| **Engelsk ækvivalent** | Claim Art |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Overordnet klassifikation af en fordring, der bestemmer det grundlæggende behandlingsregime. Kendte værdier er INDR (inddrivelse) og MODR (kun modregning).

---

### Fordringstype

| Felt | Værdi |
|---|---|
| **ID** | `fordringstype` |
| **Dansk term** | Fordringstype |
| **Engelsk ækvivalent** | Claim Type |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Specifik klassifikation af en fordring baseret på det retlige grundlag, der bestemmer valideringsregler, renteberegningsregler, indgangsfilterregler og tilladt adfærd.

**Note:** Eksempler: PSRESTS (restskat), POBODPO (politibøder), CFFAKTU (civilretlige fakturakrav). Fordringstypen er forankret i fordringshaveraftalen.

---

## Kravstruktur

### Fordringskompleks

| Felt | Værdi |
|---|---|
| **ID** | `fordringskompleks` |
| **Dansk term** | Fordringskompleks |
| **Engelsk ækvivalent** | Claim Complex |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Sammensætning af en hovedfordring og tilknyttede fordringer (underfordringer), der i størst muligt omfang behandles som én samlet fordring.

**Note:** Et fordringskompleks opstår ved opskrivning eller ved selvstændige relaterede rentefordringer. Tilbagekald af hovedfordring medfører automatisk tilbagekald af hele komplekset.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| består af | Fordring |

---

## Livscyklus

### Overdragelse til inddrivelse

| Felt | Værdi |
|---|---|
| **ID** | `overdragelse-til-inddrivelse` |
| **Dansk term** | Overdragelse til inddrivelse |
| **Engelsk ækvivalent** | Transfer for Debt Collection |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Den handling, hvorved fordringshaver sender en restance til restanceinddrivelsesmyndigheden med henblik på videre inddrivelse.

**Note:** Forudsætter at sædvanlig rykkerprocedure er gennemført forgæves, at skyldner er underrettet skriftligt, og at betalingsfrist er overskredet. Ved overdragelse returneres kvittering med status UDFØRT, AFVIST eller HØRING.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| omfatter | Restance |
| sker til | Restanceinddrivelsesmyndighed |
| kan resultere i | Høring |

---

### Høring

| Felt | Værdi |
|---|---|
| **ID** | `hoering` |
| **Dansk term** | Høring |
| **Engelsk ækvivalent** | Hearing |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Livscyklustilstand, hvor en indsendt fordring afventer fordringshaveres stillingtagen, fordi stamdata afviger fra indgangsfilterets regler.

**Note:** En fordring i høring er ikke modtaget til inddrivelse. Fordringshaveres egne forældelsesregler gælder. Høring afsluttes ved godkendelse (med begrundelse) eller tilbagetrækning.

---

### Regulering *(abstrakt)*

| Felt | Værdi |
|---|---|
| **ID** | `regulering` |
| **Dansk term** | Regulering |
| **Engelsk ækvivalent** | Claim Adjustment |
| **Abstrakt** | Ja |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Abstrakt begreb for en fordringshaver-handling, der justerer saldoen på en allerede overdraget restance.

**Specialiseringer:** Opskrivning, Nedskrivning

**Relationer:**

| Relation | Målbegreb |
|---|---|
| vedrører | Restance |

---

### Opskrivning

| Felt | Værdi |
|---|---|
| **ID** | `opskrivning` |
| **Dansk term** | Opskrivning |
| **Engelsk ækvivalent** | Write-up |
| **Specialisering af** | Regulering |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Regulering, der forhøjer saldoen på en overdraget restance.

**Note:** Renter kan ikke opskrives; de skal indsendes som ny relateret fordring. Opskrivningen danner et fordringskompleks. Kræver skriftlig underretning af skyldner.

---

### Nedskrivning

| Felt | Værdi |
|---|---|
| **ID** | `nedskrivning` |
| **Dansk term** | Nedskrivning |
| **Engelsk ækvivalent** | Write-down |
| **Specialisering af** | Regulering |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Regulering, der nedsætter saldoen på en overdraget restance.

**Note:** Nedskrivning kan ikke overstige restsaldoen og afvises i givet fald. Kendte årsagskoder: INDB (indbetaling), REGU (korrektion), OANI og OONR (annullering).

---

### Tilbagekald

| Felt | Værdi |
|---|---|
| **ID** | `tilbagekald` |
| **Dansk term** | Tilbagekald |
| **Engelsk ækvivalent** | Withdrawal |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Den handling, hvorved fordringshaver trækker en overdraget restance tilbage fra inddrivelse.

**Note:** Virkningerne afhænger af årsagskode: BORD, BORT, FEJL, HENS, KLAG. FEJL ophæver alle dækninger; KLAG og HENS fastholder dem. Tilbagekald af hovedfordring medfører automatisk tilbagekald af relaterede fordringer.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| vedrører | Restance |

---

### Genindsendelse

| Felt | Værdi |
|---|---|
| **ID** | `genindsendelse` |
| **Dansk term** | Genindsendelse |
| **Engelsk ækvivalent** | Resubmission |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Fornyet indsendelse af en tidligere tilbagekaldt restance til inddrivelse.

**Note:** Kun tilgængelig efter KLAG- eller HENS-tilbagekald. FEJL-tilbagekaldte fordringer kan ikke genindsendes og skal oprettes som nye fordringer.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| vedrører | Restance |
| følger af | Tilbagekald |

---

## Finansiel

### Dækning

| Felt | Værdi |
|---|---|
| **ID** | `daekning` |
| **Dansk term** | Dækning |
| **Engelsk ækvivalent** | Recovery |
| **Implementeringsstatus** | `missing` — payment-service |

**Definition:**
Betaling opnået under inddrivelse af en restance.

**Note:** Dækningsrækkefølge: inddrivelsesrente dækkes forud for hovedstol. Dækninger afregnes periodisk til fordringshaver via afregningsunderretning.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| vedrører | Restance |

---

### Inddrivelsesrente

| Felt | Værdi |
|---|---|
| **ID** | `inddrivelsesrente` |
| **Dansk term** | Inddrivelsesrente |
| **Engelsk ækvivalent** | Recovery Interest |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Rente tilskrevet en restance under inddrivelse.

**Note:** Beregnes som simpel dag-til-dag rente (p.t. 5,75 % p.a. pr. 1. januar 2026) fra 1. i måneden efter modtagelse. Kan nulstilles ved FEJL-tilbagekald. Bøder er undtaget.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| tilskrives | Restance |

---

## Kommunikation

### Underretning *(abstrakt)*

| Felt | Værdi |
|---|---|
| **ID** | `underretning` |
| **Dansk term** | Underretning |
| **Engelsk ækvivalent** | Notification |
| **Abstrakt** | Ja |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Abstrakt begreb for formel meddelelse til en relevant part om en fordring, dens status eller dens behandling.

**Specialiseringer:** Skyldnerunderretning, Underretningsmeddelelse

---

### Skyldnerunderretning *(abstrakt)*

| Felt | Værdi |
|---|---|
| **ID** | `skyldnerunderretning` |
| **Dansk term** | Skyldnerunderretning |
| **Engelsk ækvivalent** | Debtor Notice |
| **Abstrakt** | Ja |
| **Specialisering af** | Underretning |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Abstrakt underbegreb for formel meddelelse til skyldner om betalingsforpligtelse eller konsekvenser af manglende betaling.

**Specialiseringer:** Påkrav, Rykker

**Relationer:**

| Relation | Målbegreb |
|---|---|
| tilgår | Skyldner |
| vedrører | Fordring |

---

### Påkrav

| Felt | Værdi |
|---|---|
| **ID** | `paakrav` |
| **Dansk term** | Påkrav |
| **Engelsk ækvivalent** | Demand for Payment |
| **Specialisering af** | Skyldnerunderretning |
| **Implementeringsstatus** | `partial` — letter-service |

**Definition:**
Skyldnerunderretning, der formelt anmoder skyldner om at opfylde betalingsforpligtelsen inden en angivet frist.

---

### Rykker

| Felt | Værdi |
|---|---|
| **ID** | `rykker` |
| **Dansk term** | Rykker |
| **Engelsk ækvivalent** | Reminder Notice |
| **Specialisering af** | Skyldnerunderretning |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Skyldnerunderretning, der meddeler, at betaling ikke er sket rettidigt, og at yderligere handling kan følge.

---

### Underretningsmeddelelse *(abstrakt)*

| Felt | Værdi |
|---|---|
| **ID** | `underretningsmeddelelse` |
| **Dansk term** | Underretningsmeddelelse |
| **Engelsk ækvivalent** | Creditor Notification |
| **Abstrakt** | Ja |
| **Specialisering af** | Underretning |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Abstrakt underbegreb for formel meddelelse fra restanceinddrivelsesmyndigheden til fordringshaver om nye handlinger på en overdraget restance.

**Note:** Fungerer som kvittering og orientering. Kan ikke hentes ældre end 3 måneder. Distributionskanal: system-til-system, portal eller e-Boks.

**Specialiseringer:** Afregningsunderretning, Udligningsunderretning, Allokeringsunderretning, Renteunderretning, Detaljeret renteunderretning, Afskrivningsunderretning, Tilbagesendelsesunderretning, Returneringsunderretning

**Relationer:**

| Relation | Målbegreb |
|---|---|
| tilgår | Fordringshaver |
| vedrører | Restance |

---

### Afregningsunderretning

| Felt | Værdi |
|---|---|
| **ID** | `afregningsunderretning` |
| **Dansk term** | Afregningsunderretning |
| **Engelsk ækvivalent** | Settlement Notification |
| **Specialisering af** | Underretningsmeddelelse |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Underretningsmeddelelse, der orienterer fordringshaver om månedlig afregning af modtagne indbetalinger på en eller flere restancer.

---

### Udligningsunderretning

| Felt | Værdi |
|---|---|
| **ID** | `udligningsunderretning` |
| **Dansk term** | Udligningsunderretning |
| **Engelsk ækvivalent** | Reconciliation Notification |
| **Specialisering af** | Underretningsmeddelelse |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Underretningsmeddelelse, der orienterer fordringshaver om bevægelser på en restance, som påvirker saldoen.

**Note:** Gensidigt eksklusiv med Allokeringsunderretning.

---

### Allokeringsunderretning

| Felt | Værdi |
|---|---|
| **ID** | `allokeringsunderretning` |
| **Dansk term** | Allokeringsunderretning |
| **Engelsk ækvivalent** | Allocation Notification |
| **Specialisering af** | Underretningsmeddelelse |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Underretningsmeddelelse, der orienterer fordringshaver om, hvordan saldoændringer fordeles mellem hovedstol, renter og andre finansielle komponenter.

**Note:** Gensidigt eksklusiv med Udligningsunderretning.

---

### Renteunderretning

| Felt | Værdi |
|---|---|
| **ID** | `renteunderretning` |
| **Dansk term** | Renteunderretning |
| **Engelsk ækvivalent** | Interest Notification |
| **Specialisering af** | Underretningsmeddelelse |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Underretningsmeddelelse, der orienterer fordringshaver om rentetilskrivning på en restance.

**Note:** Gensidigt eksklusiv med Detaljeret renteunderretning.

---

### Detaljeret renteunderretning

| Felt | Værdi |
|---|---|
| **ID** | `detaljeret-renteunderretning` |
| **Dansk term** | Detaljeret renteunderretning |
| **Engelsk ækvivalent** | Detailed Interest Notification |
| **Specialisering af** | Underretningsmeddelelse |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Underretningsmeddelelse, der orienterer fordringshaver detaljeret om nye renteposteringer, typisk pr. døgn.

**Note:** Kun tilgængelig for system-til-system-løsning. Gensidigt eksklusiv med Renteunderretning.

---

### Afskrivningsunderretning

| Felt | Værdi |
|---|---|
| **ID** | `afskrivningsunderretning` |
| **Dansk term** | Afskrivningsunderretning |
| **Engelsk ækvivalent** | Write-off Notification |
| **Specialisering af** | Underretningsmeddelelse |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Underretningsmeddelelse, der orienterer fordringshaver om, at en restance er afskrevet.

---

### Tilbagesendelsesunderretning

| Felt | Værdi |
|---|---|
| **ID** | `tilbagesendelsesunderretning` |
| **Dansk term** | Tilbagesendelsesunderretning |
| **Engelsk ækvivalent** | Return-to-Creditor Notification |
| **Specialisering af** | Underretningsmeddelelse |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Underretningsmeddelelse, der orienterer fordringshaver om, at en restance tilbagesendes til fordringshaver.

---

### Returneringsunderretning

| Felt | Værdi |
|---|---|
| **ID** | `returneringsunderretning` |
| **Dansk term** | Returneringsunderretning |
| **Engelsk ækvivalent** | Returned Claim Notification |
| **Specialisering af** | Underretningsmeddelelse |
| **Implementeringsstatus** | `missing` — letter-service |

**Definition:**
Underretningsmeddelelse, der orienterer fordringshaver om, at en restance returneres, eller at der er opstået saldorelateret opfølgning på en tidligere tilbagekaldt fordring.

---

## Reaktion og tvist

### Indsigelse

| Felt | Værdi |
|---|---|
| **ID** | `indsigelse` |
| **Dansk term** | Indsigelse |
| **Engelsk ækvivalent** | Objection |
| **Implementeringsstatus** | `missing` — case-service |

**Definition:**
Skyldners bestridelse af fordringens eksistens, størrelse, grundlag eller behandling.

**Note:** Indsigelse er ikke en underretning, men en reaktion. Ved klage med opsættende virkning (KLAG) skal fordringshaver tilbagekalde fordringen.

**Relationer:**

| Relation | Målbegreb |
|---|---|
| vedrører | Fordring |

---

### Hæftelse

| Felt | Værdi |
|---|---|
| **ID** | `haeftelse` |
| **Dansk term** | Hæftelse |
| **Engelsk ækvivalent** | Liability |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Det retlige og forretningsmæssige forhold, der knytter en skyldner til en fordring og beskriver arten og omfanget af betalingsansvaret.

**Note:** PSRM understøtter i praksis kun solidarisk hæftelse. Alle medhæftere skal indberettes samtidig.

**Specialiseringer:** Enehæftelse, Solidarisk hæftelse, Delt hæftelse

**Relationer:**

| Relation | Målbegreb |
|---|---|
| vedrører | Fordring |
| vedrører | Skyldner |

---

### Enehæftelse

| Felt | Værdi |
|---|---|
| **ID** | `eneheftelse` |
| **Dansk term** | Enehæftelse |
| **Engelsk ækvivalent** | Sole Liability |
| **Specialisering af** | Hæftelse |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Hæftelse, hvor en fordring kun er knyttet til én skyldner som ansvarlig part.

---

### Solidarisk hæftelse

| Felt | Værdi |
|---|---|
| **ID** | `solidarisk-haeftelse` |
| **Dansk term** | Solidarisk hæftelse |
| **Engelsk ækvivalent** | Joint and Several Liability |
| **Specialisering af** | Hæftelse |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Hæftelse, hvor flere skyldnere hæfter for samme fordring på en måde, så kravet kan gøres gældende efter de regler, der gælder for solidarisk ansvar.

---

### Delt hæftelse

| Felt | Værdi |
|---|---|
| **ID** | `delt-haeftelse` |
| **Dansk term** | Delt hæftelse |
| **Engelsk ækvivalent** | Several Liability |
| **Specialisering af** | Hæftelse |
| **Implementeringsstatus** | `missing` — debt-service |

**Definition:**
Hæftelse, hvor flere skyldnere hæfter for adskilte eller fordelte andele af samme fordring eller kravkompleks.

---

## Myndighedsudøvelse

### Inddrivelsesskridt *(abstrakt)*

| Felt | Værdi |
|---|---|
| **ID** | `inddrivelsesskridt` |
| **Dansk term** | Inddrivelsesskridt |
| **Engelsk ækvivalent** | Debt Collection Measure |
| **Abstrakt** | Ja |
| **Implementeringsstatus** | `partial` — case-service (`CollectionStrategy`) |

**Definition:**
Abstrakt begreb for retlige eller administrative skridt, der iværksættes for at opnå betaling af en overdraget restance.

**Note:** Tilgængelige inddrivelsesskridt afhænger af fordringens fordringsart og fordringstype. Civilretlige fordringer har begrænsede muligheder.

**Specialiseringer:** Modregning, Lønindeholdelse, Udlæg, Afdragsordning

**Relationer:**

| Relation | Målbegreb |
|---|---|
| vedrører | Restance |

---

### Modregning

| Felt | Værdi |
|---|---|
| **ID** | `modregning` |
| **Dansk term** | Modregning |
| **Engelsk ækvivalent** | Set-off |
| **Specialisering af** | Inddrivelsesskridt |
| **Implementeringsstatus** | `partial` — payment-service |

**Definition:**
Inddrivelsesskridt, hvor et offentligt tilgodehavende søges dækket gennem modregning i et modgående krav eller beløb efter gældende regler.

---

### Lønindeholdelse

| Felt | Værdi |
|---|---|
| **ID** | `loenindeholdelse` |
| **Dansk term** | Lønindeholdelse |
| **Engelsk ækvivalent** | Wage Garnishment |
| **Specialisering af** | Inddrivelsesskridt |
| **Implementeringsstatus** | `partial` — wage-garnishment-service |

**Definition:**
Inddrivelsesskridt, hvor betaling søges opnået gennem indeholdelse i skyldners løn eller anden indkomst efter gældende regler.

**Note:** Generelt ikke tilgængelig for civilretlige fordringer.

---

### Udlæg

| Felt | Værdi |
|---|---|
| **ID** | `udlaeg` |
| **Dansk term** | Udlæg |
| **Engelsk ækvivalent** | Attachment |
| **Specialisering af** | Inddrivelsesskridt |
| **Implementeringsstatus** | `missing` — *(ingen service endnu)* |

**Definition:**
Inddrivelsesskridt, hvor der foretages udlæg efter de regler, der gælder for tvangsfuldbyrdelse og offentlig inddrivelse.

---

### Afdragsordning

| Felt | Værdi |
|---|---|
| **ID** | `afdragsordning` |
| **Dansk term** | Afdragsordning |
| **Engelsk ækvivalent** | Instalment Arrangement |
| **Specialisering af** | Inddrivelsesskridt |
| **Implementeringsstatus** | `missing` — case-service |

**Definition:**
Inddrivelsesskridt, hvor der indgås en betalingsaftale med skyldner om afdragsvis indfrielse af restancen.

---

## Oversigtstabel

| Område | Antal begreber | exists | partial | missing |
|---|---|---|---|---|
| Aktører og roller | 4 | 0 | 3 | 1 |
| Krav og status | 7 | 0 | 4 | 3 |
| Kravstruktur | 1 | 0 | 0 | 1 |
| Livscyklus | 7 | 0 | 0 | 7 |
| Finansiel | 2 | 0 | 0 | 2 |
| Kommunikation | 13 | 0 | 1 | 12 |
| Reaktion og tvist | 5 | 0 | 0 | 5 |
| Myndighedsudøvelse | 5 | 0 | 2 | 3 |
| **Total** | **44** | **0** | **10** | **34** |

> **Næste skridt:** Review `domain/concept-model.yaml` og lad en domæneekspert godkende det.
> Sæt `approved_by` og `approved_date` når klar.
