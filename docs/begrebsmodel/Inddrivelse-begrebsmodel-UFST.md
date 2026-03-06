# Begrebsmodel for inddrivelsesområdet

## Dokumentoplysninger

| Felt | Indhold |
|---|---|
| Dokumenttitel | Begrebsmodel for inddrivelsesområdet |
| Målgruppe | UFST |
| Dokumenttype | Arbejdsudkast i FDA-stil |
| Version | 0.1 |
| Dato | 2026-03-06 |
| Status | Udkast til videre faglig og juridisk validering |

## 1. Formål

Dette dokument beskriver et første, men forholdsvist detaljeret, udkast til en fælles begrebsmodel for inddrivelsesområdet i dansk offentlig kontekst. Formålet er at etablere et fælles semantisk grundlag for analyse, arkitektur, integration, datamodellering og fremtidig modernisering af systemunderstøttelsen på gælds- og inddrivelsesområdet.

Dokumentet er udarbejdet i en stil, der ligger tæt op ad de fællesoffentlige principper for begrebs- og datamodellering. Fokus er derfor på:

- entydige begreber
- præcise definitioner
- klare relationer mellem begreber
- eksplicitte afgrænsninger
- sporbarhed til juridisk og forvaltningsmæssigt kildegrundlag

Dokumentet er et begrebsmodeludkast og ikke en juridisk fortolkning i sig selv. Det bør derfor anvendes som arbejdsgrundlag og efterfølgende valideres mod gældende lovgivning, Den juridiske vejledning og UFST's egne forretningsregler.

## 2. Metodisk udgangspunkt

### 2.1 FDA-stil og fællesoffentlig begrebsmodellering

Udkastet er struktureret efter tankegangen i de fællesoffentlige regler for begrebs- og datamodellering, dvs. at hvert centralt begreb så vidt muligt beskrives ved hjælp af:

- foretrukken term
- kort definition
- note og afgrænsning
- relationer til andre begreber
- kildegrundlag

Det centrale princip er, at data skal bygge på forretningens begreber og ikke omvendt. Et begreb som `fordring` skal derfor forstås semantisk og juridisk, før det omsættes til tabeller, objekter, felter og integrationer.

### 2.2 Hvad modellen skal bruges til

Begrebsmodellen kan danne grundlag for:

- begrebsafklaring mellem UFST, Gældsstyrelsen og øvrige myndigheder
- afgrænsning mellem forretningsbegreb og teknisk dataobjekt
- udarbejdelse af logisk datamodel
- harmonisering af API-kontrakter og integrationsbegreber
- analyse af forskelle mellem nuværende PSRM-begreber og fremtidige målbegreber
- dokumentation i forbindelse med modernisering, udbud og governance

### 2.3 Modelniveau

Dette dokument ligger på begrebsniveau. Det betyder blandt andet:

- begreber beskrives uden databasefelter og fysisk implementering
- relationer beskrives semantisk før de beskrives teknisk
- hændelser og processer omtales kun i det omfang, de er nødvendige for at forstå begreberne

## 3. Afgrænsning

### 3.1 Omfattet område

Udkastet dækker de centrale begreber i den del af domænet, hvor:

1. et krav på betaling eksisterer
2. en skyldner identificeres
3. fordringshaver kommunikerer kravet og dets forfald
4. restancen eventuelt overdrages til inddrivelse
5. restanceinddrivelsesmyndigheden iværksætter inddrivelsesskridt

### 3.2 Ikke fuldt modelleret i denne version

Følgende områder er kun delvist eller slet ikke modelleret i denne version:

- renter, gebyrer og omkostningsarter som selvstændige begreber
- modregning, lønindeholdelse, udlæg mv. som hver deres særskilte undertyper af inddrivelsesskridt
- klage-, anke- og domstolsforløb
- betalingsordninger og afdragsordninger
- detaljeret håndtering af flere skyldnere, hæftelsesformer og skyldnerskifte
- dataejeransvar og snitflader mellem fordringshaversystemer og inddrivelsessystemer

### 3.3 Sproglig præcision

I dokumentet anvendes som udgangspunkt de forvaltningsnære termer:

- `fordringshaver` frem for generel termen `kreditor`
- `skyldner` frem for generel termen `debitor`
- `restanceinddrivelsesmyndighed` frem for kun organisatoriske navne, medmindre en konkret myndighed menes

## 4. Begrebslandskab

For at gøre domænet overskueligt opdeles begreberne her i fem grupper.

| Gruppe | Beskrivelse | Centrale begreber |
|---|---|---|
| Parter | Aktører eller retssubjekter i domænet | Fordringshaver, Skyldner, Restanceinddrivelsesmyndighed |
| Kravobjekter | Det, der juridisk og forretningsmæssigt behandles | Kravgrundlag, Fordring, Restance |
| Kommunikation | Meddelelser og underretninger om betalingskravet | Underretning, Påkrav, Rykker, Betalingsfrist |
| Reaktioner | Skyldners eller myndigheders reaktioner på kravet | Indsigelse, Overdragelse til inddrivelse |
| Inddrivelse | Den efterfølgende myndighedsmæssige håndtering | Inddrivelsesskridt |

## 5. Begrebskatalog

## 5.1 Kravgrundlag

### Foretrukken term
`Kravgrundlag`

### Definition
Det retlige eller faktiske grundlag, der begrunder, at en fordringshaver kan gøre en fordring gældende mod en skyldner.

### Note
Kravgrundlaget er ikke selve fordringen, men det fundament som fordringen hviler på. Det kan eksempelvis være en afgørelse, en lovbestemt betalingspligt, en opgørelse, en kontrakt eller en anden hjemmelsskabende begivenhed.

### Afgrænsning
Begrebet bør holdes adskilt fra `fordring`. En fordring er selve betalingskravet, mens kravgrundlaget er begrundelsen for, at kravet eksisterer.

### Relaterede begreber
- Et kravgrundlag kan begrunde en eller flere fordringer.
- En fordring bør kunne spores tilbage til et kravgrundlag.

### Kildegrundlag
- Generelt lov- og afgørelsesgrundlag for den enkelte fordringstype
- Fællesoffentlig metode for sporbarhed mellem begreb og retsgrundlag

### Mulige kandidater til senere datamodellering
- type af kravgrundlag
- hjemmel
- afgørelsesdato
- identifikator for afgørelse eller sag

## 5.2 Fordring

### Foretrukken term
`Fordring`

### Definition
Et krav på betaling, som en fordringshaver har mod en skyldner.

### Note
Fordringen er modellens centrale objekt. Den repræsenterer det krav, der kan varsles, rykkes, gøres til restance og eventuelt overdrages til inddrivelse.

### Afgrænsning
Begrebet bør afgrænses fra både `kravgrundlag` og `restance`. En fordring kan eksistere uden at være i restance. Først når betalingsfristen er overskredet uden betaling, opstår der en restance.

### Relaterede begreber
- En fordring har et kravgrundlag.
- En fordring tilhører eller administreres af en fordringshaver.
- En fordring er rettet mod en eller flere skyldnere.
- En fordring kan være omfattet af påkrav, rykker, indsigelse og inddrivelsesskridt.

### Kildegrundlag
- Lov om opkrævning og inddrivelse af visse fordringer
- Lov om inddrivelse af gæld til det offentlige
- Den juridiske vejledning, G.A. Inddrivelse

### Mulige kandidater til senere datamodellering
- fordringsidentifikator
- hovedstol
- stiftelsesdato
- forfaldsdato
- status
- ansvarlig fordringshaver

## 5.3 Restance

### Foretrukken term
`Restance`

### Definition
En fordring, som ikke er betalt rettidigt efter udløb af betalingsfristen.

### Note
Restance er en tilstand eller kvalificering af en fordring, ikke nødvendigvis et helt selvstændigt retsobjekt. I en begrebsmodel er det dog ofte nyttigt at modellere `restance` eksplicit, fordi store dele af inddrivelsesdomænet netop aktiveres, når en fordring bliver forfalden og ubetalt.

### Afgrænsning
Det bør afklares i næste modeliteration, om `restance` skal forstås som:

- en selvstændig begrebsenhed, eller
- en tilstand på begrebet `fordring`

I dette udkast modelleres restance som et selvstændigt begreb af hensyn til klarhed og sporbarhed i forretningen.

### Relaterede begreber
- En restance udspringer af en fordring.
- En restance kan overdrages til inddrivelse.
- En restance kan være genstand for inddrivelsesskridt.

### Kildegrundlag
- Den juridiske vejledning, G.A. Inddrivelse
- offentlig forvaltningspraksis om overdragelse til inddrivelse

### Mulige kandidater til senere datamodellering
- restancebeløb
- restancedato
- restanceårsag
- status for overdragelse

## 5.4 Fordringshaver

### Foretrukken term
`Fordringshaver`

### Definition
Den myndighed eller anden berettigede part, som har en fordring mod en skyldner og som kan fremsende kravet, foretage den sædvanlige rykkerprocedure og overdrage fordringen til inddrivelse.

### Note
I offentlig kontekst er fordringshaver typisk en myndighed eller institution, der administrerer kravet og har ansvar for, at det er korrekt etableret, opgjort og behandlet før overdragelse til inddrivelse.

### Afgrænsning
Begrebet bør afgrænses fra `restanceinddrivelsesmyndighed`. Fordringshaver ejer eller administrerer kravet før overdragelse, mens restanceinddrivelsesmyndigheden håndterer selve inddrivelsen efter de regler, der gælder herfor.

### Relaterede begreber
- En fordringshaver har en eller flere fordringer.
- En fordringshaver fremsender underretning, påkrav og rykker.
- En fordringshaver foretager overdragelse til inddrivelse.

### Kildegrundlag
- Den juridiske vejledning, G.A.1.3 Fordringshaver
- Den juridiske vejledning, G.A.1.3.2 Overdragelse af fordringer til inddrivelse, sædvanlig rykkerprocedure og fordringshavers underretningsforpligtelser

### Mulige kandidater til senere datamodellering
- myndighedsidentitet
- organisatorisk enhed
- fordringshavertype
- kontaktpunkt

## 5.5 Skyldner

### Foretrukken term
`Skyldner`

### Definition
Person eller juridisk enhed, som er forpligtet til at betale en fordring.

### Note
Skyldner er den part, som betalingskravet rettes mod, og som modtager underretning, påkrav, rykker og eventuelle senere inddrivelsesskridt.

### Afgrænsning
Begrebet bør ikke begrænses til fysiske personer. I mange sammenhænge kan skyldner også være virksomhed, forening eller anden juridisk enhed. Det bør samtidig afklares, hvordan medskyldnere, solidarisk hæftelse og skyldnerskifte skal håndteres.

### Relaterede begreber
- En skyldner hæfter for en eller flere fordringer.
- En skyldner modtager meddelelser vedrørende en fordring.
- En skyldner kan fremsætte indsigelse.

### Kildegrundlag
- Den juridiske vejledning, G.A. Inddrivelse
- Den juridiske vejledning, G.A.1.3.3 Flere skyldnere og skyldnerskifte

### Mulige kandidater til senere datamodellering
- person- eller virksomhedsidentifikator
- navn
- adresse
- status som fysisk eller juridisk person

## 5.6 Underretning

### Foretrukken term
`Underretning`

### Definition
Meddelelse til skyldner om forhold vedrørende en fordring, dens behandling eller dens videre håndtering.

### Note
Underretning er et bredere kommunikationsbegreb end både påkrav og rykker. Det kan anvendes som fælles overbegreb for en række formelle meddelelser til skyldner.

### Afgrænsning
Det bør afklares, om `underretning` i målarkitekturen skal være et selvstændigt begreb eller alene fungere som fælles klasse/overbegreb for `påkrav`, `rykker` og øvrige meddelelser.

### Relaterede begreber
- En fordringshaver giver underretning til en skyldner.
- Et påkrav kan opfattes som en særlig type underretning.
- En rykker kan opfattes som en særlig type underretning.

### Kildegrundlag
- Den juridiske vejledning, navnlig om fordringshavers underretningsforpligtelser

### Mulige kandidater til senere datamodellering
- underretningstype
- afsendelsesdato
- kanal
- modtagelsesstatus

## 5.7 Påkrav

### Foretrukken term
`Påkrav`

### Definition
En formel anmodning til skyldner om at opfylde en betalingsforpligtelse inden en angivet frist.

### Note
Påkravet tydeliggør, at der består et krav, at betaling forventes, og at manglende betaling kan få konsekvenser. I en begrebsmodel er det centralt som forbindelsespunkt mellem fordring, kommunikation og den senere restancestatus.

### Afgrænsning
Påkrav bør holdes adskilt fra `rykker`, selv om de i praksis kan ligge tæt op ad hinanden. Påkravet kan beskrives som den formelle betalingsopfordring, mens rykker typisk anvendes efter manglende betaling inden den ordinære frist.

### Relaterede begreber
- Et påkrav vedrører en fordring.
- Et påkrav rettes mod en skyldner.
- Et påkrav indeholder eller refererer til en betalingsfrist.

### Kildegrundlag
- almindelige påkravsregler og offentlig inddrivelsespraksis
- Den juridiske vejledning i relation til underretning og rykkerprocedure

### Mulige kandidater til senere datamodellering
- påkravsdato
- fristdato
- beløb
- henvisning til fordring

## 5.8 Rykker

### Foretrukken term
`Rykker`

### Definition
En meddelelse til skyldner om, at betaling ikke er sket rettidigt, og at yderligere handling kan følge ved fortsat manglende betaling.

### Note
Rykker er et centralt begreb i overgangen mellem almindelig kravadministration og egentlig inddrivelse. Den sædvanlige rykkerprocedure er vigtig for vurderingen af, hvornår en fordring kan overdrages.

### Afgrænsning
Rykker bør ikke forveksles med selve inddrivelsen. Den ligger typisk forud for overdragelse til restanceinddrivelsesmyndigheden.

### Relaterede begreber
- En rykker vedrører en fordring.
- En rykker fremsendes af fordringshaver til skyldner.
- En rykker kan indgå i den dokumentation, der ligger forud for overdragelse til inddrivelse.

### Kildegrundlag
- Den juridiske vejledning, G.A.1.3.2 om sædvanlig rykkerprocedure

### Mulige kandidater til senere datamodellering
- rykkertrin
- rykkerdato
- fristdato
- rykkergebyrindikator

## 5.9 Betalingsfrist

### Foretrukken term
`Betalingsfrist`

### Definition
Det tidspunkt eller den periode, inden for hvilken en fordring skal være betalt for at undgå misligholdelse og mulig restancestatus.

### Note
Betalingsfristen er central for skiftet mellem `fordring` og `restance`. Den er derfor et styrende begreb i domænet, selv om den ofte realiseres som et attributlignende forhold i systemer.

### Afgrænsning
I den videre modellering skal det afklares, om `betalingsfrist` skal stå som selvstændigt begreb eller som egenskab ved `fordring`, `påkrav` og `rykker`. I dette udkast er den modelleret som selvstændigt begreb, fordi den spiller en vigtig semantisk rolle i procesforløbet.

### Relaterede begreber
- En fordring har en betalingsfrist.
- Et påkrav og en rykker kan referere til en betalingsfrist.
- Udløb af betalingsfrist kan føre til restance.

### Kildegrundlag
- almindelige regler om forfald og betaling
- Den juridiske vejledning i relation til rykkerprocedure og overdragelse

### Mulige kandidater til senere datamodellering
- fristdato
- fristtype
- beregningsgrundlag

## 5.10 Indsigelse

### Foretrukken term
`Indsigelse`

### Definition
Skyldners bestridelse af fordringens eksistens, størrelse, grundlag eller behandlingen af den.

### Note
Indsigelse er et afgørende begreb, fordi den kan have direkte betydning for, om en fordring kan eller bør fortsætte i den ordinære proces eller overdrages til inddrivelse.

### Afgrænsning
Begrebet bør adskilles fra formelle klager og domstolsprøvelse. Indsigelse bruges her som det generelle forretningsbegreb for skyldners bestridelse af kravet.

### Relaterede begreber
- En skyldner fremsætter indsigelse.
- En indsigelse vedrører en fordring.
- En indsigelse kan nødvendiggøre behandling hos fordringshaver før videre inddrivelse.

### Kildegrundlag
- Den juridiske vejledning, G.A.1.3.1 Indsigelser om kravets eksistens og størrelse

### Mulige kandidater til senere datamodellering
- indsigelsestype
- modtagelsesdato
- status
- afgørelsesresultat

## 5.11 Overdragelse til inddrivelse

### Foretrukken term
`Overdragelse til inddrivelse`

### Definition
Den handling, hvorved fordringshaver sender en restance eller fordring til restanceinddrivelsesmyndigheden med henblik på inddrivelse.

### Note
Overdragelse er både en forretningsmæssig og systemmæssig grænseflade. Den markerer overgangen mellem fordringshavers kravadministration og den efterfølgende offentlige inddrivelse.

### Afgrænsning
Begrebet bør adskilles fra selve `inddrivelsesskridt`. Overdragelse er ikke inddrivelse, men en forudsætning for at inddrivelse kan iværksættes i myndighedsregi.

### Relaterede begreber
- Overdragelse foretages af fordringshaver.
- Overdragelse omfatter en restance eller fordring.
- Overdragelse sker til restanceinddrivelsesmyndigheden.

### Kildegrundlag
- Den juridiske vejledning, G.A.1.3.2 om overdragelse af fordringer til inddrivelse

### Mulige kandidater til senere datamodellering
- overdragelsesdato
- overdragelsesstatus
- overdragelsespakke eller hændelsesidentifikator

## 5.12 Restanceinddrivelsesmyndighed

### Foretrukken term
`Restanceinddrivelsesmyndighed`

### Definition
Den myndighed, der efter overdragelse forestår inddrivelse af offentlige fordringer efter gældende regler.

### Note
Begrebet bør fastholdes som et generisk forvaltningsbegreb, selv om det i praksis ofte vil være Gældsstyrelsen. Det gør modellen mere robust over for organisatoriske ændringer.

### Afgrænsning
Begrebet skal holdes adskilt fra `fordringshaver`, da de to roller har forskellige ansvar, hjemler og opgaver, selv om de indgår i samme samlede domæne.

### Relaterede begreber
- Restanceinddrivelsesmyndigheden modtager overdragelser.
- Restanceinddrivelsesmyndigheden iværksætter inddrivelsesskridt.

### Kildegrundlag
- lovgivning og vejledning om inddrivelse af gæld til det offentlige

### Mulige kandidater til senere datamodellering
- myndighedsidentitet
- organisatorisk enhed
- systemreference

## 5.13 Inddrivelsesskridt

### Foretrukken term
`Inddrivelsesskridt`

### Definition
Et retligt eller administrativt skridt, der iværksættes med henblik på at opnå betaling af en overdraget fordring eller restance.

### Note
Begrebet fungerer som et paraplybegreb for flere mere specifikke virkemidler. I en senere version bør det sandsynligvis specialiseres i undertyper såsom modregning, lønindeholdelse, udlæg og andre relevante inddrivelsesformer.

### Afgrænsning
Denne version beskriver kun det generelle begreb og ikke de enkelte virkemidler i detaljer.

### Relaterede begreber
- Inddrivelsesskridt iværksættes af restanceinddrivelsesmyndigheden.
- Inddrivelsesskridt vedrører en fordring eller restance.

### Kildegrundlag
- lovgivning og vejledning om offentlig inddrivelse

### Mulige kandidater til senere datamodellering
- skridttype
- iværksættelsesdato
- retsvirkning
- status

## 6. Begrebsrelationer

Nedenstående relationer er formuleret i semantisk form og bør ses som kandidatgrundlag for en senere logisk informationsmodel.

| Nr. | Kildebegreb | Relation | Målbegreb | Foreløbig kardinalitet | Bemærkning |
|---|---|---|---|---|---|
| 1 | Fordringshaver | har | Fordring | 1 til 0..* | En fordringshaver kan have mange fordringer. |
| 2 | Fordring | er begrundet i | Kravgrundlag | 1 til 1..* | En fordring bør kunne føres tilbage til mindst ét kravgrundlag. |
| 3 | Fordring | er rettet mod | Skyldner | 1 til 1..* | En fordring kan i særlige tilfælde vedrøre flere skyldnere. |
| 4 | Fordringshaver | giver | Underretning | 1 til 0..* | Underretning er et overordnet kommunikationsbegreb. |
| 5 | Underretning | tilgår | Skyldner | 1 til 1..* | En underretning kan have en eller flere modtagere. |
| 6 | Fordringshaver | fremsender | Påkrav | 1 til 0..* | Påkrav er en særlig kommunikationshandling. |
| 7 | Påkrav | vedrører | Fordring | 1 til 1 | Påkravet knytter sig til et konkret krav. |
| 8 | Påkrav | rettes mod | Skyldner | 1 til 1..* | Påkravet sendes til den eller de hæftende skyldnere. |
| 9 | Fordringshaver | fremsender | Rykker | 1 til 0..* | Rykker indgår i den sædvanlige rykkerprocedure. |
| 10 | Rykker | vedrører | Fordring | 1 til 1 | Rykker udspringer af manglende betaling af en konkret fordring. |
| 11 | Fordring | har | Betalingsfrist | 1 til 1 | Fordringen har en gældende betalingsfrist. |
| 12 | Fordring | kan overgå til | Restance | 1 til 0..1 | Det sker ved manglende rettidig betaling. |
| 13 | Skyldner | fremsætter | Indsigelse | 1 til 0..* | Skyldner kan bestride kravet. |
| 14 | Indsigelse | vedrører | Fordring | 1 til 1 | Indsigelsen relaterer sig til et konkret krav. |
| 15 | Fordringshaver | foretager | Overdragelse til inddrivelse | 1 til 0..* | Overdragelse er en handling udført af fordringshaver. |
| 16 | Overdragelse til inddrivelse | omfatter | Restance | 1 til 1..* | Det er normalt restance, der overdrages. |
| 17 | Overdragelse til inddrivelse | sker til | Restanceinddrivelsesmyndighed | 1 til 1 | Modtageren er den kompetente myndighed. |
| 18 | Restanceinddrivelsesmyndighed | iværksætter | Inddrivelsesskridt | 1 til 0..* | Myndigheden kan gennemføre flere skridt. |
| 19 | Inddrivelsesskridt | vedrører | Fordring | 1 til 1..* | Skridtet relaterer sig til den overdragne fordring eller restance. |

## 7. Begrebsmæssige hovedregler

Som hjælp til senere arkitektur- og datamodellering kan følgende hovedregler anvendes som foreløbige semantiske regler:

1. En `fordring` kan ikke forstås korrekt uden et identificerbart `kravgrundlag`.
2. En `fordring` er ikke nødvendigvis en `restance`.
3. `Restance` opstår først, når betalingsfristen er overskredet uden korrekt betaling.
4. `Påkrav` og `rykker` er kommunikationsbegreber, ikke inddrivelsesskridt.
5. `Indsigelse` skal holdes adskilt fra almindelig betalingsmisligholdelse, fordi indsigelse kan påvirke lovligheden og timingen af videre proces.
6. `Overdragelse til inddrivelse` markerer et domæneskift mellem fordringshaver og restanceinddrivelsesmyndighed.
7. `Restanceinddrivelsesmyndighed` og `fordringshaver` er roller med forskellige ansvar, også når de organisatorisk ligger inden for samme samlede statslige økosystem.

## 8. Anbefalet begrebsdiagram

Det tilhørende draw.io-diagram viser den mest centrale struktur i modellen:

- `Fordringshaver` har `Fordring`
- `Fordring` er begrundet i `Kravgrundlag`
- `Fordring` er rettet mod `Skyldner`
- `Fordringshaver` kommunikerer via `Underretning`, `Påkrav` og `Rykker`
- `Fordring` har `Betalingsfrist` og kan overgå til `Restance`
- `Skyldner` kan fremsætte `Indsigelse`
- `Restance` kan indgå i `Overdragelse til inddrivelse`
- `Restanceinddrivelsesmyndighed` kan iværksætte `Inddrivelsesskridt`

Diagrammet er holdt på begrebsniveau og er derfor bevidst uden tekniske felter, systemnavne, tabeller og integrationstekniske detaljer.

## 9. Åbne afklaringer til næste iteration

For at løfte modellen fra godt arbejdsudkast til styringsklar begrebsmodel anbefales det, at UFST afklarer følgende:

### 9.1 Skal `restance` være begreb eller status?

Der er gode argumenter for begge modeller. Hvis domænet og forretningen ofte taler om restancer som selvstændige objekter, bør begrebet sandsynligvis bevares eksplicit. Hvis fokus i højere grad er datamæssig enkelhed, kan restance alternativt gøres til en tilstand på `fordring`.

### 9.2 Skal kommunikationsbegreber arves fra et fælles overbegreb?

Det bør afklares, om `underretning` skal fungere som overbegreb for `påkrav`, `rykker` og andre meddelelser, eller om begreberne skal stå sideordnet uden egentlig generalisering.

### 9.3 Skal inddrivelsesskridt opdeles i undertyper?

Til arkitekturanalyse og systemmodernisering vil det ofte være nødvendigt at specialisere `inddrivelsesskridt` i konkrete undertyper. Det anbefales at gøre dette i næste version af modellen.

### 9.4 Skal flere skyldnere modelleres eksplicit?

Hvis hæftelsesformer har stor forretningsmæssig betydning, bør der indføres særskilte begreber for eksempelvis hæftelse, medskyldner og skyldnerskifte i næste iteration.

### 9.5 Skal beløbskomponenter skilles ud?

Hvis moderniseringsarbejdet kræver præcis styring af hovedstol, renter, gebyrer og omkostninger, bør disse eventuelt modelleres som særskilte begreber eller komponenter under `fordring`.

## 10. Anbefalet næste skridt for UFST

Det anbefales, at dette udkast anvendes som version 0 af et fælles begrebskatalog og herefter modnes i fire korte trin:

1. **Juridisk validering** mod gældende lovtekst og Den juridiske vejledning.
2. **Forretningsvalidering** med domæneejere fra Gældsstyrelsen og relevante fordringshavere.
3. **Arkitekturvalidering** mod eksisterende systembegreber i PSRM og ønskede målbegreber.
4. **Omsætning til logisk model** med informationsobjekter, attributter, kodelister og hændelser.

## 11. Kildehenvisninger

Nedenstående kilder er relevante som fagligt og metodisk grundlag for den videre modning af modellen.

### 11.1 Metode

- DIGST, de fællesoffentlige regler for begrebs- og datamodellering:  
  `https://arkitektur.digst.dk/metoder/begrebs-og-datametoder/regler-begrebs-og-datamodellering`

### 11.2 Juridisk vejledning og domænekilder

- Den juridiske vejledning, G.A. Inddrivelse:  
  `https://info.skat.dk/data.aspx?oid=9672`
- Den juridiske vejledning, G.A.1.3 Fordringshaver:  
  `https://info.skat.dk/data.aspx?oid=2304023`
- Den juridiske vejledning, G.A.1.3.1 Indsigelser om kravets eksistens og størrelse:  
  `https://info.skat.dk/data.aspx?oid=2304024`
- Den juridiske vejledning, G.A.1.3.2 Overdragelse af fordringer til inddrivelse, sædvanlig rykkerprocedure og fordringshavers underretningsforpligtelser:  
  `https://info.skat.dk/data.aspx?oid=2304026`
- Den juridiske vejledning, G.A.1.3.3 Flere skyldnere og skyldnerskifte:  
  `https://info.skat.dk/data.aspx?oid=2305049`

### 11.3 Lovgrundlag

- Lov om opkrævning og inddrivelse af visse fordringer
- Lov om inddrivelse af gæld til det offentlige

## 12. Samlet vurdering

Dette udkast etablerer et forholdsvis robust begrebsmæssigt fundament for inddrivelsesområdet i en form, som er anvendelig i UFST's videre analyse- og moderniseringsarbejde. Den vigtigste styrke ved modellen er, at den tydeligt skelner mellem:

- kravets eksistens (`kravgrundlag` og `fordring`)
- kravets tilstand (`restance`)
- kommunikationen med skyldner (`underretning`, `påkrav`, `rykker`)
- bestridelse af kravet (`indsigelse`)
- organisatorisk og processuel overgang (`overdragelse til inddrivelse`)
- den efterfølgende myndighedsudøvelse (`restanceinddrivelsesmyndighed` og `inddrivelsesskridt`)

Dermed giver modellen et egnet udgangspunkt for både begrebsharmonisering, fremtidig informationsmodellering og målarkitektur på området.
