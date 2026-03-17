# Begrebsmodel for inddrivelsesområdet (v3)

## Ændringslog

| Version | Dato | Ændring |
|---|---|---|
| v1 | — | Oprindelig model |
| v2 | — | Tilføjelse af Underretningsmeddelelse-familie, Hæftelsesspecialiseringer, Inddrivelsesskridt-specialiseringer |
| v3 | 2026-03-17 | Tilføjelse af livscyklusbegreber (Regulering, Tilbagekald, Genindsendelse, Høring), finansielle begreber (Dækning, Inddrivelsesrente, Forældelse), klassifikationsbegreber (Fordringstype, Fordringsart), Fordringskompleks, Afdragsordning. PSRM-referencemateriale krydsverificeret. |

## 1. Modelleringsprincipper

### 1.1 Grundlæggende princip

Modellen følger de fællesoffentlige principper om, at data og systemer skal tage udgangspunkt i forretningens begreber. Derfor beskrives først de begrebsmæssige enheder og deres relationer, før eventuel omsætning til felter, tabeller, API-ressourcer og hændelser.

### 1.2 Abstrakte og konkrete begreber

I modellen anvendes både abstrakte og konkrete begreber:

- **abstrakt begreb**: et begreb der ikke nødvendigvis forekommer alene i forretningen, men som samler fælles egenskaber og relationer
- **konkret begreb**: et begreb der forekommer direkte i forretningspraksis og kan identificeres som selvstændigt domæneobjekt

Følgende abstrakte begreber introduceres i modellen:

- `Part`
- `Underretning`
- `Skyldnerunderretning`
- `Underretningsmeddelelse`
- `Inddrivelsesskridt`
- `Regulering`

### 1.3 Generalisering og specialisering

Modellen anvender specialisering, når et underbegreb:

1. arver centrale egenskaber fra et overbegreb
2. samtidig har en mere snæver betydning
3. kræver særskilte regler, relationer eller forretningsmæssig behandling

Det gælder især:

- `Fordringshaver`, `Skyldner` og `Restanceinddrivelsesmyndighed` som specialiseringer af `Part`
- `Skyldnerunderretning` og `Underretningsmeddelelse` som specialiseringer af `Underretning`
- `Påkrav` og `Rykker` som specialiseringer af `Skyldnerunderretning`
- de forskellige PSRM-kvitteringer som specialiseringer af `Underretningsmeddelelse`
- `Restance` som specialisering af `Fordring`
- `Modregning`, `Lønindeholdelse`, `Udlæg` og `Afdragsordning` som specialiseringer af `Inddrivelsesskridt`
- `Opskrivning` og `Nedskrivning` som specialiseringer af `Regulering`

### 1.4 Associationsbegreber

Når en relation mellem to begreber selv har vigtig forretningsmæssig betydning, modelleres relationen som et selvstændigt begreb. Det gælder i modellen især:

- `Hæftelse`, fordi forholdet mellem `Skyldner` og `Fordring` ikke altid kan beskrives tilstrækkeligt med en simpel linje.
- `Dækning`, fordi betalingen har selvstændig betydning med egne regler for ophævelse og dækningsrækkefølge.

## 2. Overordnet begrebsstruktur

Modellen organiserer domænet i følgende hovedområder:

| Område | Formål | Centrale begreber |
|---|---|---|
| Aktører og roller | Identificerer domænets parter | Part, Fordringshaver, Skyldner, Restanceinddrivelsesmyndighed |
| Krav og status | Beskriver betalingskravet og dets udvikling | Kravgrundlag, Fordring, Restance, Betalingsfrist, Forældelse, Fordringsart, Fordringstype |
| Kravstruktur | Beskriver sammensætning af fordringer | Fordringskompleks, hovedfordring-underfordring-relation |
| Livscyklus | Beskriver fordringens overgang og tilstandsændringer | Overdragelse til inddrivelse, Høring, Regulering, Opskrivning, Nedskrivning, Tilbagekald, Genindsendelse |
| Finansiel | Beskriver pengestrømme og renter | Dækning, Inddrivelsesrente |
| Kommunikation | Beskriver formelle meddelelser til skyldner og fordringshaver | Underretning, Skyldnerunderretning, Påkrav, Rykker, Underretningsmeddelelse og dens specialiseringer |
| Reaktion og tvist | Beskriver bestridelse og hæftelsesforhold | Indsigelse, Hæftelse |
| Myndighedsudøvelse | Beskriver inddrivelsestiltag | Inddrivelsesskridt, Modregning, Lønindeholdelse, Udlæg, Afdragsordning |

### 2.1 Dansk-engelsk begrebsoversigt

| Dansk term | Engelsk ækvivalent | Bemærkning |
|---|---|---|
| Part | Party | Abstrakt overbegreb. |
| Fordringshaver | Creditor | Offentlig kontekst kan ved behov præciseres som public creditor. |
| Skyldner | Debtor | Part, der hæfter helt eller delvist for kravet. |
| Restanceinddrivelsesmyndighed | Public Debt Collection Authority | Generisk myndighedsbegreb. |
| Kravgrundlag | Basis of Claim | Det retlige eller faktiske grundlag for fordringen. |
| Fordring | Claim | Det centrale betalingskrav. |
| Restance | Overdue Claim | Specialisering af fordring i modellen. |
| Betalingsfrist | Payment Deadline | Tidspunkt eller periode for rettidig betaling. |
| Forældelse | Limitation / Prescription | Tidspunkt hvor fordringen mister retskraft. |
| Fordringsart | Claim Art | Overordnet klassifikation: INDR (inddrivelse) eller MODR (modregning). |
| Fordringstype | Claim Type | Specifik klassifikation baseret på retligt grundlag (fx PSRESTS = restskat). |
| Fordringskompleks | Claim Complex | Samling af hovedfordring og tilknyttede fordringer. |
| Underretning | Notification | Abstrakt overbegreb for formel meddelelse. |
| Skyldnerunderretning | Debtor Notice | Abstrakt underbegreb for meddelelser til skyldner. |
| Påkrav | Demand for Payment | Formel anmodning om betaling inden frist. |
| Rykker | Reminder Notice | Meddelelse efter manglende rettidig betaling. |
| Underretningsmeddelelse | Creditor Notification | Kvitteringspræget meddelelse til fordringshaver. |
| Afregningsunderretning | Settlement Notification | Meddelelse om periodisk afregning af indbetalinger. |
| Udligningsunderretning | Reconciliation Notification | Meddelelse om saldoændringer på fordring. |
| Allokeringsunderretning | Allocation Notification | Meddelelse om fordeling mellem hovedstol, renter mv. |
| Renteunderretning | Interest Notification | Meddelelse om rentetilskrivning. |
| Detaljeret renteunderretning | Detailed Interest Notification | Detaljeret meddelelse om nye renteposteringer. |
| Afskrivningsunderretning | Write-off Notification | Meddelelse om afskrivning af fordring. |
| Tilbagesendelsesunderretning | Return-to-Creditor Notification | Meddelelse om tilbagesendelse til fordringshaver. |
| Returneringsunderretning | Returned Claim Notification | Meddelelse om returnering af fordring. |
| Indsigelse | Objection | Skyldners bestridelse af fordringen. |
| Hæftelse | Liability | Forhold, der knytter skyldner til fordring. |
| Enehæftelse | Sole Liability | Kun én skyldner hæfter. |
| Solidarisk hæftelse | Joint and Several Liability | Flere skyldnere hæfter solidarisk. |
| Delt hæftelse | Several Liability | Flere skyldnere hæfter for fordelte andele. |
| Overdragelse til inddrivelse | Transfer for Debt Collection | Overgang til restanceinddrivelsesmyndigheden. |
| Høring | Hearing | Venteposition hvor stamdata afviger fra indgangsfilter. |
| Regulering | Claim Adjustment | Abstrakt overbegreb for op- og nedskrivning. |
| Opskrivning | Write-up | Forhøjelse af fordringens saldo. |
| Nedskrivning | Write-down | Nedsættelse af fordringens saldo. |
| Tilbagekald | Withdrawal | Fordringens tilbagetrækning fra inddrivelse. |
| Genindsendelse | Resubmission | Fornyet indsendelse efter tilbagekald. |
| Dækning | Recovery / Payment | Betaling opnået under inddrivelse. |
| Inddrivelsesrente | Recovery Interest | Rente tilskrevet en restance under inddrivelse. |
| Inddrivelsesskridt | Debt Collection Measure | Abstrakt overbegreb for inddrivelsestiltag. |
| Modregning | Set-off | Dækning gennem modgående krav eller beløb. |
| Lønindeholdelse | Wage Garnishment | Indeholdelse i løn eller anden indkomst. |
| Udlæg | Attachment / Seizure | Udlæg efter regler om tvangsfuldbyrdelse. |
| Afdragsordning | Instalment Arrangement | Betalingsordning aftalt med skyldner. |

## 3. Formelle modelleringsvalg

### 3.1 `Part` som abstrakt overbegreb

Tidligere stod `fordringshaver`, `skyldner` og `restanceinddrivelsesmyndighed` som sideordnede begreber. Her samles de under det abstrakte overbegreb `Part`.

Det giver følgende fordele:

- fælles sted at forankre identitet, kontaktoplysninger og rollebaseret kommunikation
- mulighed for at samme organisatoriske enhed i forskellige sammenhænge kan have forskellige roller
- mere formel og genbrugelig model ved overgang til informationsarkitektur

### 3.2 `Restance` som specialisering af `Fordring`

`Restance` modelleres som **specialisering af `Fordring`**.

Det betyder begrebsmæssigt:

- alle restancer er fordringer
- ikke alle fordringer er restancer
- restancer arver fordringens grundlæggende semantik, men får egen betydning i relation til overdragelse og inddrivelse

### 3.3 `Underretning` som overbegreb

`Underretning` er et abstrakt overbegreb for formelle meddelelser om en fordring. Det opdeles i:

- `Skyldnerunderretning`, som retter sig mod skyldner
- `Underretningsmeddelelse`, som retter sig mod fordringshaver

### 3.4 `Underretningsmeddelelse` som særskilt meddelelsesfamilie

`Underretningsmeddelelse` er et abstrakt underbegreb under `Underretning` med specialiseringer:

- `Afregningsunderretning`
- `Udligningsunderretning`
- `Allokeringsunderretning`
- `Renteunderretning`
- `Detaljeret renteunderretning`
- `Afskrivningsunderretning`
- `Tilbagesendelsesunderretning`
- `Returneringsunderretning`

Gensidigt eksklusive valg: Udligningsunderretning og Allokeringsunderretning kan ikke begge være aktive for samme fordringshaver (man vælger én). Tilsvarende: Renteunderretning og Detaljeret renteunderretning er gensidigt eksklusive; Detaljeret renteunderretning er kun tilgængelig for system-til-system-løsning.

### 3.5 `Hæftelse` som selvstændigt begreb

`Hæftelse` er et selvstændigt associationsbegreb mellem `Fordring` og `Skyldner`.

**Bemærkning vedr. PSRM-begrænsning:** PSRM understøtter i praksis kun solidarisk hæftelse. Ved ikke-solidarisk hæftelse skal fordringshaver selv fordele fordringen mellem skyldnere som separate fordringer. Begrebsmodellen bevarer de tre specialiseringer (Ene-, Solidarisk, Delt) for at afspejle den begrebsmæssige virkelighed, men implementeringsmodellen bør notere denne begrænsning.

### 3.6 Specialisering af `Inddrivelsesskridt`

Inddrivelsesskridt opdeles i undertyper med forskellige retlige forudsætninger:

- `Modregning`
- `Lønindeholdelse`
- `Udlæg`
- `Afdragsordning`

**Bemærkning vedr. civilretlige fordringer:** For civilretlige fordringer (fordringer der stammer fra offentlig aktivitet men ikke offentligretlig myndighedsudøvelse) gælder begrænsede inddrivelsesmuligheder. Lønindeholdelse er generelt ikke tilgængelig. Udlæg kræver eksekutionsfundament (dom, betalingspåkrav eller skylderklæring). Denne begrænsning styres af fordringens Fordringstype.

### 3.7 `Fordringstype` og `Fordringsart` som klassifikationsbegreber (NYT)

Modellen indføres to klassifikationsbegreber for fordringer:

- `Fordringsart` er den overordnede klassifikation: INDR (inddrivelse) eller MODR (kun modregning). Fordringsarten bestemmer, hvilke inddrivelsesskridt der er tilgængelige.
- `Fordringstype` er den specifikke klassifikation baseret på retligt grundlag (fx PSRESTS = restskat, POBODPO = bøder). Fordringstypen bestemmer, hvilke stamdata-krav der gælder, hvilke renteberegningsregler der er aktive, og hvilke valideringsregler der anvendes ved indgangsfiltrering.

### 3.8 `Forældelse` som selvstændigt begreb (NYT)

Modellen skelner nu mellem `Betalingsfrist` (hvornår betaling forfalder) og `Forældelse` (hvornår fordringen mister retskraft). Begge er tidsmæssige begreber knyttet til Fordring, men med fundamentalt forskellig betydning:

- Betalingsfrist overskridet udløser restancestatus
- Forældelse overskridet udløser tab af retskraft og krav på afskrivning

Forældelsesdatoen skal afspejle den aktuelle dato ved oversendelse til inddrivelse (ikke den oprindelige). Forældelsesafbrydelse kan ske ved skyldserkendelse, betalingspåkrav, fogedretudlæg eller meddelelse om modregning.

### 3.9 `Regulering` som abstrakt overbegreb for op- og nedskrivning (NYT)

Fordringshaver kan til enhver tid regulere allerede overdragne restancer. Reguleringen modelleres som abstrakt overbegreb med to specialiseringer:

- `Opskrivning`: forhøjelse af saldo med specifik årsagskode (OpskrivRegulering, FejlagtigHovedstolIndberetning, m.fl.)
- `Nedskrivning`: nedsættelse af saldo med specifik årsagskode (INDB, REGU, m.fl.)

Regulering er en fordringshaver-handling på en allerede overdraget restance. Den adskiller sig fra overdragelse (som er den initiale handling) og fra tilbagekald (som trækker fordringen helt tilbage).

En opskrivning danner sammen med den oprindelige fordring et `Fordringskompleks`.

### 3.10 `Tilbagekald` som selvstændigt livscyklusbegreb (NYT)

Tilbagekald er den handling, hvorved fordringshaver trækker en restance tilbage fra inddrivelse. Tilbagekald har væsentligt forskellige virkninger afhængigt af årsagskode:

| Årsagskode | Virkning på dækninger | Virkning på renter | Kan genindsendes |
|---|---|---|---|
| BORD (Betalingsordning) | Fastholdes | Beregnes til tilbagekaldsdato | Kun via FEJL |
| BORT (Bortfald) | Fastholdes (før virkningsdato) | Beregnes til virkningsdato | Kun via FEJL |
| FEJL | Ophæves alle | Nulstilles alle fra modtagelse | Nej — ny fordring kræves |
| HENS (Henstand) | Fastholdes | Beregnes til tilbagekaldsdato | Ja (Genindsendelse) |
| KLAG (Klage) | Fastholdes | Beregnes til tilbagekaldsdato | Ja (Genindsendelse) |

Tilbagekald af hovedfordring medfører automatisk tilbagekald af relaterede fordringer. Relaterede fordringer kan tilbagekaldes selvstændigt. Fordringer med statsrefusion kan som udgangspunkt ikke tilbagekaldes (undtaget FEJL og KLAG).

### 3.11 `Genindsendelse` som selvstændigt livscyklusbegreb (NYT)

Genindsendelse er fornyet indsendelse af en tidligere tilbagekaldt restance. Begrebet modelleres selvstændigt, fordi det:

- kræver reference til den oprindelige fordring
- stiller krav om overensstemmelse med visse stamdata fra originalen (stiftelsesdato, forfaldsdato, periode)
- resulterer i nyt fordrings-ID men genbruger den oprindelige identitet
- kun er tilgængeligt efter KLAG- eller HENS-tilbagekald (aldrig efter FEJL)

### 3.12 `Høring` som livscyklustilstand (NYT)

Høring er den tilstand, en indsendt fordring befinder sig i, når dens stamdata afviger fra indgangsfilterets regler. Fordringen er i denne tilstand IKKE modtaget til inddrivelse, og fordringshaveres egne forældelsesregler gælder.

Høring afsluttes ved at fordringshaver enten:
- godkender indsendelsen (med skriftlig begrundelse), hvorefter Gældsstyrelsen vurderer
- fortryder og trækker fordringen tilbage

### 3.13 `Dækning` som selvstændigt begreb (NYT)

Dækning er betaling opnået under inddrivelse. Begrebet modelleres selvstændigt, fordi:

- dækninger har selvstændige regler for ophævelse (FEJL-tilbagekald ophæver; KLAG-tilbagekald fastholder)
- dækningsrækkefølge fastlægger, at inddrivelsesrente dækkes forud for hovedstol
- dækninger er genstand for afstemning mellem fordringshaver og restanceinddrivelsesmyndighed
- dækninger afregnes periodisk via afregningsunderretning

### 3.14 `Inddrivelsesrente` som selvstændigt begreb (NYT)

Inddrivelsesrente er rente tilskrevet en restance under inddrivelse. Begrebet modelleres selvstændigt, fordi det:

- har sin egen beregningsregel (simpel dag-til-dag, p.t. 5,75 % p.a.)
- beregnes fra 1. i måneden efter modtagelse
- dækkes forud for hovedstol i dækningsrækkefølgen
- kan nulstilles ved FEJL-tilbagekald
- kan suspenderes ved rentestop for uafklaret gæld
- kan fraviges for specifikke fordringstyper (bøder er undtaget; afvigende rentesats kan aftales)
- behandles selvstændigt ved opskrivning og tilbagekald

### 3.15 `Fordringskompleks` som strukturbegreb (NYT)

Fordringskompleks er den sammensætning, der opstår når en fordring får tilknyttede fordringer:

- en hovedfordring kan have underfordringer (typisk renter, opskrivninger)
- underfordringer refererer til hovedfordringen via et hovedfordrings-ID
- opskrivning danner et fordringskompleks med den oprindelige fordring og behandles i størst muligt omfang som én samlet fordring
- tilbagekald af hovedfordring medfører automatisk tilbagekald af hele fordringskomplekset

Relationen modelleres som selvreference på `Fordring`: en fordring kan være underfordring af en anden fordring.

## 4. Begrebskatalog

### Aktører og roller

#### 4.1 Part

**Foretrukken term:** `Part`

**Definition:** Abstrakt aktørbegreb, der dækker en identificerbar enhed, som kan indgå i relationer om fordringer, kommunikation og inddrivelse.

**Note:** `Part` anvendes som fælles overbegreb og forventes normalt ikke at forekomme alene i forretningen.

**Specialiseringer:** Fordringshaver, Skyldner, Restanceinddrivelsesmyndighed

#### 4.2 Fordringshaver

**Definition:** Part, der har eller administrerer en fordring mod en skyldner og som kan fremsende skyldnerunderretning, foretage overdragelse til inddrivelse, regulere overdragne fordringer, tilbagekalde fordringer og modtage underretningsmeddelelser fra restanceinddrivelsesmyndigheden.

**Specialisering af:** `Part`

#### 4.3 Skyldner

**Definition:** Part, der hæfter helt eller delvist for en fordring og som kan modtage skyldnerunderretning og fremsætte indsigelse.

**Note:** Skyldner identificeres via CPR, CVR/SE eller AKR-nummer. AKR-nummer tildeles personer og virksomheder uden dansk CPR/CVR. For I/S (interessentskaber) skal alle personligt hæftende interessenter indberettes. For PEF (personligt ejede firmaer) linkes ejerens CPR automatisk efter indsendelse.

**Specialisering af:** `Part`

#### 4.4 Restanceinddrivelsesmyndighed

**Definition:** Part, der efter overdragelse forestår inddrivelse af offentlige fordringer efter gældende regler og kan sende underretningsmeddelelser til fordringshaver om handlinger på restancer.

**Specialisering af:** `Part`

### Krav, klassifikation og status

#### 4.5 Kravgrundlag

**Definition:** Det retlige eller faktiske grundlag, der begrunder en fordring.

**Note:** Kravgrundlaget kan eksempelvis være afgørelse, hjemmel, beregning eller anden retsskabende begivenhed. Det retlige grundlag udtrykkes operationelt gennem Fordringstype.

#### 4.6 Fordring

**Definition:** Et krav på betaling, som en fordringshaver har mod en eller flere skyldnere.

**Note:** Relationen til skyldner går via `Hæftelse`. En fordring kan være en hovedfordring i et fordringskompleks og kan have underfordringer (renter, opskrivninger).

#### 4.7 Restance

**Definition:** Fordring, hvis betalingsfrist er overskredet uden rettidig betaling.

**Specialisering af:** `Fordring`

**Note:** `Restance` er den specialisering af fordring, som kan danne genstand for overdragelse til inddrivelse, regulering, tilbagekald og efterfølgende inddrivelsesskridt.

#### 4.8 Betalingsfrist

**Definition:** Det tidspunkt eller den periode, inden for hvilken fordringen skal være betalt.

**Note:** Sidste rettidige betalingsdato (SRB) er den operative udmøntning. Henstand eller betalingsordning ændrer SRB. Rykkerskrivelser udskyder IKKE SRB.

#### 4.9 Forældelse

**Definition:** Det tidspunkt, hvor fordringen mister retskraft og ikke længere kan gøres gældende.

**Note:** Forældelsesdato er obligatorisk stamdata ved overdragelse og skal afspejle den aktuelle dato (ikke den oprindelige). Forældelse kan afbrydes ved skyldserkendelse, betalingspåkrav, fogedretudlæg eller meddelelse om modregning. For civilretlige fordringer er mulighederne for forældelsesafbrydelse begrænsede, da pantefogedudlæg ikke er tilgængeligt.

#### 4.10 Fordringsart

**Definition:** Overordnet klassifikation af en fordring, der bestemmer det grundlæggende behandlingsregime.

**Note:** De kendte værdier er: INDR (inddrivelse — fuld inddrivelse med alle skridt) og MODR (modregning — kun modregning). I PSRM eksisterer kun INDR; MODR er et ældre DMI-begreb. Fordringsart styrer, hvilke inddrivelsesskridt der er tilgængelige.

#### 4.11 Fordringstype

**Definition:** Specifik klassifikation af en fordring baseret på det retlige grundlag, der bestemmer valideringsregler, renteberegningsregler, indgangsfilterregler og tilladt adfærd.

**Note:** Eksempler: PSRESTS (restskat), POBODPO (politibøder), CFFAKTU (civilretlige fakturakrav). Fordringstypen bestemmer, hvilke stamdata der er obligatoriske, hvilken rentesats der gælder, og om fordringen er civilretlig (med begrænsede inddrivelsesmuligheder) eller offentligretlig. Fordringstypen er forankret i fordringshaveraftalen.

#### 4.12 Fordringskompleks

**Definition:** Sammensætning af en hovedfordring og tilknyttede fordringer (underfordringer), der i størst muligt omfang behandles som én samlet fordring.

**Note:** Et fordringskompleks opstår, når en fordring opskrives (opskrivningen danner en ny delfordring med eget modtagelsestidspunkt men samme fordrings-ID) eller når renter indsendes som selvstændige relaterede fordringer. Tilbagekald af hovedfordring medfører automatisk tilbagekald af hele komplekset. Alle dele deler hæftelsesstruktur.

### Livscyklus

#### 4.13 Overdragelse til inddrivelse

**Definition:** Den handling, hvorved fordringshaver sender en restance til restanceinddrivelsesmyndigheden med henblik på videre inddrivelse.

**Note:** Forudsætter at sædvanlig rykkerprocedure er gennemført forgæves, at skyldner er underrettet skriftligt, og at betalingsfrist er overskredet. Hovedfordringer og renter/gebyrer skal overdrages som selvstændige fordringer. Ved overdragelse returneres en kvittering med fordrings-ID og slutstatus (UDFØRT, AFVIST eller HØRING).

#### 4.14 Høring

**Definition:** Livscyklustilstand, hvor en indsendt fordring afventer fordringshaveres stillingtagen, fordi stamdata afviger fra indgangsfilterets regler.

**Note:** En fordring i høring er IKKE modtaget til inddrivelse. Fordringshaveres egne forældelsesregler gælder i denne periode. Fordringshaver kan godkende indsendelsen (med skriftlig begrundelse) eller fortryde. Ved godkendelse skifter status til "Afventer RIM", og en sagsbehandler vurderer inden for 14 dage. Gældsstyrelsen kan: godkende, afvise, eller tilpasse indgangsfilteret.

#### 4.15 Regulering

**Definition:** Abstrakt begreb for en fordringshaver-handling, der justerer saldoen på en allerede overdraget restance.

**Specialiseringer:** Opskrivning, Nedskrivning

**Note:** Fordringshaver bør altid afstemme saldoen inden regulering for at undgå uoverensstemmelser (fx ved direkte indbetaling fra skyldner).

#### 4.16 Opskrivning

**Definition:** Regulering, der forhøjer saldoen på en overdraget restance.

**Specialisering af:** `Regulering`

**Note:** Renter kan IKKE opskrives; de skal indsendes som ny relateret fordring. Opskrivningsfordringen danner et fordringskompleks med den oprindelige. Kendte årsagskoder:

- OpskrivRegulering: saldojustering (fx ompostering der fjerner indbetaling)
- FejlagtigHovedstolIndberetning: ændrer den oprindelige hovedstol til højere beløb (indberettet beløb er ny hovedstol, ikke difference; påvirker ikke saldo)
- NAOR (NedskrivningAnnulleretOpskrivningRegulering): annullerer en fejlagtig opskrivning

Krav: skriftlig underretning af skyldner påkrævet. Inddrivelsesrenter beregnes selvstændigt fra 1. i måneden efter indsendelse af opskrivning.

#### 4.17 Nedskrivning

**Definition:** Regulering, der nedsætter saldoen på en overdraget restance.

**Specialisering af:** `Regulering`

**Note:** Nedskrivning kan ikke overstige restsaldoen (afvises). Kendte årsagskoder:

- INDB (Indbetaling): indbetaling modtaget fra skyldner efter oversendelse. Virkningsdato = indbetalingsdato. Ved samtidige indbetalinger afgør bogføringstidspunkt. Fordringshaver må IKKE udbetale direkte til skyldner ved dobbeltbetaling.
- REGU (Regulering): korrektion i eget system. Virkningsdato = altid fordringens modtagelsesdato. For meget afregnet trækkes tilbage; inddrivelsesrenter nulstilles på reguleret beløb.
- OANI (OpskrivningAnnulleretNedskrivningIndbetaling): annullerer fejlagtig INDB-nedskrivning.
- OONR (OpskrivningOmgjortNedskrivningRegulering): omgør fejlagtig REGU-nedskrivning.

#### 4.18 Tilbagekald

**Definition:** Den handling, hvorved fordringshaver trækker en overdraget restance tilbage fra inddrivelse.

**Note:** Tilbagekald har fundamentalt forskellige virkninger afhængigt af årsagskode (se tabel i afsnit 3.10). Tilbagekald af hovedfordring medfører automatisk tilbagekald af relaterede fordringer. Relaterede fordringer kan tilbagekaldes selvstændigt. Fordringer med statsrefusion kan som udgangspunkt ikke tilbagekaldes (undtaget FEJL og KLAG). En tilbagekaldt fordring låses og kan i visse tilfælde genindsendes.

#### 4.19 Genindsendelse

**Definition:** Fornyet indsendelse af en tidligere tilbagekaldt restance til inddrivelse.

**Note:** Kun tilgængelig efter KLAG- eller HENS-tilbagekald. FEJL-tilbagekaldte fordringer kan IKKE genindsendes og skal i stedet oprettes som helt nye fordringer. Genindsendelse kræver angivelse af den oprindelige fordring. Visse stamdata skal matche originalen (stiftelsesdato, forfaldsdato, periode). Den genindsendte fordring får tildelt nyt fordrings-ID. Forældelsesdato skal være den nyeste tilgængelige.

### Finansiel

#### 4.20 Dækning

**Definition:** Betaling opnået under inddrivelse af en restance.

**Note:** Dækning er genstand for dækningsrækkefølge: inddrivelsesrente dækkes forud for hovedstol. Dækningers skæbne ved tilbagekald afhænger af årsagskode: FEJL ophæver alle dækninger; KLAG og HENS fastholder dem. Dækninger afregnes periodisk til fordringshaver via afregningsunderretning og er genstand for afstemning.

#### 4.21 Inddrivelsesrente

**Definition:** Rente tilskrevet en restance under inddrivelse.

**Note:** Beregnes som simpel dag-til-dag rente (p.t. 5,75 % p.a. pr. 1. januar 2026) af hovedstol fra 1. i måneden efter modtagelse. Dækkes forud for hovedstol. Renteberegning kan ikke ændres efter overdragelse — er den angivet forkert, kræves tilbagekald med FEJL og ny indsendelse. Undtagelser: bøder (straffelovsbøder er helt undtaget), rentestop for uafklaret gæld (siden 1. november 2024), fordringer med aftalt afvigende rente. Ingen fradragsret for inddrivelsesrenter siden 1. januar 2020.

### Kommunikation

#### 4.22 Underretning

**Definition:** Abstrakt begreb for formel meddelelse til en relevant part om en fordring, dens status eller dens behandling.

**Specialiseringer:** Skyldnerunderretning, Underretningsmeddelelse

#### 4.23 Skyldnerunderretning

**Definition:** Abstrakt underbegreb for formel meddelelse til skyldner om betalingsforpligtelse eller konsekvenser af manglende betaling.

**Specialisering af:** `Underretning`

**Specialiseringer:** Påkrav, Rykker

#### 4.24 Påkrav

**Definition:** Skyldnerunderretning, der formelt anmoder skyldner om at opfylde betalingsforpligtelsen inden en angivet frist.

**Specialisering af:** `Skyldnerunderretning`

#### 4.25 Rykker

**Definition:** Skyldnerunderretning, der meddeler, at betaling ikke er sket rettidigt, og at yderligere handling kan følge.

**Specialisering af:** `Skyldnerunderretning`

#### 4.26 Underretningsmeddelelse

**Definition:** Abstrakt underbegreb for formel meddelelse fra restanceinddrivelsesmyndigheden til fordringshaver om nye handlinger på en overdraget restance.

**Note:** Fungerer som kvittering og orientering. Kan ikke hentes ældre end 3 måneder. Distributionskanal er enten system-til-system, portal eller e-Boks.

**Specialisering af:** `Underretning`

**Specialiseringer:** Afregningsunderretning, Udligningsunderretning, Allokeringsunderretning, Renteunderretning, Detaljeret renteunderretning, Afskrivningsunderretning, Tilbagesendelsesunderretning, Returneringsunderretning

#### 4.27 Afregningsunderretning

**Definition:** Underretningsmeddelelse, der orienterer fordringshaver om månedlig afregning af modtagne indbetalinger på en eller flere restancer.

**Note:** Sendes månedligt, sidste hverdag. Periode: sidste bankdag forrige måned til dagen før sidste bankdag indeværende. Indhold: identifikationsnummer, beløb, dato, fordring. Afregning sendes til NemKonto.

**Specialisering af:** `Underretningsmeddelelse`

#### 4.28 Udligningsunderretning

**Definition:** Underretningsmeddelelse, der orienterer fordringshaver om bevægelser på en restance, som påvirker saldoen.

**Note:** Sendes dagligt ved saldoændring (dækning eller saldostigning). Bruges til at holde saldi ajour — vigtigt for nedskrivning. Gensidigt eksklusiv med Allokeringsunderretning.

**Specialisering af:** `Underretningsmeddelelse`

#### 4.29 Allokeringsunderretning

**Definition:** Underretningsmeddelelse, der orienterer fordringshaver om, hvordan saldoændringer fordeles mellem hovedstol, renter og andre finansielle komponenter.

**Note:** Som Udligningsunderretning, men viser derudover fordeling mellem afdrag på hovedstol og dækning af renter. Inkluderer dækningsophævelser ved krydsende finansielle transaktioner. Gensidigt eksklusiv med Udligningsunderretning.

**Specialisering af:** `Underretningsmeddelelse`

#### 4.30 Renteunderretning

**Definition:** Underretningsmeddelelse, der orienterer fordringshaver om rentetilskrivning på en restance.

**Note:** Månedlig rentetilskrivning. Ved tilbagekald med rente-returnering udspecificeres renter i kvitteringen. Gensidigt eksklusiv med Detaljeret renteunderretning.

**Specialisering af:** `Underretningsmeddelelse`

#### 4.31 Detaljeret renteunderretning

**Definition:** Underretningsmeddelelse, der orienterer fordringshaver detaljeret om nye renteposteringer, typisk pr. døgn.

**Note:** Kun tilgængelig for system-til-system-løsning. Gensidigt eksklusiv med Renteunderretning.

**Specialisering af:** `Underretningsmeddelelse`

#### 4.32 Afskrivningsunderretning

**Definition:** Underretningsmeddelelse, der orienterer fordringshaver om, at en restance er afskrevet.

**Note:** Årsager: forældelse, konkurs, dødsbo, gældssanering, åbenbart formålsløs eller uforholdsmæssigt omkostningsfuld inddrivelse. Ved flere hæftende skyldnere kan meddelelsen indeholde identifikationsnummer for den berørte hæfter.

**Specialisering af:** `Underretningsmeddelelse`

#### 4.33 Tilbagesendelsesunderretning

**Definition:** Underretningsmeddelelse, der orienterer fordringshaver om, at en restance tilbagesendes til fordringshaver.

**Specialisering af:** `Underretningsmeddelelse`

#### 4.34 Returneringsunderretning

**Definition:** Underretningsmeddelelse, der orienterer fordringshaver om, at en restance returneres, eller at der er opstået saldorelateret opfølgning på en tidligere tilbagekaldt fordring.

**Note:** Underretter også om saldostigninger på tilbagekaldte fordringer (fx fra omkontering).

**Specialisering af:** `Underretningsmeddelelse`

### Reaktion og tvist

#### 4.35 Indsigelse

**Definition:** Skyldners bestridelse af fordringens eksistens, størrelse, grundlag eller behandling.

**Note:** Indsigelse er ikke en underretning, men en reaktion. Ved klage med opsættende virkning (KLAG) skal fordringshaver tilbagekalde fordringen. Indsigelse kan resultere i tilbagekald med enten FEJL (skyldner får medhold) eller Genindsendelse (efter afgørelse).

#### 4.36 Hæftelse

**Definition:** Det retlige og forretningsmæssige forhold, der knytter en skyldner til en fordring og beskriver arten og omfanget af betalingsansvaret.

**Note:** PSRM understøtter i praksis kun solidarisk hæftelse. Ved ikke-solidarisk hæftelse skal fordringshaver fordele fordringen som separate fordringer. Alle medhæftere skal indberettes samtidig.

**Specialiseringer:** Enehæftelse, Solidarisk hæftelse, Delt hæftelse

#### 4.37 Enehæftelse

**Definition:** Hæftelse, hvor en fordring kun er knyttet til én skyldner som ansvarlig part.

**Specialisering af:** `Hæftelse`

#### 4.38 Solidarisk hæftelse

**Definition:** Hæftelse, hvor flere skyldnere hæfter for samme fordring på en måde, så kravet kan gøres gældende efter de regler, der gælder for solidarisk ansvar.

**Specialisering af:** `Hæftelse`

#### 4.39 Delt hæftelse

**Definition:** Hæftelse, hvor flere skyldnere hæfter for adskilte eller fordelte andele af samme fordring eller kravkompleks.

**Note:** Kræver i PSRM at fordringshaver selv fordeler fordringen som separate fordringer per skyldner. Begrebsmodellen bevarer begrebet, men implementeringsmodellen bør notere begrænsningen.

**Specialisering af:** `Hæftelse`

### Myndighedsudøvelse

#### 4.40 Inddrivelsesskridt

**Definition:** Abstrakt begreb for retlige eller administrative skridt, der iværksættes for at opnå betaling af en overdraget restance.

**Note:** Tilgængelige inddrivelsesskridt afhænger af fordringens fordringsart og fordringstype. Civilretlige fordringer har begrænsede muligheder (generelt ikke lønindeholdelse; udlæg kun med eksekutionsfundament).

**Specialiseringer:** Modregning, Lønindeholdelse, Udlæg, Afdragsordning

#### 4.41 Modregning

**Definition:** Inddrivelsesskridt, hvor et offentligt tilgodehavende søges dækket gennem modregning i et modgående krav eller beløb efter gældende regler.

**Note:** Krav for modregning: gensidighed, udjævnelighed, forfaldenhed og retskraft. GIL §8 giver indtrædelsesret for ikke-statslige fordringshavere til at modregne i statslige udbetalinger til skyldner.

**Specialisering af:** `Inddrivelsesskridt`

#### 4.42 Lønindeholdelse

**Definition:** Inddrivelsesskridt, hvor betaling søges opnået gennem indeholdelse i skyldners løn eller anden indkomst efter gældende regler.

**Note:** Generelt IKKE tilgængelig for civilretlige fordringer (jf. bilag 1 til GIL).

**Specialisering af:** `Inddrivelsesskridt`

#### 4.43 Udlæg

**Definition:** Inddrivelsesskridt, hvor der foretages udlæg efter de regler, der gælder for tvangsfuldbyrdelse og offentlig inddrivelse.

**Note:** For civilretlige fordringer kræves eksekutionsfundament (dom, betalingspåkrav eller skylderklæring) via retsplejeloven §478. For krav under 100.000 kr. kan Gældsstyrelsen indgive betalingspåkrav til fogedretten. Over 100.000 kr. skal fordringshaver selv anlægge stævning.

**Specialisering af:** `Inddrivelsesskridt`

#### 4.44 Afdragsordning

**Definition:** Inddrivelsesskridt, hvor der indgås en betalingsaftale med skyldner om afdragsvis indfrielse af restancen.

**Note:** Hjemmel i GIL §3 stk. 3. Kræver ikke eksekutionsfundament. Kan medføre tilbagekald med årsagskode BORD, hvis aftalen indgås efter fordringen er oversendt til inddrivelse.

**Specialisering af:** `Inddrivelsesskridt`

## 5. Formelle relationer

### 5.1 Associationsrelationer

| Nr. | Kildebegreb | Relation | Målbegreb | Kardinalitet | Bemærkning |
|---|---|---|---|---|---|
| 1 | Fordringshaver | har | Fordring | 1 til 0..* | En fordringshaver kan have mange fordringer. |
| 2 | Fordring | er begrundet i | Kravgrundlag | 1 til 1..* | En fordring skal kunne spores til kravgrundlag. |
| 3 | Fordring | har | Betalingsfrist | 1 til 1 | En gældende fordring har en betalingsfrist. |
| 4 | Fordring | har | Forældelse | 1 til 1 | En gældende fordring har en forældelsesdato. |
| 5 | Fordring | er klassificeret som | Fordringsart | 1 til 1 | Hver fordring har en overordnet klassifikation. |
| 6 | Fordring | har | Fordringstype | 1 til 1 | Hver fordring har en specifik type baseret på retligt grundlag. |
| 7 | Fordring | er underfordring af | Fordring | 0..1 til 0..* | En fordring kan være underfordring af en hovedfordring (selvreference). |
| 8 | Hæftelse | vedrører | Fordring | 1 til 1 | Hver hæftelse knytter sig til én fordring. |
| 9 | Hæftelse | vedrører | Skyldner | 1 til 1 | Hver hæftelse knytter sig til én skyldner. |
| 10 | Fordringshaver | giver | Skyldnerunderretning | 1 til 0..* | Skyldnerunderretning udsendes af fordringshaver. |
| 11 | Skyldnerunderretning | tilgår | Skyldner | 1 til 1..* | Meddelelsen kan have flere skyldnermodtagere. |
| 12 | Skyldnerunderretning | vedrører | Fordring | 1 til 1 | Skyldnerunderretning relaterer sig til et konkret krav. |
| 13 | Skyldner | fremsætter | Indsigelse | 1 til 0..* | Skyldner kan bestride kravet. |
| 14 | Indsigelse | vedrører | Fordring | 1 til 1 | Indsigelse relaterer sig til en konkret fordring. |
| 15 | Fordringshaver | foretager | Overdragelse til inddrivelse | 1 til 0..* | Overdragelse er en handling hos fordringshaver. |
| 16 | Overdragelse til inddrivelse | omfatter | Restance | 1 til 1..* | Overdragelse knyttes til restance. |
| 17 | Overdragelse til inddrivelse | sker til | Restanceinddrivelsesmyndighed | 1 til 1 | Modtageren er kompetent myndighed. |
| 18 | Overdragelse til inddrivelse | kan resultere i | Høring | 0..1 til 1 | En overdragelse kan udløse en høring. |
| 19 | Fordringshaver | foretager | Regulering | 1 til 0..* | Regulering er en handling hos fordringshaver. |
| 20 | Regulering | vedrører | Restance | 1 til 1 | Regulering justerer en overdraget restance. |
| 21 | Fordringshaver | foretager | Tilbagekald | 1 til 0..* | Tilbagekald er en handling hos fordringshaver. |
| 22 | Tilbagekald | vedrører | Restance | 1 til 1 | Tilbagekald gælder en overdraget restance. |
| 23 | Genindsendelse | vedrører | Restance | 1 til 1 | Genindsendelse sender en tilbagekaldt restance igen. |
| 24 | Genindsendelse | følger af | Tilbagekald | 1 til 1 | Genindsendelse er kun mulig efter tilbagekald. |
| 25 | Fordringshaver | foretager | Genindsendelse | 1 til 0..* | Genindsendelse er en handling hos fordringshaver. |
| 26 | Restanceinddrivelsesmyndighed | iværksætter | Inddrivelsesskridt | 1 til 0..* | Myndigheden kan iværksætte flere skridt. |
| 27 | Inddrivelsesskridt | vedrører | Restance | 1 til 1..* | Skridtet vedrører overdraget restance. |
| 28 | Restanceinddrivelsesmyndighed | opnår | Dækning | 1 til 0..* | Myndigheden opnår dækning gennem inddrivelse. |
| 29 | Dækning | vedrører | Restance | 1 til 1 | Dækning vedrører en specifik restance. |
| 30 | Inddrivelsesrente | tilskrives | Restance | 0..* til 1 | Rente tilskrives en overdraget restance. |
| 31 | Restanceinddrivelsesmyndighed | sender | Underretningsmeddelelse | 1 til 0..* | Meddelelser udsendes af myndigheden. |
| 32 | Underretningsmeddelelse | tilgår | Fordringshaver | 1 til 1..* | En meddelelse kan tilgå flere fordringshavere. |
| 33 | Underretningsmeddelelse | vedrører | Restance | 1 til 1..* | Meddelelsen vedrører en eller flere restancer. |

### 5.2 Generaliseringsrelationer

| Overbegreb | Underbegreb | Bemærkning |
|---|---|---|
| Part | Fordringshaver | Rolle som kravbærende eller kravadministrerende part |
| Part | Skyldner | Rolle som hæftende part |
| Part | Restanceinddrivelsesmyndighed | Rolle som inddrivende myndighed |
| Underretning | Skyldnerunderretning | Formelle meddelelser rettet mod skyldner |
| Underretning | Underretningsmeddelelse | Formelle meddelelser rettet mod fordringshaver |
| Skyldnerunderretning | Påkrav | Særlig type formel betalingsopfordring |
| Skyldnerunderretning | Rykker | Særlig type meddelelse efter manglende betaling |
| Underretningsmeddelelse | Afregningsunderretning | Kvittering for afregning af indbetalinger |
| Underretningsmeddelelse | Udligningsunderretning | Kvittering for saldoændring på restance |
| Underretningsmeddelelse | Allokeringsunderretning | Kvittering for fordeling af saldoændring |
| Underretningsmeddelelse | Renteunderretning | Kvittering for rentetilskrivning |
| Underretningsmeddelelse | Detaljeret renteunderretning | Detaljeret kvittering for renteposteringer |
| Underretningsmeddelelse | Afskrivningsunderretning | Kvittering for afskrivning |
| Underretningsmeddelelse | Tilbagesendelsesunderretning | Kvittering for tilbagesendelse |
| Underretningsmeddelelse | Returneringsunderretning | Kvittering for returnering |
| Fordring | Restance | Specialisering ved overskredet betalingsfrist |
| Hæftelse | Enehæftelse | Kun én ansvarlig skyldner |
| Hæftelse | Solidarisk hæftelse | Flere skyldnere med solidarisk ansvar |
| Hæftelse | Delt hæftelse | Flere skyldnere med opdelt ansvar |
| Regulering | Opskrivning | Forhøjelse af saldo |
| Regulering | Nedskrivning | Nedsættelse af saldo |
| Inddrivelsesskridt | Modregning | Dækning via modgående krav |
| Inddrivelsesskridt | Lønindeholdelse | Indeholdelse i løn eller indkomst |
| Inddrivelsesskridt | Udlæg | Tvangsfuldbyrdelse |
| Inddrivelsesskridt | Afdragsordning | Betalingsordning med skyldner |

## 6. Fortolkning af flere skyldnere og hæftelse

### 6.1 Én fordring med én skyldner

- én `Fordring`, én `Skyldner`, én `Enehæftelse`

### 6.2 Én fordring med flere solidarisk hæftende skyldnere

- én `Fordring`, flere `Skyldner`, én `Solidarisk hæftelse` pr. skyldnerrelation
- **PSRM-begrænsning:** Dette er den eneste understøttede flerpartshæftelse i PSRM. Alle medhæftere skal indberettes samtidigt.

### 6.3 Én fordring med flere skyldnere med delt ansvar

- Kræver i PSRM at fordringshaver opretter separate fordringer pr. skyldner
- Begrebsmodellen bevarer den formelle mulighed for `Delt hæftelse`, men implementeringsmodellen bør håndtere det via flere fordringer

### 6.4 I/S og PEF skyldnere

- **I/S (Interessentskab):** Indsend CVR for partnerskabet + CPR/CVR for alle personligt hæftende interessenter. Inddrivelse kan kun ske mod eksplicit indberettede skyldnere.
- **PEF (Personligt ejet firma):** Indsend CVR (aktiv) eller ejerens CPR (ophørt). PSRM tilknytter automatisk ejerens CPR efter indsendelse.

## 7. Fordringens livscyklus

Fordringens fulde livscyklus kan beskrives som følgende tilstandsovergange:

```
Fordring (oprettet)
    |
    v [betalingsfrist overskredet]
Restance
    |
    v [fordringshaver foretager overdragelse]
Overdragelse til inddrivelse
    |
    +---> UDFØRT: Restance under inddrivelse
    |         |
    |         +---> Regulering (Opskrivning / Nedskrivning)
    |         +---> Inddrivelsesskridt (Modregning, Lønindeholdelse, Udlæg, Afdragsordning)
    |         +---> Dækning (betaling opnået)
    |         +---> Inddrivelsesrente (tilskrives)
    |         +---> Tilbagekald (BORD, BORT, FEJL, HENS, KLAG)
    |         |         |
    |         |         +---> Genindsendelse (kun KLAG/HENS)
    |         |         +---> Ny fordring (kun efter FEJL)
    |         |
    |         +---> Afskrivning (forældelse, konkurs, etc.)
    |
    +---> AFVIST: Fordring afvist (valideringsfejl)
    |
    +---> HØRING: Fordring i venteposition
              |
              +---> Godkendt -> Afventer RIM
              +---> Fortrudt -> Fordringshaver korrigerer og genindsender
```

## 8. Semantiske hovedregler

1. Alle `Restancer` er `Fordringer`, men ikke alle `Fordringer` er `Restancer`.
2. Relation mellem `Fordring` og `Skyldner` bør i formel model gå via `Hæftelse`.
3. `Skyldnerunderretning` og `Underretningsmeddelelse` er to forskellige specialiseringer af `Underretning` med hver sin modtagerrolle.
4. `Påkrav` og `Rykker` er specialiseringer af `Skyldnerunderretning`.
5. `Underretningsmeddelelse` fungerer som kvittering til fordringshaver om handlinger på overdragede restancer.
6. `Inddrivelsesskridt` er et abstrakt overbegreb med egne undertyper. Tilgængelige undertyper afhænger af fordringsart og fordringstype.
7. `Overdragelse til inddrivelse` knyttes til `Restance` frem for enhver `Fordring`.
8. `Part` er et abstraherende overbegreb og skal ikke forveksles med konkrete organisatoriske enheder.
9. `Regulering` (Opskrivning/Nedskrivning) og `Tilbagekald` er fordringshaver-handlinger på allerede overdragede restancer.
10. `Opskrivning` danner et `Fordringskompleks` med den oprindelige fordring.
11. `Tilbagekald` af hovedfordring medfører automatisk tilbagekald af hele fordringskomplekset.
12. `Genindsendelse` er kun mulig efter KLAG- eller HENS-tilbagekald, aldrig efter FEJL.
13. `Inddrivelsesrente` dækkes forud for hovedstol i dækningsrækkefølgen.
14. `Forældelse` er distinkt fra `Betalingsfrist` — begge er tidsmæssige begreber, men med fundamentalt forskellig retsvirkning.
15. `Udligningsunderretning` og `Allokeringsunderretning` er gensidigt eksklusive. Tilsvarende: `Renteunderretning` og `Detaljeret renteunderretning`.

## 9. Konsekvenser for senere informationsmodellering

Modellen gør det lettere at omsætte begreberne til logisk informationsmodel:

- `Part` kan blive et fælles referenceobjekt med rollemarkering
- `Hæftelse` kan blive associationsentitet mellem `Fordring` og `Skyldner`
- `Underretning` kan blive fælles dokument- eller meddelelsesstruktur med modtagerrolle og undertyper
- `Underretningsmeddelelse` kan få selvstændige typekoder og metadata for distributionskanal, tilgængelighedsperiode og berørt restance
- `Inddrivelsesskridt` kan blive arvshierarki eller typekodebaseret struktur
- `Regulering` kan modelleres som hændelse med årsagskode, beløb og virkningsdato
- `Tilbagekald` kan modelleres som hændelse med årsagskode, virkningsdato og referencer til berørte dækninger og renter
- `Fordring` kan få selvreference for hovedfordring-underfordring-relation (fordringskompleks)
- `Fordringstype` kan implementeres som referencedata med tilknyttede valideringsregler og renteopsætning
- `Dækning` kan modelleres med dækningsrækkefølge (rente før hovedstol) og ophævelsesstatus
- `Inddrivelsesrente` kan modelleres med beregningsparametre (sats, startdato, suspensionsflag)

## 10. Kildehenvisninger

### 10.1 Metodisk grundlag

- DIGST, de fællesoffentlige regler for begrebs- og datamodellering:  
  `https://arkitektur.digst.dk/metoder/begrebs-og-datametoder/regler-begrebs-og-datamodellering`

### 10.2 Juridisk vejledning og domænekilder

- Den juridiske vejledning, G.A. Inddrivelse:  
  `https://info.skat.dk/data.aspx?oid=9672`
- Den juridiske vejledning, G.A.1.3 Fordringshaver:  
  `https://info.skat.dk/data.aspx?oid=2304023`
- Den juridiske vejledning, G.A.1.3.1 Indsigelser om kravets eksistens og størrelse:  
  `https://info.skat.dk/data.aspx?oid=2304024`
- Den juridiske vejledning, G.A.1.3.2 Overdragelse af fordringer til inddrivelse:  
  `https://info.skat.dk/data.aspx?oid=2304026`
- Den juridiske vejledning, G.A.1.3.3 Flere skyldnere og skyldnerskifte:  
  `https://info.skat.dk/data.aspx?oid=2305049`

### 10.3 PSRM-referencemateriale (Gældsstyrelsen)

- Generelle krav til fordringer: `https://gaeldst.dk/fordringshaver/find-vejledning/generelle-krav-til-fordringer`
- Fordringer i høring: `https://gaeldst.dk/fordringshaver/find-vejledning/fordringer-i-hoering`
- Tilføj/fjern skyldner: `https://gaeldst.dk/fordringshaver/find-vejledning/tilfoej-eller-fjern-paa-en-fordring`
- Regulering af fordringer: `https://gaeldst.dk/fordringshaver/find-vejledning/regulering-af-fordringer`
- Opskriv fordring: `https://gaeldst.dk/fordringshaver/find-vejledning/regulering-af-fordringer/opskriv-fordring`
- Nedskriv fordring: `https://gaeldst.dk/fordringshaver/find-vejledning/regulering-af-fordringer/nedskriv-fordring`
- Tilbagekald fordring: `https://gaeldst.dk/fordringshaver/find-vejledning/regulering-af-fordringer/tilbagekald-fordring`
- AKR-numre: `https://gaeldst.dk/fordringshaver/find-vejledning/registrering-af-skyldner-uden-et-cpr-eller-cvr-nummer`
- Underretningsmeddelelser: `https://gaeldst.dk/fordringshaver/find-vejledning/underretningsmeddelelser`
- Renteregler: `https://gaeldst.dk/fordringshaver/find-vejledning/renteregler`
- Afstemning: `https://gaeldst.dk/fordringshaver/find-vejledning/afstemning`
- Civilretlige fordringer: `https://gaeldst.dk/fordringshaver/find-vejledning/civilretlige-fordringer`
- I/S og PEF skyldnere: `https://gaeldst.dk/fordringshaver/find-vejledning/is-og-pef-skyldnere`
