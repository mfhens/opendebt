# Implementation Gap Report

**Concept model version:** 1.0  
**Domain:** Inddrivelse af offentlige fordringer  
**Generated from:** `domain/concept-model.yaml`  
**Approval status:** Not yet approved

> This report lists all concepts with implementation status `partial` or `missing`.
> Concepts with `exists` status are not shown. `exists` count: **0**.
> Update `domain/concept-model.yaml` as implementation progresses, then regenerate.

---

## Summary

| Status | Count |
|---|---|
| `exists` | 0 |
| `partial` | 10 |
| `missing` | 34 |
| Total assessed | 44 |

---

## Partial — concepts that exist but are incomplete

| Concept ID | Term | Service | Entity / API | Notes |
|---|---|---|---|---|
| `part` | Part | person-registry | — | Rollen findes indirekte som debtorPersonId og creditorOrgId, men ingen fælles Part-model eller rollelag. |
| `fordringshaver` | Fordringshaver | debt-service | — | Findes som reference (creditorOrgId i DebtEntity), ikke som domæneobjekt med egne regler. |
| `skyldner` | Skyldner | debt-service | — | Findes som reference (debtorPersonId i DebtEntity og CaseEntity), men ingen eksplicit relation til hæftelse eller flerpartshæftelse. |
| `fordring` | Fordring | debt-service | DebtEntity / `/api/debts` | DebtEntity bærer fordringsdata men er navngivet som generisk debt; hæftelse, overdragelse og indsigelse mangler. |
| `restance` | Restance | debt-service | — | Begrebet findes kun implicit som tilstand i DebtEntity, ikke som tydelig specialisering af Fordring. |
| `betalingsfrist` | Betalingsfrist | debt-service | DebtEntity.dueDate | Findes som felt, ikke som selvstændigt begreb eller regelobjekt. |
| `paakrav` | Påkrav | letter-service | — | Bruges begrebsmæssigt i OCR-baseret betalingsflow, men ikke som eksplicit entity eller API. |
| `inddrivelsesskridt` | Inddrivelsesskridt | case-service | CollectionStrategy (enum) | Findes som enum, ikke som fælles abstrakt domæneobjekt med livscyklus. |
| `modregning` | Modregning | payment-service | BookkeepingService.recordOffsetting | Findes som bogføringsoperation, ikke som selvstændigt skridtobjekt. |
| `loenindeholdelse` | Lønindeholdelse | wage-garnishment-service | CollectionStrategy.WAGE_GARNISHMENT | Findes som strategi/terminologi, ikke som objekt med livscyklus. |

---

## Missing — grouped by service

### debt-service

| Concept ID | Term | Area | Notes |
|---|---|---|---|
| `kravgrundlag` | Kravgrundlag | Krav og status | Ingen entity, intet felt, ingen API; skal modelleres som sporbar baggrund for en fordring. |
| `foraeldelse` | Forældelse | Krav og status | Forældelsesdato ikke modelleret som begreb; ingen valideringsregler for forældelsesafbrydelse. |
| `fordringsart` | Fordringsart | Krav og status | Klassifikation ikke modelleret; styrer tilgængelige inddrivelsesskridt. |
| `fordringstype` | Fordringstype | Krav og status | Ikke modelleret; bestemmer om fordringen er civilretlig med begrænsede inddrivelsesmuligheder. |
| `fordringskompleks` | Fordringskompleks | Kravstruktur | Strukturel sammenhæng mellem hoved- og underfordringer ikke modelleret. |
| `overdragelse-til-inddrivelse` | Overdragelse til inddrivelse | Livscyklus | Overdragelseshandlingen og audit-sporet mangler; readinessStatus er stub. |
| `hoering` | Høring | Livscyklus | Høring-tilstanden ikke modelleret; ingen workflow for fordringshaveres godkendelse. |
| `regulering` | Regulering | Livscyklus | Op- og nedskrivning ikke modelleret som selvstændige handlinger med årsagskoder. |
| `opskrivning` | Opskrivning | Livscyklus | Ikke modelleret. |
| `nedskrivning` | Nedskrivning | Livscyklus | Ikke modelleret. |
| `tilbagekald` | Tilbagekald | Livscyklus | Årsagskoder (BORD, BORT, FEJL, HENS, KLAG) og differentierede virkninger ikke modelleret. |
| `genindsendelse` | Genindsendelse | Livscyklus | Kun mulig efter KLAG/HENS-tilbagekald; ikke modelleret. |
| `inddrivelsesrente` | Inddrivelsesrente | Finansiel | Renteberegning og undtagelser ikke modelleret. |
| `haeftelse` | Hæftelse | Reaktion og tvist | Ingen entity; blokerer korrekt modellering af flerpartshæftelse. |
| `eneheftelse` | Enehæftelse | Reaktion og tvist | Ingen subtype eller typekode. |
| `solidarisk-haeftelse` | Solidarisk hæftelse | Reaktion og tvist | Ingen subtype eller typekode; PSRM understøtter kun solidarisk hæftelse i praksis. |
| `delt-haeftelse` | Delt hæftelse | Reaktion og tvist | Kræver i PSRM separate fordringer pr. skyldner. |

### case-service

| Concept ID | Term | Area | Notes |
|---|---|---|---|
| `restanceinddrivelsesmyndighed` | Restanceinddrivelsesmyndighed | Aktører og roller | Ingen entity eller API; implicit systemrolle. |
| `indsigelse` | Indsigelse | Reaktion og tvist | Ingen entity, ingen API, ingen workflow-tilstand; mangler helt som forretningsobjekt. |
| `afdragsordning` | Afdragsordning | Myndighedsudøvelse | Ikke modelleret som selvstændigt skridtobjekt. |

### letter-service

| Concept ID | Term | Area | Notes |
|---|---|---|---|
| `underretning` | Underretning | Kommunikation | Ingen fælles Underretning-model; kun LetterDto-typer. |
| `skyldnerunderretning` | Skyldnerunderretning | Kommunikation | Ingen fælles abstraktion for skyldnerunderretninger. |
| `rykker` | Rykker | Kommunikation | Ingen særskilt entity eller API. |
| `underretningsmeddelelse` | Underretningsmeddelelse | Kommunikation | Ingen fælles model med PSRM-undertyperne. |
| `afregningsunderretning` | Afregningsunderretning | Kommunikation | Ikke modelleret. |
| `udligningsunderretning` | Udligningsunderretning | Kommunikation | Ikke modelleret; gensidigt eksklusiv med Allokeringsunderretning. |
| `allokeringsunderretning` | Allokeringsunderretning | Kommunikation | Ikke modelleret; gensidigt eksklusiv med Udligningsunderretning. |
| `renteunderretning` | Renteunderretning | Kommunikation | Ikke modelleret; gensidigt eksklusiv med Detaljeret renteunderretning. |
| `detaljeret-renteunderretning` | Detaljeret renteunderretning | Kommunikation | Kun for system-til-system; ikke modelleret. |
| `afskrivningsunderretning` | Afskrivningsunderretning | Kommunikation | Ikke modelleret. |
| `tilbagesendelsesunderretning` | Tilbagesendelsesunderretning | Kommunikation | Ikke modelleret. |
| `returneringsunderretning` | Returneringsunderretning | Kommunikation | Ikke modelleret. |

### payment-service

| Concept ID | Term | Area | Notes |
|---|---|---|---|
| `daekning` | Dækning | Finansiel | Dækning og dækningsrækkefølge ikke modelleret som selvstændigt begreb. |

### *(ingen service — fremtidig)*

| Concept ID | Term | Area | Notes |
|---|---|---|---|
| `udlaeg` | Udlæg | Myndighedsudøvelse | Ingen service, entity eller API; fremtidig enforcement-funktion. |

---

## Anbefalede petitions

Baseret på gap-analysen anbefales følgende petition-opdeling:

| Petition | Fokus | Primære services |
|---|---|---|
| `petition003` | Fordring → Restance → Overdragelse til inddrivelse (inkl. Fordringsart, Fordringstype, Forældelse) | debt-service, case-service, rules-engine |
| `petition004` | Underretning / Påkrav / Rykker / Underretningsmeddelelse | letter-service, debt-service, payment-service |
| `petition005` | Hæftelse + flerpartsskyldner + Fordringskompleks | debt-service, person-registry, case-service |
| `petition006` | Indsigelse + workflow blocking | case-service, debt-service, letter-service |
| `petition007` | Inddrivelsesskridt: Modregning / Lønindeholdelse / Udlæg / Afdragsordning | payment-service, wage-garnishment-service, case-service |
| `petition008` | Livscyklus: Regulering / Tilbagekald / Genindsendelse / Høring | debt-service, case-service, rules-engine |
| `petition009` | Finansiel: Dækning / Inddrivelsesrente / Dækningsrækkefølge | payment-service, debt-service |
