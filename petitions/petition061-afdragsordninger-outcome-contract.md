# Petition 061 Outcome Contract

## Petition reference

**Petition 061:** Afdragsordninger — instalment plan management (G.A.3.1.1)
**Depends on:** Petition 059 (forældelse), Petition 007 (lønindeholdelse — suspension model)
**Legal basis:** GIL §§ 11, 11 stk. 1–2, 11 stk. 6, 11 stk. 11, 11a, 45; Gæld.bekendtg. §§ 11–12, chapter 7; G.A.3.1.1, G.A.3.1.1.1–3, G.A.2.4
**G.A. snapshot version:** v3.16 (2026-03-28)

---

## Observable outcomes by functional requirement

### FR-1: Tabeltræk calculation engine (GIL § 11, stk. 1–2)

**Preconditions**
- A valid annual nettoindkomst figure is available for the debtor.
- The afdragsordning service has loaded the index table for the relevant calendar year.
- The `forsørgerpligt` flag is set on the debtor record.

**Trigger**
- A `CreateAfdragsordningRequest` is submitted with `method = TABELTRÆK`, supplying `person_id`,
  `annualNettoindkomst`, `forsørgerpligt`, and optionally `indexYear`.

**Expected service behaviour**
- The service looks up the afdragsprocent for the income bracket and forsørgerpligt status from
  the index table for the effective year.
- Annual betalingsevne = afdragsprocent × annualNettoindkomst.
- Monthly unrounded ydelse = annual_betalingsevne / 12.
- Monthly ydelse = `floor(unrounded / 50) × 50` (rounded DOWN to nearest 50 kr).
- If annualNettoindkomst < lavindkomstgrænse for the effective year: service returns
  HTTP 422 with error `BELOW_LAVINDKOMSTGRAENSE`; no afdragsordning is created.
- The response body includes: `afdragsprocent`, `annualBetalingsevne`, `monthlyYdelseUnrounded`,
  `monthlyYdelse` (rounded), and `indexYear`.
- All inputs and the computed values are persisted on the `afdragsordning` entity.

**Example (reference values for test fixtures)**

| Input | Value |
|---|---|
| Annual nettoindkomst | 250,000 kr |
| Forsørgerpligt | false |
| Afdragsprocent (bracket) | 13% |
| Annual betalingsevne | 32,500 kr |
| Monthly ydelse (unrounded) | 2,708.33 kr |
| Monthly ydelse (rounded DOWN to nearest 50 kr) | 2,700 kr |

| Input | Value |
|---|---|
| Annual nettoindkomst | 250,000 kr |
| Forsørgerpligt | true |
| Afdragsprocent (bracket) | 10% |
| Annual betalingsevne | 25,000 kr |
| Monthly ydelse (unrounded) | 2,083.33 kr |
| Monthly ydelse (rounded DOWN to nearest 50 kr) | 2,050 kr |

**Failure conditions (FR-1)**
- Afdragsprocent is looked up for wrong income bracket or wrong forsørgerpligt column.
- Monthly ydelse is rounded up or rounded to nearest (not down).
- Monthly ydelse is not a multiple of 50.
- Income below lavindkomstgrænse is accepted and an afdragsordning is created.
- Calculation inputs or results are not persisted on the afdragsordning entity.

---

### FR-2: Yearly index table management (GIL § 45)

**Preconditions**
- The service has an admin API endpoint for loading a new year's index table.
- The service stores historical index tables keyed by calendar year.

**Trigger**
- An admin loads a new index table via `POST /admin/index-tables` before 1 January of the
  new year, specifying the effective year and the bracket data.

**Expected service behaviour**
- The new table is stored and marked as effective from 1 January of the specified year.
- Calculations for afdragsordninger created before 1 January continue to use the table that was
  in force at creation time.
- Calculations for afdragsordninger created on or after 1 January use the new table.
- Existing AKTIV afdragsordninger are NOT automatically recalculated; recalculation requires an
  explicit ændring event with income evidence.
- `GET /admin/index-tables/{year}` returns the full bracket table for the specified year.
- `GET /afdragsordning/{id}` response includes `indexYear` used for this plan's calculation.

**Failure conditions (FR-2)**
- New brackets take effect immediately for existing plans without explicit ændring.
- Historical tables are not stored; old index years cannot be retrieved.
- The API returns the current table for a plan created in a previous year.

---

### FR-3: Afdragsordning lifecycle management (G.A.3.1.1)

**Preconditions**
- A fordring is under inddrivelse.
- The debtor has a `person_id` in the system (referenced from Person Registry).

**Trigger**
- A `CreateAfdragsordningRequest` is submitted and accepted.

**Expected service behaviour (state transitions)**
- New afdragsordning is created in state `OPRETTET`.
- Caseworker confirmation (or debtor acceptance) transitions to `AKTIV`.
- On AKTIV:
  - Any existing lønindeholdelsesafgørelse for the same debtor is suspended (event recorded in
    lønindeholdelse service); exception: tvangsbøder fordringer coexist.
  - The afdragsordning lists all included fordringer with amounts and priorities.
- Caseworker annullation from AKTIV transitions to `ANNULLERET`; justification is required.
- Full payment of all included fordringer transitions AKTIV → `AFVIKLET` automatically.
- All state transitions are written to the `lifecycle_event` table: actor, timestamp, from_state,
  to_state, reason.

**Trigger (additional fordring)**
- Caseworker submits `AddFordringToAfdragsordningRequest` for an AKTIV afdragsordning.

**Expected service behaviour (additional fordring)**
- The fordring is added to the included list.
- If the total outstanding amount changes materially, the ydelse is recalculated (tabeltræk or
  konkret, using the same method as the original plan).
- An `YDELSE_AENDRET` lifecycle event is recorded if the ydelse changes.

**Failure conditions (FR-3)**
- Illegal state transitions are not rejected (e.g., ANNULLERET → AKTIV is accepted).
- Lønindeholdelse is not suspended when afdragsordning transitions to AKTIV.
- State transition events are not persisted (no actor, no timestamp, no reason).
- Adding a fordring to an ANNULLERET or MISLIGHOLT plan is accepted without error.

---

### FR-4: Misligholdelse detection (G.A.3.1.1)

**Preconditions**
- An afdragsordning is in state AKTIV.
- The expected ydelse payment date has passed and no payment has been registered.

**Trigger**
- Scheduled job runs (daily or weekly) and checks payment status for all AKTIV afdragsordninger.

**Expected service behaviour**
- When a ydelse payment is missed: a `misligholdelsesvarsel` is sent to the debtor via Digital Post.
  The varsel is recorded on the afdragsordning entity with the sent timestamp.
- After the statutory notice period elapses without payment: the afdragsordning transitions to
  `MISLIGHOLT`. The transition event is recorded.
- On MISLIGHOLT: the suspension of lønindeholdelse is lifted. The lønindeholdelse service
  (petition 062) is notified via an internal event. Lønindeholdelse may resume at caseworker
  discretion.
- Caseworker can reinstate to AKTIV from MISLIGHOLT; reinstatement requires a caseworker note
  (HTTP 422 if the note is empty).
- A caseworker may manually register a missed payment to trigger the notice period without
  waiting for the scheduled job.

**Failure conditions (FR-4)**
- Misligholdelsesvarsel is not sent when a payment is missed.
- Afdragsordning does not transition to MISLIGHOLT after the notice period.
- Lønindeholdelse suspension is not lifted on MISLIGHOLT.
- Reinstatement to AKTIV is accepted without a caseworker note.

---

### FR-5: Konkret betalingsevnevurdering (GIL § 11, stk. 6 / Gæld.bekendtg. chapter 7)

**Preconditions**
- The debtor's annual nettoindkomst is at or above the lavindkomstgrænse.

**Trigger**
- A `CreateAfdragsordningRequest` is submitted with `method = KONKRET` and a `BudgetskemaDto`
  containing monthly income, monthly expenses, and number of dependents.

**Expected service behaviour**
- The service validates that `annualNettoindkomst >= lavindkomstgraense`; otherwise HTTP 422 with
  error `BELOW_LAVINDKOMSTGRAENSE_KONKRET_NOT_AVAILABLE`.
- The service computes monthly disposable income = monthly_income − monthly_expenses per
  Gæld.bekendtg. chapter 7 rules.
- The konkret monthly ydelse is compared to the tabeltræk reference ydelse for the same debtor.
- If konkret ydelse < tabeltræk ydelse: the caseworker is informed and may adopt the konkret amount.
- The adopted ydelse and the method (`TABELTRÆK` or `KONKRET`) are recorded on the afdragsordning.
- The full budgetskema inputs and the tabeltræk reference ydelse are stored for audit.

**Failure conditions (FR-5)**
- Konkret betalingsevnevurdering is accepted for a debtor below lavindkomstgrænse.
- Budgetskema inputs are not stored; audit trail is incomplete.
- The tabeltræk reference ydelse is not computed or stored alongside the konkret ydelse.
- The service automatically adopts the konkret ydelse without caseworker confirmation.

---

### FR-6: Kulanceaftale workflow (GIL § 11, stk. 11)

**Preconditions**
- Caseworker holds the `SAGSBEHANDLER` role with kulanceaftale permission.

**Trigger**
- Caseworker submits a `CreateKulanceaftaleRequest` with proposed monthly ydelse and justification text.

**Expected service behaviour**
- The service validates that `justification` is non-empty (HTTP 422 if missing or blank).
- The kulanceaftale is stored as an afdragsordning with `type = KULANCE`, the manually entered
  ydelse, and the justification text.
- No tabeltræk or konkret calculation is performed.
- The caseworker identity is recorded on the kulanceaftale entity.
- The kulanceaftale is subject to the same lifecycle state machine as other afdragsordninger.

**Failure conditions (FR-6)**
- Kulanceaftale is accepted without a justification text.
- The system performs a tabeltræk or konkret calculation and overrides the caseworker's ydelse.
- The caseworker identity is not recorded.

---

### FR-7: Virksomhed afdragsordning (G.A.3.1.1.2)

**Preconditions (igangværende virksomhed)**
- The debtor entity is a registered virksomhed (referenced via `org_id` from Person Registry).
- The virksomhed is currently active (igangværende).

**Trigger**
- Caseworker submits a `CreateAfdragsordningRequest` with `entityType = VIRKSOMHED_IGANGVAERENDE`
  and an `evidenceReference` (link to uploaded bank statements or accountant certificate).

**Expected service behaviour (igangværende virksomhed)**
- The service validates that `evidenceReference` is present (HTTP 422 if missing).
- Tabeltræk does not apply; the ydelse is based on the documented cash surplus from the evidence.
- The caseworker manually enters the proposed ydelse based on the evidence.
- The evidence reference and the caseworker-entered ydelse are stored on the afdragsordning.

**Expected service behaviour (afmeldt virksomhed)**
- A `CreateAfdragsordningRequest` with `entityType = VIRKSOMHED_AFMELDT` is processed like a
  private person: tabeltræk or konkret applies.
- No evidence reference is required; the same income validation rules apply.

**Failure conditions (FR-7)**
- Igangværende virksomhed afdragsordning is accepted without an evidence reference.
- Tabeltræk is applied to an igangværende virksomhed.
- Afmeldt virksomhed is rejected when tabeltræk would otherwise apply.

---

### FR-8: Sagsbehandler portal UI

**Preconditions**
- Caseworker holds role `SAGSBEHANDLER` with afdragsordning read/write permission.

**Trigger**
- Caseworker navigates to the afdragsordning section of the sagsbehandler portal.

**Expected portal behaviour (list view)**
- The portal displays a list of afdragsordninger for a given debtor or case, showing lifecycle
  state, monthly ydelse, and next payment date for each.
- Filtering by state (AKTIV, MISLIGHOLT, etc.) is supported.

**Expected portal behaviour (detail view)**
- The detail view shows: lifecycle state, included fordringer with balances, monthly ydelse, next
  ydelse date, payment history, the calculation method used (TABELTRÆK / KONKRET / KULANCE), and
  the tabeltræk reference values (afdragsprocent, index year).
- The debtor's CPR number is displayed (resolved at runtime from Person Registry via `person_id`;
  not stored in portal DB).

**Expected portal behaviour (create)**
- The create form collects the relevant inputs for TABELTRÆK, KONKRET, or KULANCE and submits to
  the backend API.
- Validation errors from the backend are surfaced to the caseworker.

**Failure conditions (FR-8)**
- The detail view does not show the calculation method or afdragsprocent.
- CPR number is fetched from the afdragsordning DB instead of Person Registry.
- Payment history is absent from the detail view.
- The create form accepts input for an invalid method without displaying an error.

---

### FR-9: Forældelse interaction (G.A.2.4)

**Preconditions**
- An afdragsordning is created for a fordring whose forældelsesfrist is running.

**Trigger**
- `GET /afdragsordning/{id}` or any lifecycle event on the afdragsordning.

**Expected service behaviour**
- The API response for any afdragsordning includes `"afbryderForaeldelse": false`.
- The lifecycle event log does not record an `AFBRYDELSE` event on the forældelse entity when an
  afdragsordning is created or maintained.
- When a MISLIGHOLT event fires and the lønindeholdelse service is notified, the prescription
  interaction is handled exclusively by the lønindeholdelse service (petition 059/062), not here.

**Failure conditions (FR-9)**
- The API response omits `afbryderForaeldelse` or sets it to `true`.
- An `AFBRYDELSE` event is recorded on the forældelse entity when an afdragsordning is created.

---

## Acceptance criteria

1. For annual nettoindkomst 250,000 kr without forsørgerpligt and afdragsprocent 13%, the service
   computes monthly ydelse = 2,700 kr (rounded DOWN from 2,708.33 kr to nearest 50 kr). (FR-1)

2. For annual nettoindkomst 250,000 kr with forsørgerpligt and afdragsprocent 10%, the service
   computes monthly ydelse = 2,050 kr (rounded DOWN from 2,083.33 kr to nearest 50 kr). (FR-1)

3. For the same income, a debtor with forsørgerpligt receives a lower afdragsprocent than a debtor
   without forsørgerpligt, resulting in a lower monthly ydelse. (FR-1)

4. For annual nettoindkomst below the lavindkomstgrænse, the tabeltræk engine returns 0 kr/month
   and the CreateAfdragsordningRequest is rejected with HTTP 422 and error code
   `BELOW_LAVINDKOMSTGRAENSE`. (FR-1)

5. The tabeltræk engine is deterministic: the same (nettoindkomst, forsørgerpligt, indexYear)
   inputs always return the same monthly ydelse. (FR-1 / NFR-1)

6. Monthly ydelse is always a multiple of 50 and always ≤ the unrounded value. (FR-1)

7. A new index table loaded via admin API for year Y+1 is applied to plans created on or after
   1 January of year Y+1; plans created before that date retain the table for year Y. (FR-2)

8. Existing AKTIV afdragsordninger are not automatically recalculated when a new index table is
   loaded. (FR-2)

9. An afdragsordning starts in state OPRETTET and transitions to AKTIV on caseworker confirmation;
   invalid transitions (e.g., ANNULLERET → AKTIV) are rejected with HTTP 409. (FR-3)

10. On transition to AKTIV, an existing lønindeholdelsesafgørelse for the same debtor is suspended
    (except tvangsbøder); the suspension event is recorded. (FR-3)

11. Adding a fordring to an AKTIV afdragsordning succeeds; adding to ANNULLERET or MISLIGHOLT
    fails with HTTP 409. (FR-3)

12. Misligholdelsesvarsel is sent and recorded when a payment is missed; after the notice period,
    the afdragsordning transitions to MISLIGHOLT. (FR-4)

13. On MISLIGHOLT, lønindeholdelse suspension is lifted and the lønindeholdelse service is notified.
    (FR-4)

14. Caseworker reinstatement from MISLIGHOLT to AKTIV requires a non-empty note; HTTP 422 if
    note is absent. (FR-4)

15. Konkret betalingsevnevurdering is rejected with HTTP 422 for debtors below lavindkomstgrænse;
    the error code is `BELOW_LAVINDKOMSTGRAENSE_KONKRET_NOT_AVAILABLE`. (FR-5)

16. Budgetskema inputs and the tabeltræk reference ydelse are both stored on the afdragsordning
    entity when method = KONKRET. (FR-5)

17. Kulanceaftale creation is rejected with HTTP 422 if the justification text is absent or blank.
    (FR-6)

18. No tabeltræk or konkret calculation is triggered for a kulanceaftale; the caseworker-entered
    ydelse is stored as-is. (FR-6)

19. Igangværende virksomhed afdragsordning is rejected with HTTP 422 if evidenceReference is
    absent; afmeldt virksomhed is processed with the standard tabeltræk flow. (FR-7)

20. Sagsbehandler portal detail view displays state, included fordringer, monthly ydelse, next
    payment date, payment history, and calculation method (TABELTRÆK / KONKRET / KULANCE). (FR-8)

21. `afbryderForaeldelse: false` is present in every afdragsordning API response. (FR-9)

22. All lifecycle events are logged to CLS with actor, timestamp, from_state, to_state, and
    reason. (NFR-2)

23. No CPR, CVR, name, or address appears in the afdragsordning database or API responses; all
    person references are `person_id` (UUID). (NFR-3)

---

## Definition of done

- All 23 acceptance criteria above are satisfied.
- `TabeltræksUdregningService` unit tests cover at least five bracket-representative cases for
  each forsørgerpligt value, including boundary values at and just above/below lavindkomstgrænse.
- Integration tests cover all lifecycle state transitions (valid and invalid).
- Misligholdelse detection is tested with a scheduled-job integration test that verifies varsel
  dispatch and state transition timing.
- The OpenAPI spec for the afdragsordning endpoints is present and passes schema validation.
- Database migration `V061__afdragsordninger.sql` applies cleanly on a fresh schema and does not
  break existing migrations.
- No CPR or personal data field appears in any DB migration or entity class.
- `behave --dry-run` passes on `petitions/petition061-afdragsordninger.feature`.
- All i18n keys referenced by the sagsbehandler portal are present in both DA and EN bundles.

---

## Failure conditions (summary)

- Monthly ydelse is rounded up or rounded to nearest instead of rounded DOWN.
- Monthly ydelse is not a multiple of 50.
- Income below lavindkomstgrænse results in an afdragsordning being created (tabeltræk path).
- Debtor below lavindkomstgrænse can request konkret betalingsevnevurdering.
- Illegal state transitions are accepted (e.g., ANNULLERET → AKTIV).
- Lønindeholdelse is not suspended when afdragsordning transitions to AKTIV.
- Misligholdelsesvarsel is not sent on missed payment.
- MISLIGHOLT transition does not fire after the notice period.
- Kulanceaftale is accepted without a justification text.
- Igangværende virksomhed afdragsordning is accepted without an evidence reference.
- Tabeltræk is applied to an igangværende virksomhed.
- `afbryderForaeldelse` is missing from or set to `true` in the API response.
- CPR or personal data is stored in the afdragsordning service database.
- Lifecycle events are not logged to CLS.
- `behave --dry-run` fails on the feature file.
