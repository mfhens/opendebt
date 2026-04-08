# Petition 061: Afdragsordninger — instalment plan management (G.A.3.1.1)

## Summary

Afdragsordninger (instalment plans) are the primary soft-collection tool available to PSRM. They
allow a debtor to settle accumulated debt through structured periodic payments calibrated to
individual payment capacity rather than the full outstanding amount on demand. Without a formal
rule engine, caseworkers apply GIL § 11 tabeltræk rules inconsistently, and manual betalings-
evnevurdering (§ 11 stk. 6) lacks auditability. This petition specifies the complete backend
calculation engine, lifecycle management, and caseworker portal support required to enforce
proportional, legally compliant afdragsordninger for both private persons and virksomheder.

**References:** Petition 007 (lønindeholdelse — suspended by active afdragsordning),
Petition 059 (forældelse — interaction on misligholdelse and resumption of inddrivelse),
Petition 062 (lønindeholdelse fuld specifikation — companion tool).
**Catala companion:** Petition 071 (P071) — tabeltræk lookup and betalingsevne calculation
are Catala Tier A targets.
**G.A. snapshot version:** v3.16 (2026-03-28).

---

## Context and motivation

After a fordring is accepted for inddrivelse, PSRM must choose the appropriate inddrivelse method.
Afdragsordninger are preferred for debtors who demonstrate ability to pay over time but cannot
settle the debt in full. The legal basis for determining the monthly instalment is threefold:

1. **Tabeltræk (GIL § 11, stk. 1):** Automated lookup of debtor's annual nettoindkomst in a
   statutory percentage table. The result is the presumptive monthly ydelse and is the default method.
2. **Konkret betalingsevnevurdering (GIL § 11, stk. 6):** An individually calculated alternative,
   available only to debtors at or above the lavindkomstgrænse. The debtor submits a budgetskema
   (budget form) and PSRM calculates concrete disposable income per Gæld.bekendtg. chapter 7.
3. **Kulanceaftale (GIL § 11, stk. 11):** A discretionary humanitarian arrangement with a ydelse
   below both other methods. Caseworker-initiated only; no automated calculation.

Current gaps in the OpenDebt implementation:
- Tabeltræk percentage brackets are not encoded; caseworkers apply percentages manually.
- Betalingsevnevurdering logic is undocumented; budgetskema input is not structured.
- Lifecycle state machine for afdragsordning is absent; misligholdelse is handled informally.
- No systematic suspension of lønindeholdelse when an afdragsordning becomes AKTIV.
- Virksomheder assessed under different evidence rules, not enforced.

---

## Domain terms

| Term | Danish definition |
|------|-------------------|
| Afdragsordning | An instalment plan between PSRM and a debtor for paying off a fordring in monthly instalments. |
| Tabeltræk | Statutory method per GIL § 11 stk. 1: afdragsprocent derived from a published table based on annual nettoindkomst and forsørgerpligt. |
| Nettoindkomst | Annual income after tax, used as the base for tabeltræk calculation. |
| Afdragsprocent | The percentage of annual nettoindkomst that determines the annual betalingsevne. |
| Betalingsevne | Payment capacity — annual amount a debtor can pay toward debt after basic living expenses. |
| Lavindkomstgrænse | Minimum income threshold; below this level, tabeltræk yields 0 kr and no plan is possible via tabeltræk. |
| Månedlig ydelse | Monthly instalment amount, rounded DOWN to nearest 50 or 100 kr per GIL § 11 stk. 2. |
| Forsørgerpligt | Legal obligation to support dependents (children); debtors with forsørgerpligt receive a more favourable tabeltræk percentage. |
| Konkret betalingsevnevurdering | Individual payment-capacity assessment per GIL § 11 stk. 6 and Gæld.bekendtg. chapter 7, using a budgetskema. |
| Budgetskema | Standardised budget form submitted by the debtor to document income and expenses for konkret betalingsevnevurdering. |
| Kulanceaftale | Discretionary humanitarian arrangement per GIL § 11 stk. 11 with ydelse below tabeltræk/konkret; requires caseworker justification. |
| Misligholdelse | Default on the afdragsordning — debtor has missed a payment after the notice period has elapsed. |
| MISLIGHOLT | Lifecycle state indicating the debtor has defaulted; inddrivelse tools may resume. |
| Virksomhedsafdragsordning | Afdragsordning for a virksomhed (business entity); different evidence and assessment rules apply. |
| Igangværende virksomhed | Active/registered business entity; requires evidence of ability to pay ongoing obligations. |
| Afmeldt virksomhed | Deregistered business entity; assessed like a private person (tabeltræk applicable). |

---

## Legal basis / G.A. references

| Reference | Content |
|---|---|
| G.A.3.1.1 | Afdragsordninger — primary legal framework and lifecycle overview |
| G.A.3.1.1.1 | Tabeltræk (GIL § 11 stk. 1–2) and konkret betalingsevnevurdering (§ 11 stk. 6) |
| G.A.3.1.1.2 | Afdragsordning for virksomheder — different evidence and assessment rules |
| G.A.3.1.1.3 | Afdragsordning vs. udlægsforretning — coexistence rules |
| G.A.2.4 | Forældelse — afdragsordning is an inddrivelsesskridt but does NOT afbryde forældelsesfrist |
| GIL § 11, stk. 1 | Tabeltræk: afdragsprocent based on annual nettoindkomst and forsørgerpligt |
| GIL § 11, stk. 2 | Monthly ydelse rounded DOWN to nearest 50 or 100 kr |
| GIL § 11, stk. 6 | Konkret betalingsevnevurdering: available at/above lavindkomstgrænse; can yield lower ydelse |
| GIL § 11, stk. 11 | Kulanceaftale: discretionary humanitarian arrangement, caseworker-initiated |
| GIL § 11a | Tabeltræk for virksomheder — specific rules for igangværende virksomheder |
| GIL § 45 | Annual index regulation: income brackets updated every 1 January by SKM-meddelelse |
| Gæld.bekendtg. §§ 11–12 | Afdragsordning procedural requirements |
| Gæld.bekendtg. chapter 7 | Konkret betalingsevnevurdering calculation methodology |

---

## PSRM Reference Context

In PSRM, the afdragsordning is modelled as a distinct entity linked to one or more fordringer.
Key PSRM behaviour:

- **Tabeltræk:** PSRM looks up the debtor's annual nettoindkomst in the current index table (updated
  1 January each year per GIL § 45). The lookup yields an afdragsprocent. Annual betalingsevne =
  afdragsprocent × nettoindkomst. Monthly ydelse = annual_betalingsevne / 12, then rounded DOWN
  to the nearest 50 or 100 kr (GIL § 11, stk. 2).

- **Lavindkomstgrænse:** If the debtor's nettoindkomst is below the lavindkomstgrænse, the
  tabeltræk yields an afdragsprocent of 0% (effectively 0 kr/month). An afdragsordning cannot be
  created via tabeltræk for such debtors.

- **Forsørgerpligt:** Debtors with a legal obligation to support dependents (børn) look up a
  separate, more favourable percentage column in the tabeltræk table. At the same nettoindkomst
  level above the lavindkomstgrænse, the forsørgerpligtig debtor receives a lower afdragsprocent
  than a debtor without forsørgerpligt, reflecting higher living costs.

- **Konkret betalingsevnevurdering (§ 11 stk. 6):** Only available to debtors at or above the
  lavindkomstgrænse. Debtor submits a budgetskema. PSRM calculates concrete disposable income per
  Gæld.bekendtg. chapter 7. If the resulting monthly ydelse is lower than the tabeltræk ydelse,
  PSRM evaluates whether to adopt the concrete assessment. The konkret ydelse replaces the
  tabeltræk ydelse for the duration of the afdragsordning.

- **Lifecycle:** PSRM tracks the lifecycle state machine: OPRETTET → AKTIV → (MISLIGHOLT |
  AFVIKLET | ANNULLERET). An AKTIV afdragsordning automatically suspends any existing
  lønindeholdelsesafgørelse (except for tvangsbøder). On MISLIGHOLT, lønindeholdelse may resume.

- **Forældelse interaction (G.A.2.4):** An afdragsordning is an inddrivelsesskridt but does NOT
  itself afbryde (interrupt) the forældelsesfrist. Resumption of lønindeholdelse after misligholdelse
  constitutes a new inddrivelsesskridt and DOES afbryde.

- **Virksomheder (GIL § 11a):** For igangværende virksomheder, tabeltræk is replaced by a
  cash-flow–based assessment. Evidence of ability to meet ongoing obligations must be provided.
  Afmeldte virksomheder are assessed identically to private persons.

- **GDPR:** No CPR number or personal name is stored in the afdragsordning service. Persons are
  referenced via `person_id` (UUID) from the Person Registry (petition 023/024).

---

## Functional requirements

### FR-1: Tabeltræk calculation engine (GIL § 11, stk. 1–2)

The afdragsordning service shall implement a tabeltræk calculation engine that:

1. Accepts as input: debtor `person_id`, annual nettoindkomst (kr), forsørgerpligt flag (boolean),
   and the effective index year.
2. Looks up the correct afdragsprocent from the index table for the effective year, income bracket,
   and forsørgerpligt status.
3. Computes annual betalingsevne = `afdragsprocent × nettoindkomst`.
4. Computes monthly ydelse = `annual_betalingsevne / 12`.
5. Rounds DOWN to the nearest 50 or 100 kr (GIL § 11, stk. 2). If the unrounded ydelse is not
   a multiple of 50, the result is the largest multiple of 50 that does not exceed the unrounded value.
6. If the debtor's nettoindkomst is below the lavindkomstgrænse for the effective year, the engine
   returns 0 kr/month (no afdragsordning possible via tabeltræk).
7. The calculation result, input parameters, and effective index table year shall be stored on the
   afdragsordning entity for auditability.

The engine shall be deterministic: the same inputs with the same index table always produce the
same output.

### FR-2: Yearly index table management (GIL § 45)

The service shall maintain a versioned index table of (income bracket, forsørgerpligt, afdragsprocent)
triples per calendar year. Requirements:

1. A new year's table can be loaded via an admin API endpoint before 1 January.
2. New brackets take effect from 1 January of the relevant year; calculations for earlier dates
   use the table in effect at the time of the original plan creation.
3. Existing afdragsordninger are NOT automatically recalculated upon an index update. Recalculation
   requires an explicit ændring event (caseworker or debtor income change).
4. The current and all historical index tables are stored and retrievable.
5. An admin may view which index table version applies to any given afdragsordning.

### FR-3: Afdragsordning lifecycle management (G.A.3.1.1)

The service shall enforce the full lifecycle state machine:

| From state | To state | Trigger | Guard |
|---|---|---|---|
| (new) | OPRETTET | CreateAfdragsordningRequest | Valid inputs, fordring under inddrivelse |
| OPRETTET | AKTIV | Caseworker confirms / debtor accepts | — |
| AKTIV | MISLIGHOLT | Misligholdelse detection (FR-4) fires | Notice period elapsed |
| AKTIV | AFVIKLET | All included fordringer fully paid | Balance = 0 |
| AKTIV | ANNULLERET | Caseworker annullerer | Justification required |
| MISLIGHOLT | AKTIV | Caseworker reinstatement | Justification required |
| MISLIGHOLT | ANNULLERET | Caseworker annullerer | — |

Invariants:
- An AKTIV afdragsordning suppresses any existing lønindeholdelsesafgørelse for the same debtor
  (except for tvangsbøder fordringer per G.A.3.1.1). The suppression event is recorded.
- Additional fordringer can be added to an AKTIV afdragsordning; the ydelse is recalculated
  if the total outstanding amount changes materially.
- All state transitions are recorded with actor, timestamp, reason, and the state before and after.

### FR-4: Misligholdelse detection (G.A.3.1.1)

The service shall detect payment default automatically:

1. For each AKTIV afdragsordning, the service checks whether the expected ydelse was received
   within the payment period.
2. On first missed payment, a misligholdelsesvarsel (notice) is sent to the debtor via Digital Post.
3. After the statutory notice period has elapsed without payment, the afdragsordning transitions to
   MISLIGHOLT.
4. On transition to MISLIGHOLT, the suspension of lønindeholdelse is lifted; lønindeholdelse may
   resume at caseworker discretion.
5. A caseworker may reinstate (genindtræde) an afdragsordning after misligholdelse, resetting it to
   AKTIV. Reinstatement requires a caseworker note.

### FR-5: Konkret betalingsevnevurdering (GIL § 11, stk. 6 / Gæld.bekendtg. chapter 7)

The service shall support the concrete payment-capacity assessment method:

1. The method is only available to debtors whose nettoindkomst is at or above the lavindkomstgrænse.
   Requests for debtors below the lavindkomstgrænse shall be rejected with a clear error.
2. The debtor or caseworker submits a structured budgetskema containing:
   - Monthly gross income (all sources)
   - Monthly fixed expenses (housing, transportation, food, utilities)
   - Number of dependents
3. PSRM calculates monthly disposable income = income − expenses (per Gæld.bekendtg. chapter 7 rules).
4. The konkret monthly ydelse is compared to the tabeltræk ydelse. If the konkret ydelse is lower,
   the caseworker evaluates and may adopt the konkret amount.
5. Both the tabeltræk ydelse and the konkret ydelse (with full budgetskema inputs) are stored
   for audit.
6. The resulting afdragsordning records which method (TABELTRÆK or KONKRET) was used and why.

### FR-6: Kulanceaftale workflow (GIL § 11, stk. 11)

The service shall support the kulanceaftale as a workflow-only feature:

1. A caseworker initiates a kulanceaftale from the sagsbehandler portal.
2. The caseworker must supply a free-text justification describing the humanitarian grounds.
3. The proposed monthly ydelse (lower than tabeltræk and konkret) is entered manually by the
   caseworker; there is no automated calculation.
4. The kulanceaftale is stored as an afdragsordning with type KULANCE and includes the justification
   text, the manually entered ydelse, and the caseworker identity.
5. No automated approval is granted; the case may require supervisory review (out of scope for
   automated enforcement).

### FR-7: Virksomhed afdragsordning (G.A.3.1.1.2)

The service shall apply different assessment rules for virksomheder:

1. **Igangværende virksomhed:** The afdragsordning requires cash-flow evidence (e.g., most recent
   3 months of bank statements or accountant certification). Tabeltræk does not apply; the ydelse
   is based on the virksomhed's documented cash surplus. The creation request must include an
   evidence reference.
2. **Afmeldt virksomhed:** Assessed like a private person; tabeltræk and konkret
   betalingsevnevurdering both apply in the same way as for persons.
3. The `person_id` / `org_id` on the afdragsordning entity distinguishes person afdragsordninger
   from virksomhed afdragsordninger. No CVR number is stored directly.

### FR-8: Sagsbehandler portal UI

The sagsbehandler portal shall support the following afdragsordning management tasks:

1. **Create:** Form to create a new afdragsordning (tabeltræk, konkret, kulance) with income and
   forsørgerpligt input, budgetskema upload for konkret, and justification for kulance.
2. **View:** List and detail views showing lifecycle state, included fordringer, monthly ydelse,
   next ydelse date, and payment history.
3. **Update:** Add fordring to an active afdragsordning; register a ydelse change (income evidence
   required); annullere with reason; reinstate after misligholdelse.
4. **Register misligholdelse manually:** Caseworker can record a missed payment and trigger the
   notice period.
5. The portal uses `person_id` for all debtor references; CPR is resolved for display via
   Person Registry API call (not stored in the portal or backend DB).

### FR-9: Forældelse interaction (G.A.2.4)

The service shall correctly model the forældelse interaction:

1. An AKTIV afdragsordning is classified as an inddrivelsesskridt in the case timeline.
2. The afdragsordning entity does NOT record an afbrydelse of the forældelsesfrist. Creating or
   maintaining an afdragsordning does not reset the forældelsesfrist.
3. When lønindeholdelse is resumed after misligholdelse, the lønindeholdelse service (petition 062)
   is responsible for recording the afbrydelse (via the lønindeholdelsesafgørelse). Petition 061
   notifies petition 062 of the MISLIGHOLT event; prescription afbrydelse is not recorded here.
4. The API response for an afdragsordning includes a boolean `afbryderForaeldelse: false` to make
   this clear to consuming systems.

---

## Non-functional requirements

- **NFR-1: Tabeltræk determinism.** Given the same (nettoindkomst, forsørgerpligt, index year)
  inputs, the tabeltræk engine must produce identical results on every invocation. The engine must
  be independently testable via unit tests with fixed table fixtures.

- **NFR-2: Full audit trail.** Every afdragsordning event (creation, state transition, ydelse
  change, misligholdelse notice, reinstatement, annullation) must be logged to the Central Log
  System (CLS) audit trail with actor, timestamp, before/after state, and reason.

- **NFR-3: GDPR.** No CPR number, CVR number, name, or address is stored in the afdragsordning
  service or its database. Persons are referenced via `person_id` (UUID). CPR display in the portal
  is resolved at runtime via Person Registry API. All audit log entries reference `person_id` only.

---

## Constraints

- **GIL § 11 stk. 2:** Monthly ydelse must be rounded DOWN (not up, not rounded to nearest) to
  the nearest 50 or 100 kr. Failure to round down is a legal compliance defect.

- **GIL § 45:** Index tables are updated annually by SKM-meddelelse. Calculations must use the
  table in force at the time of plan creation; existing plans do not auto-update.

- **GIL § 11 stk. 6 eligibility:** Konkret betalingsevnevurdering is only accessible to debtors at
  or above the lavindkomstgrænse. Requests for ineligible debtors must be rejected.

- **Lønindeholdelse coexistence (G.A.3.1.1):** An AKTIV afdragsordning suspends lønindeholdelse,
  except where the fordring type is tvangsbøder (GIL special provision). An afdragsordning on
  tvangsbøder can coexist with lønindeholdelse.

- **Udlæg coexistence (G.A.3.1.1.3):** An active afdragsordning may coexist with the initiation of
  an udlægsforretning in certain cases. The specific boundary is documented in G.A.3.1.1.3 and is
  tracked in the udlæg petition (petition 066). Petition 061 does not implement udlæg logic.

- **Forældelse non-afbrydelse (G.A.2.4):** Afdragsordning creation is NOT an afbrydelsesgrund.
  This must be clearly documented in API responses and system architecture.

- **Catala companion (P071):** Tabeltræk lookup and betalingsevne rounding are Catala Tier A
  targets. The Java calculation engine in this petition must expose deterministic inputs/outputs
  matching the Catala specification in P071. No divergence is acceptable.

---

## Deliverables

| Deliverable | Path / Location |
|---|---|
| AfdragsordningService (tabeltræk engine, lifecycle, detection) | `opendebt-debt-service/src/main/java/.../service/AfdragsordningService.java` |
| TabeltræksUdregningService (pure calculation, Catala-compatible) | `opendebt-debt-service/src/main/java/.../service/TabeltræksUdregningService.java` |
| IndeksTabelsRepository (versioned index tables) | `opendebt-debt-service/src/main/java/.../repository/IndeksTabelsRepository.java` |
| AfdragsordningRepository | `opendebt-debt-service/src/main/java/.../repository/AfdragsordningRepository.java` |
| AfdragsordningController (create, update, query, annullere) | `opendebt-debt-service/src/main/java/.../controller/AfdragsordningController.java` |
| AfdragsordningDto, CreateAfdragsordningRequest, BudgetskemaDto | `opendebt-debt-service/src/main/java/.../dto/` |
| Database migration (afdragsordning + lifecycle_event + betalingsevne + index tables) | `opendebt-debt-service/src/main/resources/db/migration/V061__afdragsordninger.sql` |
| AfdragsordningController (sagsbehandler portal BFF) | `opendebt-caseworker-portal/src/main/java/.../controller/AfdragsordningController.java` |
| Thymeleaf templates: list, detail, create, update | `opendebt-caseworker-portal/src/main/resources/templates/afdragsordning/` |
| Danish message bundle additions | `opendebt-caseworker-portal/src/main/resources/messages_da.properties` |
| English message bundle additions | `opendebt-caseworker-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition061-afdragsordninger.feature` |
| OpenAPI spec additions | `opendebt-debt-service/src/main/resources/openapi/afdragsordning.yaml` |

---

## Out of scope

- **Catala encoding (P071):** Catala companion for tabeltræk and betalingsevne is tracked in
  Petition 071. P061 delivers the Java implementation; P071 delivers the formal specification.
- **Automatiseret modregning (G.A.3.1.4):** Separate petition.
- **Udlægsforretning logic (G.A.3.1.1.3):** Udlæg coexistence rules are tracked in petition 066.
- **Rentegodtgørelse (GIL § 18 l):** Not relevant to afdragsordninger; see petition 053 / TB-039.
- **Supervisory approval workflow for kulanceaftale:** Out of scope for automated enforcement;
  caseworker note is sufficient for this petition.
- **International debtors (G.A.3.6):** Not in scope.
- **Autrisations-fastholdelses afdragsordning (special permit-preservation type):** Noted in
  G.A.3.1.1.2 but not in scope for this petition; tracked in technical backlog.

---

## Definition of done

- `TabeltræksUdregningService` produces deterministic results matching the GIL § 11 table for
  at least three representative income brackets with and without forsørgerpligt.
- Income below lavindkomstgrænse returns 0 kr/month; afdragsordning creation is rejected at API.
- Monthly ydelse is always rounded DOWN to the nearest 50 kr (not up, not nearest 100).
- The index table can be updated via admin API; the new table takes effect for plans created on
  or after 1 January of the relevant year; existing plans retain their original table version.
- The lifecycle state machine enforces all transitions in the table above; invalid transitions
  return HTTP 409.
- Misligholdelse detection correctly sends notice and transitions to MISLIGHOLT after the notice
  period without manual caseworker intervention.
- Konkret betalingsevnevurdering rejects requests for debtors below lavindkomstgrænse (HTTP 422).
- Budgetskema input is persisted; konkret ydelse is stored alongside the tabeltræk reference ydelse.
- Kulanceaftale creation requires a non-empty justification text (HTTP 422 if missing).
- Virksomhed (igangværende) creation requires an evidence reference (HTTP 422 if missing).
- The sagsbehandler portal displays afdragsordning list and detail views with state, fordringer,
  ydelse, and next payment date.
- `afbryderForaeldelse: false` is included in all afdragsordning API responses.
- No CPR, CVR, or personal name appears in the afdragsordning database tables or API responses.
- All lifecycle events are logged to CLS with actor, timestamp, state transition, and reason.
- All new i18n keys are present in both `messages_da.properties` and `messages_en_GB.properties`.
- `behave --dry-run` passes on `petitions/petition061-afdragsordninger.feature`.
