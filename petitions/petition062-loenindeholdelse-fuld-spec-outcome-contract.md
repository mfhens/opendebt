# Petition 062 Outcome Contract

## Petition reference

**Petition 062:** Lønindeholdelse — fuld G.A.3.1.2-komplient specifikation
**Extends:** Petition 007 (basic inddrivelsesskridt lifecycle — implemented)
**Depends on:** Petition 059 (forældelse model), Petition 061 (afdragsordning suspension model,
shared betalingsevnevurdering service)
**Legal basis:** G.A.3.1.2 (v3.16 2026-03-28), GIL §§ 10, 10a; Gæld.bekendtg. §§ 11, 14;
Forældelsesl. §§ 18–19; SKM2015.718.ØLR

---

## Observable outcomes by functional requirement

### FR-1: Eligible fordringer — pre-initiation validation
*(G.A.3.1.2.2 / GIL bilag 1)*

**Preconditions**
- Caseworker holds role `CASEWORKER` or `SUPERVISOR`.
- A set of fordringer is proposed for inclusion in a new lønindeholdelse.

**Trigger**
- `POST /api/v1/loenindeholdelse` is submitted with a list of fordring UUIDs.

**Expected backend behaviour**
- The system validates each fordring against the eligibility rules: offentligretlige fordringer are
  accepted; civilretlige fordringer are accepted only when an eksekutionsfundament is recorded on the
  fordring.
- If all fordringer are eligible, the request proceeds to calculation.
- If any fordring is ineligible, the request is rejected with HTTP 422 and a structured problem-detail
  body identifying the ineligible fordring(s) by UUID and reason code.

**Expected portal behaviour**
- The initiation form shows only eligible fordringer as selectable. Ineligible fordringer are shown as
  disabled with a tooltip citing the ineligibility reason (no eksekutionsfundament).
- The portal does not submit ineligible fordringer to the API.

---

### FR-2: Lønindeholdelsesprocent calculation
*(Gæld.bekendtg. § 14, stk. 2 / SKM2015.718.ØLR)*

**Preconditions**
- Fordringer have passed FR-1 eligibility validation.
- eSkattekortet data is available for the debtor: nettoindkomst, fradragsbeløb, trækprocent.
- tabeltræk lookup yields afdragsprocent for the debtor's income bracket.

**Trigger**
- The system performs the lønindeholdelsesprocent calculation as part of `POST /api/v1/loenindeholdelse`
  or during an ændring recalculation.

**Expected calculation behaviour**
- The formula `(afdragsprocent × nettoindkomst) / ((nettoindkomst − fradragsbeløb) × (100% − trækprocent))`
  is evaluated using fixed-point arithmetic.
- The result is **rounded down** (floor) to the nearest whole percentage point. No rounding up.
- The calculation is deterministic: identical inputs always produce the identical output.

**Expected audit behaviour**
- The audit trail records all formula inputs (afdragsprocent, nettoindkomst, fradragsbeløb, trækprocent)
  and the resulting lønindeholdelsesprocent at the time of each calculation.

---

### FR-3: Frikort threshold handling
*(GIL § 10a / G.A.3.1.2.5)*

**Preconditions**
- The debtor's eSkattekortet indicates frikort status (fradragsbeløb ≥ nettoindkomst, or trækprocent = 0%).

**Trigger**
- The system evaluates whether the standard formula denominator reaches zero during FR-2 calculation.

**Expected calculation behaviour**
- If `(nettoindkomst − fradragsbeløb) ≤ 0`, the calculation falls back to the gross-income basis:
  lønindeholdelsesprocent = afdragsprocent (no withholding adjustment required).
- The fallback basis and frikort status are recorded in the audit trail.

---

### FR-4: Statutory maximum and reduceret rate enforcement
*(GIL § 10a)*

**Preconditions**
- The formula result from FR-2 (or FR-3) is available.

**Trigger**
- The system checks the formula result against the statutory maximum.

**Expected enforcement behaviour**
- If the formula result exceeds the statutory maximum, the applied percentage is capped at the maximum.
- A cap event is recorded in the audit trail with both the formula-derived value and the applied maximum.
- If the reduceret lønindeholdelsesprocent applies (low income after fradrag within the GIL § 10a
  threshold), the reduced rate is applied and the legal basis is recorded.

---

### FR-5: eSkattekortet dispatch
*(G.A.3.1.2.5 / GDPR)*

**Preconditions**
- A lønindeholdelsesafgørelse has been confirmed (underretning received by debtor).

**Trigger**
- The system initiates eSkattekortet dispatch after underretning confirmation.

**Expected dispatch behaviour**
- The dispatch message includes: the lønindeholdelsesprocent, effective date, and covered fordringer.
- CPR is retrieved from Person Registry at dispatch time and is not persisted after the call.
- The dispatch is idempotent: retries using the same afgørelse UUID idempotency key do not create
  duplicate eSkattekortet entries.
- A successful dispatch is recorded as an event in the audit trail (person_id + dispatch timestamp,
  no CPR in log).
- A failed dispatch triggers automatic retry. On final failure, the case is placed in a caseworker
  alert queue.

---

### FR-6: Varsel generation and tracking
*(G.A.3.1.2.4.1 / G.A.2.4.4.1.2)*

**Preconditions**
- Caseworker initiates lønindeholdelse via the portal or API.
- Eligible fordringer are confirmed (FR-1).
- Lønindeholdelsesprocent is calculated (FR-2–FR-4).

**Trigger**
- Caseworker confirms initiation; system generates varsel.

**Expected varsel behaviour**
- The varsel document contains: fordringer covered (art og størrelse), proposed
  lønindeholdelsesprocent, debtor rights statement (object / request afdragsordning), and response
  deadline.
- The varsel is dispatched via Digital Post.
- The system records: varsel sent timestamp, response deadline, and subsequent debtor response (or
  non-response after deadline).

**Expected forældelsesfrist behaviour**
- No forældelsesbrud event is created when the varsel is dispatched or when it is delivered
  (G.A.2.4.4.1.2).

---

### FR-7: Afgørelse generation and Digital Post dispatch
*(G.A.3.1.2.4.2)*

**Preconditions**
- Varsel deadline has passed and debtor has not responded, or debtor has objected without valid grounds.

**Trigger**
- Caseworker or system issues a lønindeholdelsesafgørelse.

**Expected afgørelse behaviour**
- The afgørelse contains: fordringens art og størrelse for each covered fordring, the determined
  lønindeholdelsesprocent, legal basis (GIL § 10, Gæld.bekendtg. § 14), and the debtor's klage right.
- The afgørelse is dispatched via Digital Post.
- The system tracks dispatch status: sent, awaiting confirmation, confirmed.

**Expected backend behaviour**
- If fordringens art og størrelse is absent from the afgørelse content, the document is rejected with
  HTTP 422 before dispatch.

---

### FR-8: Underretning tracking and forældelsesbrud event
*(G.A.3.1.2.4.2 / Forældelsesl. § 18, stk. 1)*

**Preconditions**
- A lønindeholdelsesafgørelse has been dispatched via Digital Post.

**Trigger**
- Digital Post returns a delivery confirmation (underretning confirmed).

**Expected forældelsesbrud behaviour**
- The system records a forældelsesbrud event for **every fordring** listed in the afgørelsen.
- Each event records: fordring UUID, afgørelse reference, underretning confirmation timestamp, and the
  new forældelsesfrist start date.
- When varsel is delivered (not afgørelse), no forældelsesbrud event is created.

---

### FR-9: Betalingsevnevurdering concurrent support
*(G.A.3.1.2.4.3)*

**Preconditions**
- Lønindeholdelse is aktiv.
- Debtor has submitted a budgetskema.

**Trigger**
- Caseworker triggers betalingsevnevurdering via the API or portal.

**Expected behaviour**
- The system calls the shared betalingsevnevurdering service (P061) and obtains the konkrete
  afdragsbeløb.
- If the konkrete beløb is less than the lønindeholdelsesbeløb, a new afgørelse with a reduced
  percentage is generated (FR-7).
- Lønindeholdelse remains aktiv (not i bero) during the assessment period.
- Ægtefælle/samlever indkomst is included in the assessment per G.A.3.1.2.4.3.2.
- All assessment inputs and results are recorded in the audit trail.

---

### FR-10: Ændring workflow
*(G.A.3.1.2.4.4)*

**Preconditions**
- Lønindeholdelse is aktiv.
- A significant income change has been registered (caseworker trigger or new eSkattekortet data).

**Trigger**
- Caseworker submits `PUT /api/v1/loenindeholdelse/{id}` with updated income data.

**Expected ændring behaviour**
- The system recalculates the lønindeholdelsesprocent using updated eSkattekortet data.
- A new afgørelse is generated with the updated percentage.
- Delivery of the new afgørelse-underretning generates a new forældelsesbrud event for all in-scope
  fordringer.
- The audit trail records: prior percentage, triggering income change event, new formula inputs, new
  percentage, and new afgørelse reference.

---

### FR-11: Tværgående lønindeholdelse
*(G.A.3.1.2.1.2 / G.A.2.3.2.4)*

**Preconditions**
- Debtor has A-indkomst from multiple employers.
- Lønindeholdelse has been initiated and afgørelse is confirmed.

**Trigger**
- System dispatches lønindeholdelsesprocent to eSkattekortet across multiple employers.

**Expected tværgående behaviour**
- Dækningsrækkefølge: primary employer withholds first; secondary employers withhold the residual up to
  the total lønindeholdelsesbeløb (G.A.2.3.2.4).
- Each employer dispatch is individually tracked; failures per employer are independently retried.
- The combined total lønindeholdelsesprocent across all employers does not exceed the statutory maximum
  (FR-4).

---

### FR-12: Lønindeholdelse in bero and new forældelsesfrist
*(Forældelsesl. § 19, stk. 3 / P061 interaction)*

**Preconditions**
- Active afdragsordning is registered for the debtor (P061), causing lønindeholdelse to be set to i bero.

**Trigger — afdragsordning active:**
- Lønindeholdelse transitions to i bero state when afdragsordning becomes active.

**Expected i bero behaviour**
- The i bero event, start date, and reason (active afdragsordning) are recorded.
- If i bero lasts 1 year, the system records a new forældelsesfrist start event for the in-scope
  fordringer.

**Trigger — afdragsordning misligholdelse:**
- P061 registers afdragsordning misligholdelse.

**Expected resumption behaviour**
- The system notifies the caseworker queue that a new afgørelse is required to resume lønindeholdelse.
- Lønindeholdelse does NOT resume automatically without a new afgørelse.

---

### FR-13: API endpoints
*(OpenAPI 3.1)*

**Preconditions**
- Caller is authenticated with `CASEWORKER` or `SUPERVISOR` role.

**Trigger**
- Any HTTP request to the lønindeholdelse endpoints.

**Expected API behaviour**
- `POST /api/v1/loenindeholdelse`: validates eligible fordringer (FR-1), calculates percentage
  (FR-2–FR-4), creates lønindeholdelse case, returns case UUID and calculated percentage.
- `PUT /api/v1/loenindeholdelse/{id}`: recalculates (FR-10) or applies betalingsevnevurdering (FR-9);
  returns updated percentage and new afgørelse reference.
- `GET /api/v1/loenindeholdelse/{id}`: returns current state (varsel / afgørelse / aktiv / i-bero /
  afsluttet), current percentage, full event history, and fordringer in scope.
- `POST /api/v1/loenindeholdelse/{id}/afgoerelse/confirm`: records underretning confirmation, emits
  forældelsesbrud events (FR-8).
- Unauthenticated or unauthorised requests return HTTP 401 or 403 respectively.

---

### FR-14: Sagsbehandler portal UI
*(Spring MVC / Thymeleaf)*

**Preconditions**
- Caseworker is authenticated with `CASEWORKER` or `SUPERVISOR` role.

**Trigger**
- Caseworker navigates to the lønindeholdelse section for a sag.

**Expected portal behaviour**
- Initiate form: only eligible fordringer are selectable; formula breakdown is displayed before
  confirmation.
- Varsel/afgørelse timeline: shows sent dates, response deadline, debtor response, underretning
  confirmation, and each forældelsesbrud event with timestamp.
- Ændring form: displays updated percentage preview before caseworker confirms.
- Status badge reflects current state accurately and updates after each event.

---

### FR-15: GDPR — CPR isolation
*(GDPR Art. 25 / project GDPR constraint)*

**Preconditions**
- Any operation on inddrivelse-service that requires debtor identification.

**Trigger**
- Any write or read operation involving debtor data in inddrivelse-service.

**Expected GDPR behaviour**
- No CPR number is stored in inddrivelse-service entities or logs.
- eSkattekortet dispatch calls retrieve CPR from Person Registry at runtime and do not persist it.
- The dispatch audit event records person_id + dispatch timestamp only.

---

## Acceptance criteria

**AC-1 (FR-1):** A lønindeholdelse initiation request including an eligible offentligretlig fordring
is accepted by the backend; the request proceeds to calculation.

**AC-2 (FR-1):** A lønindeholdelse initiation request including a civilretlig fordring without
eksekutionsfundament is rejected with HTTP 422; the response body identifies the ineligible fordring by
UUID.

**AC-3 (FR-2):** Given nettoindkomst = 400 000 DKK, fradragsbeløb = 48 000 DKK, trækprocent = 37%,
afdragsprocent = 10%, the system calculates lønindeholdelsesprocent = 18% (floor of 18.04%).

**AC-4 (FR-2):** The calculation is deterministic: submitting the same inputs twice always yields the
identical result.

**AC-5 (FR-2):** The calculated lønindeholdelsesprocent is always rounded **down** (floor). A result
of 18.99% yields 18%, not 19%.

**AC-6 (FR-2):** All formula inputs and the result are recorded in the audit trail at calculation time.

**AC-7 (FR-3):** When the debtor has frikort (fradragsbeløb ≥ nettoindkomst), the calculation falls
back to the gross-income basis; the lønindeholdelsesprocent equals the afdragsprocent directly without
withholding adjustment.

**AC-8 (FR-4):** When the formula result exceeds the statutory maximum, the applied percentage is
capped; a cap audit event records both the formula-derived value and the applied maximum.

**AC-9 (FR-5):** After underretning confirmation, eSkattekortet dispatch is triggered. The dispatch
message includes the correct percentage and effective date. CPR is retrieved from Person Registry and
not persisted after the dispatch call.

**AC-10 (FR-5):** Retrying the eSkattekortet dispatch with the same afgørelse UUID idempotency key
does not create a duplicate eSkattekortet entry.

**AC-11 (FR-6):** A varsel generated for a new lønindeholdelse contains fordringens art og størrelse,
the proposed percentage, the debtor rights statement, and the response deadline.

**AC-12 (FR-6):** Dispatching the varsel via Digital Post does **not** create a forældelsesbrud event
for any of the covered fordringer.

**AC-13 (FR-7):** An afgørelse generated after the varsel deadline contains fordringens art og størrelse
for every covered fordring, the determined percentage, and the debtor klage right.

**AC-14 (FR-7):** A lønindeholdelsesafgørelse missing fordringens art og størrelse is rejected by the
backend with HTTP 422 before Digital Post dispatch.

**AC-15 (FR-8):** When Digital Post confirms underretning delivery, the system records a forældelsesbrud
event for every fordring listed in the afgørelsen.

**AC-16 (FR-8):** Each forældelsesbrud event records the fordring UUID, afgørelse reference, underretning
confirmation timestamp, and the new forældelsesfrist start date.

**AC-17 (FR-9):** When a betalingsevnevurdering yields a konkrete afdragsbeløb lower than the active
lønindeholdelsesbeløb, a new afgørelse with a reduced percentage is issued. Lønindeholdelse remains
aktiv (not i bero) during the assessment.

**AC-18 (FR-10):** An ændring recalculation produces a new afgørelse with the updated percentage;
delivery of this afgørelse-underretning creates a new forældelsesbrud event for all in-scope fordringer.

**AC-19 (FR-11):** For a debtor with two employers, tværgående dispatch sends separate eSkattekortet
messages per employer following the G.A.2.3.2.4 dækningsrækkefølge. The combined percentage does not
exceed the statutory maximum.

**AC-20 (FR-12):** When an afdragsordning becomes active (P061), the lønindeholdelse transitions to i
bero state; the state change is recorded with start date and reason.

**AC-21 (FR-12):** When afdragsordning misligholdelse is registered (P061), the system places a
caseworker alert; lønindeholdelse does NOT resume automatically. A new afgørelse is required.

**AC-22 (FR-12):** When i bero has lasted exactly 1 year, the system records a new forældelsesfrist
start event for the in-scope fordringer.

**AC-23 (FR-13):** `GET /api/v1/loenindeholdelse/{id}` returns: current state, current percentage,
event history including varsel/afgørelse/forældelsesbrud events, and fordringer in scope.

**AC-24 (FR-14):** The sagsbehandler portal displays the varsel/afgørelse timeline with sent dates,
debtor response status, underretning confirmation date, and forældelsesbrud event timestamps.

**AC-25 (FR-15):** No CPR number is present in any inddrivelse-service database column, audit log
entry, or API response. eSkattekortet dispatch audit events contain only person_id and timestamp.

---

## Definition of done

- `LoenindeholdelseProcEntberegningService` passes all calculation tests including frikort edge case and
  statutory maximum cap; fixed-point arithmetic throughout.
- AC-3 (exact numeric result) verified by a unit test with the concrete values from the Gherkin scenario.
- Varsel generation emits no forældelsesbrud event (AC-12 verified by unit + BDD test).
- Afgørelse dispatch is gated on presence of fordringens art og størrelse (AC-14 verified by backend
  validation test).
- UnderretningTrackingService emits forældelsesbrud for every fordring in the afgørelsen on Digital Post
  confirmation (AC-15–AC-16).
- eSkattekortet client is idempotent on retry; CPR not in audit log (AC-10, AC-25).
- i bero transition on active afdragsordning; no automatic resumption after misligholdelse (AC-20–AC-21).
- 1-year i bero threshold triggers new forældelsesfrist start event (AC-22).
- All new message keys present in both DA and EN message bundles.
- All API endpoints return HTTP 401/403 for missing/insufficient auth.
- `behave --dry-run` passes on `petitions/petition062-loenindeholdelse-fuld-spec.feature`.

---

## Success metrics

| Metric | Target |
|--------|--------|
| Correct formula result (AC-3 example) | 100% deterministic |
| Rounding always floor | 100% |
| Frikort fallback correctly applied | 100% |
| Statutory maximum cap applied when exceeded | 100% |
| Varsel emits no forældelsesbrud event | 100% |
| Afgørelse missing art og størrelse rejected before dispatch | 100% |
| ForældelsesbrudEvent created for every fordring on underretning | 100% |
| eSkattekortet dispatch idempotent | 100% |
| CPR absent from all inddrivelse-service logs and DB columns | 100% |
| Tværgående dispatch respects dækningsrækkefølge | 100% |
| i bero on active afdragsordning; no auto-resumption | 100% |
| 1-year i bero triggers new forældelsesfrist start | 100% |
| New i18n keys in DA and EN message bundles | All new keys |

---

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| `LoenindeholdelseProcEntberegningService` | `opendebt-inddrivelse-service/src/main/java/.../service/LoenindeholdelseProcEntberegningService.java` |
| `TabeltrekLookupService` | `opendebt-inddrivelse-service/src/main/java/.../service/TabeltrekLookupService.java` |
| `ESkattekortetClient` | `opendebt-inddrivelse-service/src/main/java/.../integration/ESkattekortetClient.java` |
| `LoenindeholdelseEntity` | `opendebt-inddrivelse-service/src/main/java/.../domain/LoenindeholdelseEntity.java` |
| `VarselEntity` / `VarselService` | `opendebt-inddrivelse-service/src/main/java/.../domain/VarselEntity.java` |
| `AfgoerelseEntity` / `AfgoerelseService` | `opendebt-inddrivelse-service/src/main/java/.../domain/AfgoerelseEntity.java` |
| `UnderretningTrackingService` | `opendebt-inddrivelse-service/src/main/java/.../service/UnderretningTrackingService.java` |
| `BetalingsevnevurderingService` (shared with P061) | `opendebt-inddrivelse-service/src/main/java/.../service/BetalingsevnevurderingService.java` |
| `AendringService` | `opendebt-inddrivelse-service/src/main/java/.../service/AendringService.java` |
| `TvaergaaendeLoenindeholdelseService` | `opendebt-inddrivelse-service/src/main/java/.../service/TvaergaaendeLoenindeholdelseService.java` |
| `LoenindeholdelseController` | `opendebt-inddrivelse-service/src/main/java/.../controller/LoenindeholdelseController.java` |
| Portal Thymeleaf templates | `opendebt-sagsbehandler-portal/src/main/resources/templates/loenindeholdelse/` |
| OpenAPI 3.1 spec | `opendebt-inddrivelse-service/src/main/resources/openapi/loenindeholdelse.yaml` |
| Danish message bundle additions | `opendebt-sagsbehandler-portal/src/main/resources/messages_da.properties` |
| English message bundle additions | `opendebt-sagsbehandler-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition062-loenindeholdelse-fuld-spec.feature` |

---

## Failure conditions

- The lønindeholdelsesprocent formula uses floating-point arithmetic (`double`/`float`), risking rounding
  artefacts.
- The formula result is rounded to the nearest integer (or rounded up) instead of always down.
- Frikort status does not trigger the gross-income fallback; division by zero or NaN occurs.
- The statutory maximum cap is not applied when the formula result exceeds it.
- The varsel dispatch creates a forældelsesbrud event for any covered fordring.
- A lønindeholdelsesafgørelse is dispatched via Digital Post without fordringens art og størrelse.
- The backend accepts a lønindeholdelsesafgørelse request missing fordringens art og størrelse.
- The underretning confirmation does not create a forældelsesbrud event for any covered fordring.
- CPR is stored in any inddrivelse-service database column, log entry, or API response payload.
- eSkattekortet dispatch is not idempotent; a retry creates a duplicate entry in eSkattekortet.
- Lønindeholdelse resumes automatically after afdragsordning misligholdelse without a new afgørelse.
- An ændring does not generate a new afgørelse; the prior afgørelse is amended in place.
- An ændring does not generate a new forældelsesbrud event on underretning delivery.
- The combined tværgående lønindeholdelsesprocent across employers exceeds the statutory maximum.
- `behave --dry-run` fails on the feature file.
- Any new i18n message key is absent from either DA or EN message bundles.
