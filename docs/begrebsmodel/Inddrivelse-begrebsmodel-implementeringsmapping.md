# Implementeringsmapping fra begrebsmodel til kode

## Formål

Dette dokument omsætter begrebsmodellen i `Inddrivelse-begrebsmodel-UFST-v2.md` til kodeorienterede implementeringsmål for OpenDebt.

Formålet er at gøre tre ting eksplicit:

1. `concept -> service`
2. `concept -> entity/API/event`
3. `status -> exists / partial / missing`

Dokumentet er ikke en logisk datamodel. Det er et arbejdsgrundlag til petition-opdeling og senere implementering.

## Statuskoder

| Status | Betydning |
|---|---|
| `exists` | Begrebet er allerede modelleret tydeligt i kode og kan genfindes som selvstændigt domæneobjekt eller stabil API-kontrakt |
| `partial` | Begrebet findes kun indirekte, er blandet sammen med andre begreber, eller mangler væsentlige regler/relationer |
| `missing` | Begrebet er ikke modelleret som selvstændigt domæneobjekt, API eller hændelse |

## Mappingtabel

| Begreb | Primær service | Nuværende entity/API/event | Status | Bemærkning |
|---|---|---|---|---|
| `Part` | `person-registry` + tværgående reference | Ingen fælles abstraktion; referencer findes som `debtorPersonId`, `creditorOrgId` | `partial` | Rollen findes indirekte, men der er ingen fælles `Part`-model eller rollelag |
| `Fordringshaver` | `person-registry`, `debt-service`, `creditor-portal`, `integration-gateway` | `DebtEntity.creditorOrgId`, petition002 API/portal-flow | `partial` | Findes som reference, ikke som domæneobjekt med egne regler |
| `Skyldner` | `person-registry`, `debt-service`, `case-service` | `DebtEntity.debtorPersonId`, `CaseEntity.debtorPersonId` | `partial` | Findes som reference, men ingen eksplicit relation til hæftelse eller flere skyldnere |
| `Restanceinddrivelsesmyndighed` | `case-service` / orchestration | Ingen entity eller API; implicit systemrolle | `missing` | Mangler som eksplicit rolle/begreb |
| `Kravgrundlag` | `debt-service` | Ingen entity, intet felt, ingen API | `missing` | Skal modelleres som sporbar baggrund for en fordring |
| `Fordring` | `debt-service` | `DebtEntity`, `DebtDto`, `DebtController` | `partial` | `DebtEntity` bærer i praksis fordringsdata, men er navngivet og livscyklusstyret som generisk debt |
| `Restance` | `debt-service`, `case-service` | `dueDate`, `readinessStatus`, `status` i `DebtEntity` | `partial` | Begrebet findes kun implicit som tilstand, ikke som tydelig specialisering af `Fordring` |
| `Betalingsfrist` | `debt-service` | `DebtEntity.dueDate` | `partial` | Findes som felt, men ikke som selvstændigt begreb eller regelobjekt |
| `Underretning` | `letter-service` | Ingen fælles entity/API; kun `LetterDto`-typer | `missing` | Der mangler fælles struktur for formel kommunikation |
| `Påkrav` | `letter-service`, `payment-service` | Petition001 refererer påkrav; OCR-flow afhænger af det | `partial` | Findes begrebsmæssigt, men ikke som eksplicit entity/API |
| `Rykker` | `letter-service` | Ingen særskilt entity/API | `missing` | Kun begreb i modellen; ingen implementering |
| `Indsigelse` | `case-service`, `debt-service` | Ingen entity, ingen API, ingen workflow-tilstand | `missing` | Mangler helt som forretningsobjekt |
| `Hæftelse` | `debt-service` | Ingen entity; én skyldner pr. `DebtEntity` | `missing` | Blokerer for korrekt modellering af flere skyldnere |
| `Enehæftelse` | `debt-service` | Ingen subtype eller typekode | `missing` | Kan ikke udtrykkes eksplicit i dag |
| `Solidarisk hæftelse` | `debt-service` | Ingen subtype eller typekode | `missing` | Flere skyldnere understøttes ikke |
| `Delt hæftelse` | `debt-service` | Ingen subtype eller typekode | `missing` | Andele/beløbsfordeling kan ikke udtrykkes |
| `Overdragelse til inddrivelse` | `debt-service`, `case-service`, `rules-engine` | Ingen entity/API/event; readiness findes som stub | `missing` | Overdragelseshandlingen og audit-sporet mangler |
| `Inddrivelsesskridt` | `case-service` + specialiserede services | `CaseEntity.CollectionStrategy`, enkelte enums | `partial` | Findes som strategiord, ikke som fælles abstrakt domæneobjekt |
| `Modregning` | `offsetting-service`, `payment-service` | `BookkeepingService.recordOffsetting`, `CollectionStrategy.OFFSETTING` | `partial` | Findes som konto-/bogføringsoperation, ikke som selvstændigt skridtobjekt |
| `Lønindeholdelse` | `wage-garnishment-service`, `case-service` | `CollectionStrategy.WAGE_GARNISHMENT`, `LetterDto.WAGE_GARNISHMENT_NOTICE` | `partial` | Findes som strategi/terminologi, ikke som objekt med egen livscyklus |
| `Udlæg` | fremtidig enforcement-funktion | Kun sporadiske teststrenge | `missing` | Ingen service, entity eller API i nuværende kode |

## Eksisterende kodeankre

### `debt-service`

- `DebtEntity` er nuværende tekniske bærer af `Fordring`
- `ReadinessValidationServiceImpl` er begyndelsen på inddrivelsesparathed, men stadig stub
- `DebtController` eksponerer CRUD og OCR-opslag, men ikke overdragelse, hæftelse eller indsigelse

### `case-service`

- `CaseEntity` er procesbærer for inddrivelsessag
- `CollectionStrategy` peger på senere understøttelse af `Modregning` og `Lønindeholdelse`
- der findes endnu ingen eksplicit `Indsigelse`-model eller blokering af workflow herpå

### `payment-service`

- `PaymentMatchingServiceImpl` implementerer OCR-baseret betaling mod `Påkrav`
- `BookkeepingServiceImpl` understøtter finansielle følger af betaling, modregning og tilbagebetaling
- overbetalingsregler er stadig placeholder og ikke koblet til fuld begrebsmodel

### `rules-engine`

- `RulesServiceImpl.evaluateReadiness()` er oplagt hjemsted for formelle regler om inddrivelsesparathed
- der findes endnu ingen regelsæt for hæftelse, overdragelse eller valg af inddrivelsesskridt

### `letter-service`

- der findes grundlæggende letter-infrastruktur, men ikke en begrebsmæssig `Underretning`-model med undertyperne `Påkrav` og `Rykker`

## Kritiske semantiske huller

### 1. `Fordring` og `Restance` er blandet sammen

Nuværende model bruger én `DebtEntity` til alt. Det gør det uklart:

- hvornår noget er en almindelig fordring
- hvornår det er blevet en restance
- hvornår det lovligt kan overdrages til inddrivelse

### 2. Flere skyldnere kan ikke modelleres korrekt

Begrebsmodel v2 kræver `Hæftelse` mellem `Fordring` og `Skyldner`. Den relation findes ikke i kode. Hele modellen for solidarisk og delt hæftelse mangler.

### 3. Kommunikation er ikke modelleret som domæneobjekt

`Påkrav` og `Rykker` bruges i tekst og flows, men der findes ingen fælles `Underretning`-struktur med:

- afsender
- modtager(e)
- kanal
- dokumentstatus
- relation til fordring/restance

### 4. Overdragelse til inddrivelse mangler som selvstændig handling

Der findes readiness, men ikke selve overdragelsesbegrebet med audit, myndighedsmodtager og procesopstart.

### 5. Inddrivelsesskridt er kun løst repræsenteret

`Modregning` og `Lønindeholdelse` optræder som strategier eller bogføringskonsekvenser, ikke som egentlige skridtobjekter med egen livscyklus og regler.

## Anbefalet petition-opdeling

| Petition | Fokus | Primære services |
|---|---|---|
| `petition003` | `Fordring -> Restance -> Overdragelse til inddrivelse` | `debt-service`, `case-service`, `rules-engine` |
| `petition004` | `Underretning / Påkrav / Rykker` | `letter-service`, `debt-service`, `payment-service` |
| `petition005` | `Hæftelse + flere skyldnere` | `debt-service`, `person-registry`, `case-service` |
| `petition006` | `Indsigelse + workflow blocking` | `case-service`, `debt-service`, `letter-service` |
| `petition007` | `Inddrivelsesskridt: Modregning / Lønindeholdelse / Udlæg` | `offsetting-service`, `wage-garnishment-service`, `case-service`, `payment-service` |

## Konklusion

OpenDebt har allerede tekniske byggesten, men begrebsmodel v2 er kun delvist repræsenteret i kode. Den største fejl ville være at lade som om nuværende `DebtEntity` allerede er en fuld implementering af modellen. Det er den ikke.

Næste skridt er derfor at omsætte denne mapping til konkrete petitions med afgrænset scope og testbare outcome contracts.
