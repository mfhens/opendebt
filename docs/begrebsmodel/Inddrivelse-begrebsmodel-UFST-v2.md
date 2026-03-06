# Begrebsmodel for inddrivelsesområdet – version 2

## Dokumentoplysninger

| Felt | Indhold |
|---|---|
| Dokumenttitel | Begrebsmodel for inddrivelsesområdet – version 2 |
| Målgruppe | UFST |
| Dokumenttype | Arbejdsudkast i FDA-stil, formel version |
| Version | 2.0 |
| Dato | 2026-03-06 |
| Status | Udkast til faglig, juridisk og arkitekturmæssig validering |
| Bygger på | `Inddrivelse-begrebsmodel-UFST.md` |

## 1. Formål

Denne version 2 viderefører den første begrebsmodel og gør den mere formel med henblik på anvendelse i UFST's arkitekturarbejde, moderniseringsanalyser og senere logisk datamodellering.

Den vigtigste forskel fra version 1 er, at modellen nu eksplicit anvender:

- **generaliseringsrelationer** for overbegreber og specialiseringer
- **undertyper af inddrivelsesskridt**
- **eksplicit modellering af flere skyldnere og hæftelse**
- en tydeligere skelnen mellem **abstrakte begreber**, **konkrete begreber** og **associationsbegreber**

Version 2 er stadig en begrebsmodel og ikke en fysisk eller logisk datamodel. Den er derfor semantisk og forretningsorienteret, men i en form der gør næste overgang til informationsmodel væsentligt lettere.

## 2. Modelleringsprincipper i version 2

### 2.1 Grundlæggende princip

Modellen følger de fællesoffentlige principper om, at data og systemer skal tage udgangspunkt i forretningens begreber. Derfor beskrives først de begrebsmæssige enheder og deres relationer, før eventuel omsætning til felter, tabeller, API-ressourcer og hændelser.

### 2.2 Abstrakte og konkrete begreber

I version 2 anvendes både abstrakte og konkrete begreber:

- **abstrakt begreb**: et begreb der ikke nødvendigvis forekommer alene i forretningen, men som samler fælles egenskaber og relationer
- **konkret begreb**: et begreb der forekommer direkte i forretningspraksis og kan identificeres som selvstændigt domæneobjekt

Følgende abstrakte begreber introduceres i denne version:

- `Part`
- `Underretning`
- `Inddrivelsesskridt`

### 2.3 Generalisering og specialisering

Version 2 anvender specialisering, når et underbegreb:

1. arver centrale egenskaber fra et overbegreb
2. samtidig har en mere snæver betydning
3. kræver særskilte regler, relationer eller forretningsmæssig behandling

Det gælder især:

- `Fordringshaver`, `Skyldner` og `Restanceinddrivelsesmyndighed` som specialiseringer af `Part`
- `Påkrav` og `Rykker` som specialiseringer af `Underretning`
- `Restance` som specialisering af `Fordring`
- `Modregning`, `Lønindeholdelse` og `Udlæg` som specialiseringer af `Inddrivelsesskridt`

### 2.4 Associationsbegreber

Når en relation mellem to begreber selv har vigtig forretningsmæssig betydning, modelleres relationen som et selvstændigt begreb. Det gælder i denne version især `Hæftelse`, fordi forholdet mellem `Skyldner` og `Fordring` ikke altid kan beskrives tilstrækkeligt med en simpel linje.

`Hæftelse` gør det muligt at beskrive:

- om én eller flere skyldnere hæfter for samme fordring
- hvilken type hæftelse der gælder
- om hæftelsen er solidarisk, delt eller eksklusiv

## 3. Overordnet begrebsstruktur

Version 2 organiserer domænet i følgende hovedområder:

| Område | Formål | Centrale begreber |
|---|---|---|
| Aktører og roller | Identificerer domænets parter | Part, Fordringshaver, Skyldner, Restanceinddrivelsesmyndighed |
| Krav og status | Beskriver betalingskravet og dets udvikling | Kravgrundlag, Fordring, Restance, Betalingsfrist |
| Kommunikation | Beskriver formelle meddelelser til skyldner | Underretning, Påkrav, Rykker |
| Reaktion og tvist | Beskriver bestridelse og hæftelsesforhold | Indsigelse, Hæftelse |
| Myndighedsudøvelse | Beskriver overdragelse og efterfølgende inddrivelse | Overdragelse til inddrivelse, Inddrivelsesskridt, Modregning, Lønindeholdelse, Udlæg |

## 4. Formelle modelleringsvalg i version 2

### 4.1 `Part` som abstrakt overbegreb

I version 1 stod `fordringshaver`, `skyldner` og `restanceinddrivelsesmyndighed` som sideordnede begreber. I version 2 samles de under det abstrakte overbegreb `Part`.

Det giver følgende fordele:

- fælles sted at forankre identitet, kontaktoplysninger og rollebaseret kommunikation
- mulighed for at samme organisatoriske enhed i forskellige sammenhænge kan have forskellige roller
- mere formel og genbrugelig model ved overgang til informationsarkitektur

### 4.2 `Restance` som specialisering af `Fordring`

I version 1 var det åbent, om `restance` skulle forstås som selvstændigt begreb eller som status på `fordring`. I version 2 vælges en mellemvej: `Restance` modelleres som **specialisering af `Fordring`**.

Det betyder begrebsmæssigt:

- alle restancer er fordringer
- ikke alle fordringer er restancer
- restancer arver fordringens grundlæggende semantik, men får egen betydning i relation til overdragelse og inddrivelse

Dette valg er ofte nyttigt i formelle begrebsmodeller, fordi det både bevarer den semantiske sammenhæng og gør særskilte regler tydelige.

### 4.3 `Underretning` som overbegreb

Version 2 fastlægger, at `Underretning` er et abstrakt overbegreb for formelle meddelelser til skyldner. `Påkrav` og `Rykker` modelleres som specialiseringer.

Det giver en mere stringent model for:

- afsendelse og modtagelse
- kanalvalg og notifikationslogik
- fælles dokumentationskrav

### 4.4 `Hæftelse` som selvstændigt begreb

I version 2 indføres `Hæftelse` som et selvstændigt associationsbegreb mellem `Fordring` og `Skyldner`.

Dette er nødvendigt, fordi en simpel relation `fordring er rettet mod skyldner` ikke er tilstrækkelig, når:

- der er flere skyldnere
- hæftelsen ikke er ens for alle skyldnere
- der skal skelnes mellem solidarisk hæftelse og delt hæftelse

### 4.5 Specialisering af `Inddrivelsesskridt`

I version 1 var `inddrivelsesskridt` et generelt paraplybegreb. I version 2 opdeles det i undertyper, fordi de forskellige skridt har forskellige retlige forudsætninger, virkninger og databehov.

De første undertyper i denne version er:

- `Modregning`
- `Lønindeholdelse`
- `Udlæg`

Disse er valgt, fordi de er begrebsmæssigt tydelige og typisk centrale i offentlig inddrivelsespraksis.

## 5. Begrebskatalog

## 5.1 Part

### Foretrukken term
`Part`

### Definition
Abstrakt aktørbegreb, der dækker en identificerbar enhed, som kan indgå i relationer om fordringer, kommunikation og inddrivelse.

### Note
`Part` anvendes som fælles overbegreb og forventes normalt ikke at forekomme alene i forretningen. Det bruges for at skabe en mere konsistent formel model.

### Specialiseringer
- `Fordringshaver`
- `Skyldner`
- `Restanceinddrivelsesmyndighed`

## 5.2 Fordringshaver

### Definition
Part, der har eller administrerer en fordring mod en skyldner og som kan fremsende påkrav, gennemføre rykkerprocedure og foretage overdragelse til inddrivelse.

### Specialisering af
`Part`

## 5.3 Skyldner

### Definition
Part, der hæfter helt eller delvist for en fordring og som kan modtage underretning og fremsætte indsigelse.

### Specialisering af
`Part`

## 5.4 Restanceinddrivelsesmyndighed

### Definition
Part, der efter overdragelse forestår inddrivelse af offentlige fordringer efter gældende regler.

### Specialisering af
`Part`

## 5.5 Kravgrundlag

### Definition
Det retlige eller faktiske grundlag, der begrunder en fordring.

### Note
Kravgrundlaget kan eksempelvis være afgørelse, hjemmel, beregning eller anden retsskabende begivenhed.

## 5.6 Fordring

### Definition
Et krav på betaling, som en fordringshaver har mod en eller flere skyldnere.

### Note
I version 2 kobles relationen til skyldner ikke direkte, men via `Hæftelse`, fordi denne løsning er mere præcis ved flere skyldnere.

## 5.7 Restance

### Definition
Fordring, hvis betalingsfrist er overskredet uden rettidig betaling.

### Specialisering af
`Fordring`

### Note
`Restance` er den specialisering af fordring, som kan danne genstand for overdragelse til inddrivelse og efterfølgende inddrivelsesskridt.

## 5.8 Betalingsfrist

### Definition
Det tidspunkt eller den periode, inden for hvilken fordringen skal være betalt.

### Note
Betalingsfrist er fortsat et vigtigt styrende begreb. I en senere informationsmodel kan det vise sig mest hensigtsmæssigt at realisere det som egenskab frem for selvstændigt objekt.

## 5.9 Underretning

### Definition
Abstrakt begreb for formel meddelelse til skyldner om en fordring, dens status eller dens behandling.

### Specialiseringer
- `Påkrav`
- `Rykker`

## 5.10 Påkrav

### Definition
Underretning, der formelt anmoder skyldner om at opfylde betalingsforpligtelsen inden en angivet frist.

### Specialisering af
`Underretning`

## 5.11 Rykker

### Definition
Underretning, der meddeler, at betaling ikke er sket rettidigt, og at yderligere handling kan følge.

### Specialisering af
`Underretning`

## 5.12 Indsigelse

### Definition
Skyldners bestridelse af fordringens eksistens, størrelse, grundlag eller behandling.

### Note
Indsigelse er ikke en underretning, men en reaktion på fordringen eller dens behandling.

## 5.13 Hæftelse

### Definition
Det retlige og forretningsmæssige forhold, der knytter en skyldner til en fordring og beskriver arten og omfanget af betalingsansvaret.

### Note
`Hæftelse` er centralt i version 2, fordi det gør modellen egnet til sager med flere skyldnere.

### Specialiseringer
- `Enehæftelse`
- `Solidarisk hæftelse`
- `Delt hæftelse`

## 5.14 Enehæftelse

### Definition
Hæftelse, hvor en fordring kun er knyttet til én skyldner som ansvarlig part.

### Specialisering af
`Hæftelse`

## 5.15 Solidarisk hæftelse

### Definition
Hæftelse, hvor flere skyldnere hæfter for samme fordring på en måde, så kravet kan gøres gældende efter de regler, der gælder for solidarisk ansvar.

### Specialisering af
`Hæftelse`

## 5.16 Delt hæftelse

### Definition
Hæftelse, hvor flere skyldnere hæfter for adskilte eller fordelte andele af samme fordring eller kravkompleks.

### Specialisering af
`Hæftelse`

## 5.17 Overdragelse til inddrivelse

### Definition
Den handling, hvorved fordringshaver sender en restance til restanceinddrivelsesmyndigheden med henblik på videre inddrivelse.

### Note
I version 2 knyttes overdragelse eksplicit til `Restance` frem for generelt til `Fordring`, fordi det giver en mere præcis og formel model.

## 5.18 Inddrivelsesskridt

### Definition
Abstrakt begreb for retlige eller administrative skridt, der iværksættes for at opnå betaling af en overdraget restance.

### Specialiseringer
- `Modregning`
- `Lønindeholdelse`
- `Udlæg`

## 5.19 Modregning

### Definition
Inddrivelsesskridt, hvor et offentligt tilgodehavende søges dækket gennem modregning i et modgående krav eller beløb efter gældende regler.

### Specialisering af
`Inddrivelsesskridt`

## 5.20 Lønindeholdelse

### Definition
Inddrivelsesskridt, hvor betaling søges opnået gennem indeholdelse i skyldners løn eller anden indkomst efter gældende regler.

### Specialisering af
`Inddrivelsesskridt`

## 5.21 Udlæg

### Definition
Inddrivelsesskridt, hvor der foretages udlæg efter de regler, der gælder for tvangsfuldbyrdelse og offentlig inddrivelse.

### Specialisering af
`Inddrivelsesskridt`

## 6. Formelle relationer

## 6.1 Associationsrelationer

| Nr. | Kildebegreb | Relation | Målbegreb | Kardinalitet | Bemærkning |
|---|---|---|---|---|---|
| 1 | Fordringshaver | har | Fordring | 1 til 0..* | En fordringshaver kan have mange fordringer. |
| 2 | Fordring | er begrundet i | Kravgrundlag | 1 til 1..* | En fordring skal kunne spores til kravgrundlag. |
| 3 | Fordring | har | Betalingsfrist | 1 til 1 | En gældende fordring har en betalingsfrist. |
| 4 | Hæftelse | vedrører | Fordring | 1 til 1 | Hver hæftelse knytter sig til én fordring. |
| 5 | Hæftelse | vedrører | Skyldner | 1 til 1 | Hver hæftelse knytter sig til én skyldner. |
| 6 | Fordringshaver | giver | Underretning | 1 til 0..* | Underretning udsendes af fordringshaver. |
| 7 | Underretning | tilgår | Skyldner | 1 til 1..* | Underretning kan have flere modtagere. |
| 8 | Underretning | vedrører | Fordring | 1 til 1 | En underretning relaterer sig til et konkret krav. |
| 9 | Skyldner | fremsætter | Indsigelse | 1 til 0..* | Skyldner kan bestride kravet. |
| 10 | Indsigelse | vedrører | Fordring | 1 til 1 | Indsigelse relaterer sig til en konkret fordring. |
| 11 | Fordringshaver | foretager | Overdragelse til inddrivelse | 1 til 0..* | Overdragelse er en handling hos fordringshaver. |
| 12 | Overdragelse til inddrivelse | omfatter | Restance | 1 til 1..* | I version 2 knyttes overdragelse til restance. |
| 13 | Overdragelse til inddrivelse | sker til | Restanceinddrivelsesmyndighed | 1 til 1 | Modtageren er kompetent myndighed. |
| 14 | Restanceinddrivelsesmyndighed | iværksætter | Inddrivelsesskridt | 1 til 0..* | Myndigheden kan iværksætte flere skridt. |
| 15 | Inddrivelsesskridt | vedrører | Restance | 1 til 1..* | Skridtet vedrører overdraget restance. |

## 6.2 Generaliseringsrelationer

| Overbegreb | Underbegreb | Bemærkning |
|---|---|---|
| Part | Fordringshaver | Rolle som kravbærende eller kravadministrerende part |
| Part | Skyldner | Rolle som hæftende part |
| Part | Restanceinddrivelsesmyndighed | Rolle som inddrivende myndighed |
| Underretning | Påkrav | Særlig type formel betalingsopfordring |
| Underretning | Rykker | Særlig type meddelelse efter manglende betaling |
| Fordring | Restance | Specialisering ved overskredet betalingsfrist |
| Hæftelse | Enehæftelse | Kun én ansvarlig skyldner |
| Hæftelse | Solidarisk hæftelse | Flere skyldnere med solidarisk ansvar |
| Hæftelse | Delt hæftelse | Flere skyldnere med opdelt ansvar |
| Inddrivelsesskridt | Modregning | Særlig type inddrivelsesskridt |
| Inddrivelsesskridt | Lønindeholdelse | Særlig type inddrivelsesskridt |
| Inddrivelsesskridt | Udlæg | Særlig type inddrivelsesskridt |

## 7. Fortolkning af flere skyldnere og hæftelse

Version 2 gør det muligt at skelne mellem forskellige tilfælde, som ellers let bliver sammenblandet:

### 7.1 Én fordring med én skyldner

Dette modelleres som:

- én `Fordring`
- én `Skyldner`
- én `Enehæftelse`

### 7.2 Én fordring med flere solidarisk hæftende skyldnere

Dette modelleres som:

- én `Fordring`
- flere `Skyldner`
- én `Solidarisk hæftelse` pr. skyldnerrelation eller en samlet hæftelsesstruktur, afhængigt af senere logisk modelvalg

### 7.3 Én fordring med flere skyldnere med delt ansvar

Dette modelleres som:

- én `Fordring`
- flere `Skyldner`
- flere forekomster af `Delt hæftelse`

I den videre logiske modellering bør det afklares, om andele, procentfordeling og beløbsfordeling skal modelleres som attributter på `Hæftelse`.

## 8. Semantiske hovedregler i version 2

1. Alle `Restancer` er `Fordringer`, men ikke alle `Fordringer` er `Restancer`.
2. Relation mellem `Fordring` og `Skyldner` bør i formel model gå via `Hæftelse`.
3. `Påkrav` og `Rykker` er ikke sideordnede kommunikationsobjekter uden sammenhæng, men specialiseringer af `Underretning`.
4. `Inddrivelsesskridt` bør forstås som abstrakt klasse med egne undertyper.
5. `Overdragelse til inddrivelse` bør i udgangspunktet knyttes til `Restance` frem for enhver `Fordring`.
6. `Part` er et abstraherende overbegreb og skal ikke forveksles med konkrete organisatoriske enheder i sig selv.

## 9. Konsekvenser for senere informationsmodellering

Version 2 gør det lettere at omsætte modellen til logisk informationsmodel, fordi den peger direkte på nogle sandsynlige modelmønstre:

- `Part` kan blive et fælles referenceobjekt med rollemarkering
- `Hæftelse` kan blive associationsentitet mellem `Fordring` og `Skyldner`
- `Underretning` kan blive fælles dokument- eller meddelelsesstruktur med undertyper
- `Inddrivelsesskridt` kan blive arvshierarki eller typekodebaseret struktur

Det er især nyttigt i moderniseringskontekst, hvor man skal sammenligne gamle og nye systemers begrebsapparat og afgøre, om variationer skyldes ægte forretningsforskelle eller blot forskellig teknisk repræsentation.

## 10. Diagramkonventioner i version 2

Det tilhørende draw.io-diagram anvender følgende konventioner:

- **afrundede bokse** for begreber
- **lyseblå** nuancer for aktør-/partsbegreber
- **lysegule** nuancer for fordrings- og inddrivelsesobjekter
- **lysegrønne** nuancer for kommunikationsbegreber
- **lilla** nuancer for struktur- og overgangsbegreber
- **hvide trekanter** for generaliseringsrelationer
- **fuldoptrukne pile** for semantiske associationsrelationer

## 11. Anbefalede næste skridt for UFST

Efter denne version 2 anbefales følgende modning:

1. juridisk validering af specialiseringerne, især `Restance` som subtype af `Fordring`
2. forretningsmæssig validering af hæftelsesbegreberne
3. afklaring af om flere undertyper af `Inddrivelsesskridt` skal medtages
4. omsætning til logisk informationsmodel med attributter, identifikatorer og kodelister
5. mapping mellem begreberne i modellen og eksisterende begreber i PSRM/ORMB-kontekst

## 12. Kildehenvisninger

### 12.1 Metodisk grundlag

- DIGST, de fællesoffentlige regler for begrebs- og datamodellering:  
  `https://arkitektur.digst.dk/metoder/begrebs-og-datametoder/regler-begrebs-og-datamodellering`

### 12.2 Juridisk vejledning og domænekilder

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

## 13. Samlet vurdering

Version 2 er mere egnet end version 1 som formelt arkitekturgrundlag, fordi den ikke kun oplister begreber, men også tydeliggør:

- hvilke begreber der er overbegreber
- hvilke begreber der er specialiseringer
- hvor relationer bør modelleres som selvstændige begreber
- hvordan flere skyldnere og forskellige hæftelsesformer kan repræsenteres
- hvordan inddrivelsesskridt kan struktureres som familie af beslægtede begreber

Dermed er modellen tættere på det niveau, der normalt er nødvendigt, hvis UFST senere vil anvende den som grundlag for målarkitektur, begrebsharmonisering og logisk datamodellering.
