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
