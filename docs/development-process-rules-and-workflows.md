# Udviklingsproces: Regler fra Excel og processer fra Qualiware

## 1. Regler for gældstyper (Excel decision tables → Drools)

**Kilde:** Forretningsanalytikere vedligeholder gældstype-regler i `.xlsx` Excel decision tables (f.eks. rentesatser, tærskler, indrivelsesparathed).

### Processen

1. **Modtag Excel-ark** fra forretningen med regler pr. gældstype (f.eks. skattegæld, børnebidrag, bøder). Formatet skal følge Drools decision table konventionen med kolonner for betingelser og handlinger:

   | Debt Type | Days Past Due | Interest Rate | Legal Basis |
   |-----------|---------------|---------------|-------------|
   | TAX       | 31+           | 10%           | Renteloven §5 |
   | FINE      | 0+            | 8%            | Standard rate |

2. **Placer filen** i `opendebt-rules-engine/src/main/resources/rules/` som `.xlsx`. `DroolsConfig` loader automatisk alle `.drl` og `.xlsx` filer fra denne mappe via `KieFileSystem`.

3. **For komplekse regler** der kræver programmatisk logik (f.eks. `debt-readiness.drl`, `collection-priority.drl`), skrives DRL-filer af udviklere. Disse bruger request/result-modeller fra `dk.ufst.opendebt.rules.model.*`.

4. **Exponering via API**: Reglerne kaldes via REST-endpoints i `RulesController`:
   - `POST /rules-engine/api/v1/rules/readiness/evaluate`
   - `POST /rules-engine/api/v1/rules/interest/calculate`
   - `POST /rules-engine/api/v1/rules/priority/evaluate`

5. **Test**: Skriv JUnit 5 tests der validerer regeludførelse med kendte input/output. Kør `mvn verify` for at sikre >80% dækning.

6. **Deployment**: Regelændringer i `.xlsx`-filer kræver re-deploy af `opendebt-rules-engine` servicen, da KieContainer bygges ved opstart.

## 2. BPMN 2.0 processer (Qualiware → Flowable)

**Kilde:** Procesmodeller defineres i Qualiware som BPMN 2.0 diagrammer og eksporteres som `.bpmn20.xml`.

### Processen

1. **Eksporter BPMN 2.0 XML fra Qualiware**. Procesmodellen skal indeholde:
   - **Service Tasks** med `flowable:delegateExpression` der peger på Spring beans (f.eks. `${caseAssessmentDelegate}`, `${sendLetterDelegate}`)
   - **User Tasks** med `flowable:assignee` eller `flowable:candidateGroups` for sagsbehandler-opgaver
   - **Timer Events** for frister (f.eks. `P30D` for betalingsfrist)
   - **Signal Events** for eksterne hændelser (f.eks. betaling modtaget, klage indgivet)
   - **Exclusive Gateways** for beslutningspunkter med `conditionExpression`

2. **Tilpas XML til Flowable**: Qualiware-eksporten skal muligvis justeres:
   - Tilføj `flowable:delegateExpression` på service tasks
   - Tilføj Flowable-specifikke extensionElements (f.eks. `flowable:field`)
   - Valider at `conditionExpression` refererer korrekte process-variabler (f.eks. `${collectionStrategy == 'VOLUNTARY_PAYMENT'}`)

3. **Placer filen** i `opendebt-case-service/src/main/resources/processes/` (f.eks. `debt-collection-case.bpmn20.xml`). Flowable auto-deployer registrerer filen ved opstart.

4. **Implementer Java Delegates** i `dk.ufst.opendebt.caseservice.workflow.delegates/`:
   - Hver `ServiceTask` kræver en `JavaDelegate`-implementation
   - Delegates kommunikerer med andre services via API-klienter (aldrig direkte DB-adgang)
   - Delegates sætter process-variabler der styrer gateways

5. **Exponering via `CaseWorkflowService`**:
   ```java
   workflowService.startCaseWorkflow(caseId, strategy, caseworker);
   workflowService.completeTask(taskId, variables);
   workflowService.signalEvent(caseId, "paymentReceived", data);
   ```

6. **Test**: Brug Flowable test support til at unit-teste workflows med mock delegates. Valider alle stier (voluntary, offsetting, garnishment, appeal).

## 3. Samlet workflow: Fra forretning til kode

```
Qualiware (BPMN 2.0)          Excel (gældstype-regler)
       |                              |
       v                              v
  Eksporter .bpmn20.xml       Eksporter .xlsx decision table
       |                              |
       v                              v
  Tilpas til Flowable          Placer i rules/
  (delegateExpressions)              |
       |                              v
       v                        DroolsConfig loader
  Placer i processes/           automatisk ved opstart
       |                              |
       v                              v
  Implementer delegates        Exponeres via RulesController
  (JavaDelegate beans)               |
       |                              v
       v                        Andre services kalder
  CaseWorkflowService          rules API via REST
  eksponerer workflow API
       |
       v
  mvn spotless:apply && mvn verify
```

**Vigtigt:** Ændringer i regler (Excel) eller processer (BPMN) kræver begge re-deploy af den respektive service, da de indlæses ved opstart. Der er ingen hot-reload i den nuværende arkitektur.

## 4. Finansielle transaktioner og hovedbog (ADR-0018)

**Kilde:** Arkitekturbeslutning `architecture/adr/0018-double-entry-bookkeeping.md` (ændring #3, 2026-04-05).

### Invariant

Enhver ny funktionalitet der **flytter penge** eller **ændrer økonomisk stilling** (f.eks. betaling, modregning, rente, gebyr, udlæg, nedskrivning, refusion, korrektion) skal **bogføres i dobbelt bogholderi** i **payment-service** (`BookkeepingService`, `ledger_entries`), medmindre arkitekturen eksplicit undtages i en ADR.

Tabeller i andre services (f.eks. rente-journal i debt-service) kan understøtte beregning og visning, men **erstatte ikke** hovedbogs-posteringer, hvor ADR-0018 gælder.

### Processen ved ny udvikling

1. **Spørgsmål i design/review:** *Hvor er posteringen til hovedbogen?* (eller: hvor er ADR-undtagelsen dokumenteret?)
2. **Petition / løsningsarkitektur:** Beskriv ledger-påvirkning og integration (HTTP til payment-service, events, eller port til `ufst-bookkeeping-core`).
3. **Kode:** Brug eksisterende bookkeeping-API’er; undgå at introducere nye «økonomiske sandheder» kun i en enkelt services database.
4. **Teknisk gæld:** Se f.eks. TB-055 (wire af `LedgerServiceClient` fra debt-service) og TB-010 (balance-validering).

## 5. Webtilgængelighed for alle UI-løsninger

**Kilde:** Digitaliseringsstyrelsens vejledning om webtilgængelighed. Offentlige websteder og mobilapplikationer skal leve op til webtilgængelighedsloven og den harmoniserede standard **EN 301 549 v3.2.1**. For webprojekter bruges **WCAG 2.1 AA** som praktisk baseline, suppleret med øvrige relevante EN 301 549-krav.

### Processen

1. **Afklar scope tidligt**
   - Afklar om løsningen er et selvstændigt websted, en mobilapplikation eller en brugerrettet del af et eksisterende websted.
   - Afklar om løsningen viser dokumenter, video, tovejs-kommunikation eller andre funktioner med ekstra EN 301 549-krav.

2. **Skriv tilgængelighedskrav ind i petition og acceptkriterier**
   - Tastaturbetjening
   - synlig fokusmarkering
   - semantisk struktur og korrekte labels
   - tilgængelige formularer og fejlbeskeder
   - farve/kontrast må ikke være eneste informationsbærer
   - tilgængelige dokumenter eller tilgængelige alternativer

3. **Implementer med tilgængelighed som standard**
   - Brug semantiske HTML-elementer frem for generiske `div`-strukturer, når det er muligt.
   - Sørg for korrekt name/role/value til skærmlæsere.
   - Understøt zoom, reflow og logisk tab-rækkefølge.
   - Undgå utilgængelige verificeringsmekanismer i kontakt- og feedbackflows.

4. **Test både automatisk og manuelt**
   - Kør automatiske accessibility-checks i CI, når UI-projekterne er etableret.
   - Verificér manuelt centrale flows med tastatur.
   - Verificér manuelt kritiske formularer, fejlbeskeder og fokusstyring.
   - Verificér skærmlæserkritiske flows før produktion.

5. **Opret tilgængelighedserklæring per websted/app**
   - Brug Digitaliseringsstyrelsens **WAS-Tool**.
   - Hvert selvstændigt websted og hver mobilapplikation skal have sin egen erklæring.
   - Erklæringen skal opdateres ved væsentlige ændringer og mindst én gang årligt.
   - Erklæringen skal have både skriftlig og telefonisk kontaktinformation.
   - Den skriftlige kontakt må ikke kræve MitID-login eller være bag utilgængelig CAPTCHA.
   - Link til erklæringen skal være let at finde, helst i footer og hvor praktisk muligt via `/was`.

6. **Release gate**
   - En UI-release er ikke klar til produktion, hvis kendte kritiske tilgængelighedsbrud blokerer centrale brugerflows.
   - Tilgængelighedserklæringen skal være opdateret, hvis releasen ændrer dens indhold.
