# Petition 062: Lønindeholdelse — fuld G.A.3.1.2-komplient specifikation

## Summary

Lønindeholdelse (wage garnishment) is the primary mandatory collection measure under PSRM once an afdragsordning has failed or is unavailable. This petition extends petition 007 (basic lønindeholdelse trigger) to cover the complete G.A.3.1.2 surface, including:

- **Lønindeholdelsesprocent calculation** (Gæld.bekendtg. § 14, stk. 2): A deterministic statutory formula applied to the debtor's annual net income, skattefradrag (fradragsbeløb), withholding rate (trækprocent), and tabeltræk-derived afdragsprocent. Incorrect calculation leads to under- or over-withholding, with direct regulatory and legal consequences.
- **eSkattekortet dispatch**: The calculated lønindeholdelsesprocent must be submitted to the digital tax card system (eSkattekortet) so the employer withholds the correct percentage from the debtor's A-indkomst.
- **Varsel → afgørelse chain**: The four-stage procedure (varsel, afgørelse, betalingsevnevurdering, ændring) is tightly coupled to the forældelsesfrist. Varsel alone does NOT afbryde forældelsesfrist (G.A.2.4.4.1.2). The afgørelse-underretning — when it reaches the debtor — DOES afbryde forældelsesfrist for all fordringer listed.
- **Interaction with afdragsordning (P061)**: An active afdragsordning suspends lønindeholdelse (i bero); misligholdelse triggers resumption requiring a new afgørelse.
- **Interaction with forældelse (P059)**: The afgørelse's underretning delivery timestamp is the controlling event for forældelsesbrud; it must be recorded precisely and included in the immutable audit trail.

**Extends:** Petition 007 (inddrivelsesskridt — basic lønindeholdelse trigger and lifecycle states).
**References:** Petition 059 (forældelse — lønindeholdelse afbryder forældelsesfrist), Petition 061
(afdragsordninger — suspension model and betalingsevnevurdering service), Petition 072 (Catala
companion — lønindeholdelsesprocent formula encoding, Tier A).

---

## Context and motivation

Under GIL § 10 and Gæld.bekendtg. §§ 11 and 14, lønindeholdelse is PSRM's primary coercive collection
measure. When a debtor has eligible A-indkomst and an eligible fordring exists, PSRM must calculate the
correct lønindeholdelsesprocent and notify the employer via eSkattekortet. Failure to implement the
calculation formula correctly directly causes regulatory non-compliance: over-withholding violates debtor
protection rules; under-withholding fails to recover the mandated afdragsbeløb.

The current implementation (petition 007) models the basic inddrivelsesskridt entity for lønindeholdelse
— lifecycle states, case visibility, and the initiation trigger — but does not implement:

- The statutory lønindeholdelsesprocent formula (Gæld.bekendtg. § 14, stk. 2)
- Frikort handling and the reduceret lønindeholdelsesprocent (GIL § 10a)
- The eSkattekortet dispatch integration
- The full four-stage varsel → afgørelse → betalingsevnevurdering → ændring procedure
- The precise linkage between afgørelsens underretning and the forældelsesbrud event
- Tværgående lønindeholdelse across multiple employers (G.A.3.1.2.1.2)
- Suspension mechanics when an afdragsordning is active (P061 dependency)

---

## Legal basis / G.A. references

| Reference | Content |
|---|---|
| G.A.3.1.2 | Lønindeholdelse — full procedure and eligibility rules |
| G.A.3.1.2.1.2 | Tværgående lønindeholdelse — multi-employer coordination via eSkattekortet |
| G.A.3.1.2.2 | Eligible fordring types — offentligretlige fordringer; civilretlige generally excluded unless eksekutionsfundament exists |
| G.A.3.1.2.3 | Eligible income types — A-indkomst; tværgående |
| G.A.3.1.2.4 | Four-stage procedure: varsel → afgørelse → betalingsevnevurdering → ændring |
| G.A.3.1.2.4.1 | Varsel — required content, debtor response deadline; does NOT afbryde forældelsesfrist |
| G.A.3.1.2.4.2 | Afgørelse — required content (fordringens art og størrelse); underretning via Digital Post; afbryder forældelsesfrist |
| G.A.3.1.2.4.3 | Betalingsevnevurdering — concurrent reduction to matched rate; ægtefælle/samlever consideration |
| G.A.3.1.2.4.3.2 | Ægtefælle/samlever indkomst included in betalingsevnevurdering |
| G.A.3.1.2.4.3.3–4 | Which income is included vs excluded in betalingsevnevurdering |
| G.A.3.1.2.4.4 | Ændring — new afgørelse required when debtor income changes significantly; new forældelsesbrud |
| G.A.3.1.2.4.5 | Henstand for botilbudsbrugere — out of scope for P062 automation |
| G.A.3.1.2.5 | Lønindeholdelsesprocenten — full calculation formula (Gæld.bekendtg. § 14, stk. 2) |
| G.A.2.4.4.1.2 | Varsel afbryder IKKE forældelsesfrist (confirmed) |
| G.A.2.3.2.4 | Dækningsrækkefølge — interaction with tværgående lønindeholdelse |
| GIL § 10 | Lønindeholdelse — legal authority and scope |
| GIL § 10a | Reduceret lønindeholdelsesprocent for low-income debtors after fradrag |
| Gæld.bekendtg. § 11, stk. 1 | Tabeltræk — afdragsprocent lookup by annual net income bracket |
| Gæld.bekendtg. § 14, stk. 2 | Lønindeholdelsesprocent formula |
| Forældelsesl. §§ 18–19 | Interruption and suspension of forældelsesfrist |
| Digital Post obligation | Afgørelse delivered via Digital Post (borger.dk / NemDigitalPost) |
| SKM2009.7.SKAT | Administrative practice for lønindeholdelse eligibility |
| SKM2015.718.ØLR | Court precedent confirming lønindeholdelsesprocent must be rounded down |

G.A. snapshot: v3.16 (2026-03-28).

---

## PSRM Reference Context

In PSRM, lønindeholdelse is modelled as an inddrivelsesskridt subtype established in petition 007. The
full P062 implementation extends this entity and introduces the following PSRM components:

- **LoenindeholdelseProcent**: A calculated percentage derived from the debtor's eSkattekortet data
  (annual net income, fradragsbeløb, trækprocent) and the tabeltræk afdragsprocent. Dispatched to
  eSkattekortet so the employer withholds the correct amount from A-indkomst.

- **Varsel**: The first formal step. Contains the fordringer covered, the proposed
  lønindeholdelsesprocent, and the debtor's right to object or request an afdragsordning. Dispatched via
  Digital Post. Does NOT afbryde forældelsesfrist (G.A.2.4.4.1.2).

- **Afgørelse**: Issued after the varsel deadline if the debtor does not respond or objects without valid
  grounds. Must include fordringens art og størrelse for each covered fordring — this content is
  mandatory for the document to constitute a valid forældelsesbrud trigger. Delivered as an underretning
  via Digital Post. When confirmed as received, it afbryder forældelsesfrist for all fordringer listed.

- **Underretning tracking**: PSRM must record the precise timestamp when the afgørelse reaches the
  debtor (via Digital Post delivery confirmation). This timestamp is the controlling event for
  forældelsesbrud.

- **eSkattekortet integration**: After the afgørelse is confirmed, PSRM dispatches the
  lønindeholdelsesprocent to eSkattekortet. The employer then withholds the specified percentage from the
  debtor's A-indkomst and remits it to PSRM.

- **Tværgående lønindeholdelse** (G.A.3.1.2.1.2): When the debtor has multiple employers, PSRM
  coordinates via eSkattekortet which employer(s) withhold and in what priority order (dækningsrækkefølge
  per G.A.2.3.2.4).

- **GDPR constraint**: eSkattekortet interaction requires the debtor's CPR number. CPR must not be
  stored in any service other than the Person Registry. CPR is retrieved at dispatch time via the Person
  Registry API and tunneled to eSkattekortet without being persisted in inddrivelse-service.

---

## Functional requirements

### FR-1: Eligible fordringer — pre-initiation validation

Before initiating a lønindeholdelse process, the system shall validate that all fordringer to be included
are eligible per G.A.3.1.2.2:

- Offentligretlige fordringer are eligible by default.
- Civilretlige fordringer are eligible only if an eksekutionsfundament (enforcement basis) exists (GIL bilag 1).

If any ineligible fordring is submitted, the initiation request shall be rejected with a structured
problem-detail response identifying the ineligible fordring(s). The sagsbehandler portal shall surface
this validation before submission and prevent the user from including ineligible fordringer.

### FR-2: Lønindeholdelsesprocent calculation (Gæld.bekendtg. § 14, stk. 2)

The system shall implement the statutory calculation formula:

```
lønindeholdelsesprocent =
    (afdragsprocent × nettoindkomst)
    ──────────────────────────────────────────────────────
    (nettoindkomst − fradragsbeløb) × (100% − trækprocent)
```

Where:
- **afdragsprocent**: Looked up from the tabeltræk table (Gæld.bekendtg. § 11, stk. 1) for the
  debtor's annual net income bracket.
- **nettoindkomst**: Annual net income from the debtor's eSkattekortet (same basis as tabeltræk lookup).
- **fradragsbeløb**: Annual skattefradrag from the debtor's skattekort.
- **trækprocent**: Withholding rate percentage from the debtor's forskudsopgørelse.

Rounding rule: The result shall always be **rounded down (floor)** to the nearest whole percentage
point. Rounding up is never permitted (SKM2015.718.ØLR).

The calculation shall use **fixed-point arithmetic** — no floating-point types (`double`, `float`).
All inputs and the result shall be recorded in the audit trail at time of calculation.

### FR-3: Frikort threshold handling

When the debtor holds a **frikort** (full tax exemption, i.e. fradragsbeløb ≥ nettoindkomst, or
trækprocent = 0%), the standard formula denominator approaches zero or produces an undefined result:

- If `(nettoindkomst − fradragsbeløb) ≤ 0`, the calculation falls back to the gross-income basis: the
  lønindeholdelsesprocent equals the afdragsprocent directly (no withholding adjustment is necessary
  because the employer does not deduct income tax).
- If the debtor's frikortbeløb is only partially consumed, the system shall calculate the residual
  taxable amount and apply the standard formula to that portion only.

Frikort status and frikortbeløb shall be retrieved from eSkattekortet at initiation time and recorded.

### FR-4: Maximum lønindeholdelsesprocent and reduceret rate enforcement

The calculated lønindeholdelsesprocent shall not exceed the statutory maximum defined under GIL § 10a.
If the formula result exceeds the maximum, the result shall be **capped at the statutory maximum**. The
capping shall be recorded as a separate audit event noting both the formula-derived value and the applied
maximum.

When a reduceret lønindeholdelsesprocent applies (GIL § 10a — debtor income after fradrag is within a
low-income threshold), the system shall apply the reduced rate and record the legal basis for the
reduction in the audit trail.

### FR-5: eSkattekortet dispatch

After a lønindeholdelsesafgørelse is confirmed (underretning received by debtor), the system shall
dispatch the lønindeholdelsesprocent to eSkattekortet:

- The dispatch message shall include: the calculated percentage, the debtor identifier (CPR retrieved
  from Person Registry at dispatch time — not persisted), effective date, and the fordringer covered.
- The dispatch shall be **idempotent**: repeated retries on network failure shall not create duplicate
  eSkattekortet entries. An idempotency key derived from the afgørelse UUID shall be included.
- A successful dispatch confirmation shall be recorded as an event in the audit trail.
- A failed dispatch shall trigger automatic retry (configurable retry policy). On final failure, the
  case shall be placed in a caseworker alert queue.

For tværgående lønindeholdelse, dispatch coordinates across multiple employers per FR-11.

### FR-6: Varsel generation and tracking

The system shall generate a varsel document when a caseworker initiates lønindeholdelse:

- **Required content** (G.A.3.1.2.4.1): fordringer covered (art og størrelse), proposed
  lønindeholdelsesprocent, the debtor's right to object, the debtor's right to request an
  afdragsordning, and the response deadline.
- The varsel shall be dispatched via Digital Post.
- The system shall record: varsel sent timestamp, debtor response deadline, and debtor response (or
  non-response after the deadline).
- **Forældelsesfrist**: The varsel event shall **not** generate a forældelsesbrud record (G.A.2.4.4.1.2).

### FR-7: Afgørelse generation and Digital Post dispatch

If the debtor does not respond within the deadline, or objects without valid grounds, the system shall
generate a lønindeholdelsesafgørelse:

- **Required content** (G.A.3.1.2.4.2): fordringens art og størrelse for each fordring, the
  determined lønindeholdelsesprocent, legal basis (GIL § 10, Gæld.bekendtg. § 14), and the debtor's
  right to klage.
- The afgørelse shall be dispatched via Digital Post.
- The system shall track dispatch status: sent, awaiting confirmation, confirmed (underretning reached
  debtor).

### FR-8: Underretning tracking and forældelsesbrud event

The system shall track when and whether the afgørelse-underretning reached the debtor:

- When Digital Post confirms delivery, the system shall record a **forældelsesbrud event** for every
  fordring listed in the afgørelsen (forældelsesl. § 18, stk. 1).
- Each forældelsesbrud event shall record: fordring UUID, afgørelse reference, underretning confirmation
  timestamp, and the new forældelsesfrist start date.
- If the debtor's Digital Post inbox is not active, the system shall follow the Digital Post fallback
  procedure (physical letter) and record that delivery timestamp as the controlling event.
- Varsel delivery shall never generate a forældelsesbrud event.

### FR-9: Betalingsevnevurdering concurrent support

When the debtor submits a budgetskema requesting a betalingsevnevurdering during an active
lønindeholdelse:

- The system shall invoke the shared betalingsevnevurdering service (P061) to calculate the debtor's
  konkrete betalingsevne.
- If the konkrete afdragsbeløb is less than the lønindeholdelsesbeløb, the lønindeholdelsesprocent may
  be **reduced** to match the konkrete rate. A new afgørelse with the reduced percentage shall be issued.
- Ægtefælle/samlever indkomst shall be considered per G.A.3.1.2.4.3.2.
- Lønindeholdelse remains **active** (not i bero) during the betalingsevnevurdering assessment period.
- The assessment period and any resulting percentage change shall be recorded as audit events.

### FR-10: Ændring workflow

When the debtor's income changes significantly (caseworker-triggered, or system-detected via new
eSkattekortet data):

- The system shall recalculate the lønindeholdelsesprocent using the updated eSkattekortet data.
- A new afgørelse shall be generated and dispatched via Digital Post.
- Delivery of the new afgørelse-underretning generates a **new forældelsesbrud event** for all fordringer
  in scope (G.A.3.1.2.4.4).
- The audit trail shall record: prior percentage, triggering income change event, new formula inputs,
  new percentage, and the new afgørelse reference.

### FR-11: Tværgående lønindeholdelse (multi-employer coordination)

When the debtor has multiple employers (G.A.3.1.2.1.2):

- The system shall support tværgående lønindeholdelse, dispatching to multiple employers via eSkattekortet.
- Dækningsrækkefølge across employers shall follow G.A.2.3.2.4 (primary employer withholds first;
  secondary employers withhold the residual amount up to the total lønindeholdelsesbeløb).
- Each employer dispatch shall be individually tracked; failures per employer shall be independently
  retried.
- The **combined total** lønindeholdelsesprocent across all employers shall not exceed the statutory
  maximum (FR-4).

### FR-12: Lønindeholdelse in bero and new forældelsesfrist

When a lønindeholdelse enters **i bero** status (e.g. active afdragsordning per P061):

- The i bero event, start date, and reason shall be recorded.
- If i bero lasts **1 year**, the system shall record a new forældelsesfrist start event for the
  fordringer in scope (forældelsesl. § 19, stk. 3). The 1-year threshold date shall be pre-computed
  and tracked.

When an **afdragsordning misligholdelse** is registered (P061), the system shall notify the caseworker
that a new afgørelse is required to resume lønindeholdelse. Automatic resumption without a new afgørelse
is **not permitted**.

### FR-13: API endpoints

The following REST endpoints shall be exposed on `inddrivelse-service`:

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/loenindeholdelse` | Initiate lønindeholdelse for one or more fordringer |
| `PUT` | `/api/v1/loenindeholdelse/{id}` | Update lønindeholdelse (ændring or betalingsevnevurdering result) |
| `GET` | `/api/v1/loenindeholdelse/{id}` | Query status: state, current percentage, event history, fordringer |
| `POST` | `/api/v1/loenindeholdelse/{id}/afgoerelse/confirm` | Record underretning confirmation; triggers forældelsesbrud |

All endpoints require the `CASEWORKER` or `SUPERVISOR` role. The `POST /initiate` endpoint validates
eligible fordringer before accepting the request (FR-1). API responses use structured OpenAPI 3.1 schemas.

### FR-14: Sagsbehandler portal UI

The sagsbehandler portal shall provide a full lønindeholdelse workflow:

- **Initiate**: Fordring selection (eligible types only — ineligible types shown as disabled with reason),
  display of the calculated lønindeholdelsesprocent with formula breakdown, and confirmation step.
- **Varsel/afgørelse timeline**: Display of varsel sent date, debtor response deadline, debtor response
  status, afgørelse date, underretning confirmation date, and each forældelsesbrud event with controlling
  timestamp.
- **Ændring**: Income-change trigger, recalculated percentage preview, and confirmation.
- **Status badge**: Current state (varsel / afgørelse / aktiv / i-bero / afsluttet), current percentage,
  tværgående employer count where applicable.
- All UI labels shall be internationalised via Spring message bundles (DA and EN).

### FR-15: GDPR — CPR only via Person Registry

The system shall enforce the following constraints:

- No service other than Person Registry shall persist the debtor's CPR number.
- All inddrivelse-service entities reference the debtor by `person_id` (UUID).
- CPR is retrieved from Person Registry at eSkattekortet dispatch time and is not persisted after the
  dispatch call completes.
- The eSkattekortet dispatch audit event records: person_id + dispatch timestamp. CPR is not written to
  the audit log.

---

## Non-functional requirements

- **NFR-1 (Deterministic calculation):** The lønindeholdelsesprocent calculation shall produce
  bit-for-bit identical results given identical inputs. Fixed-point arithmetic is mandatory; `double`
  and `float` types are prohibited in the calculation path. All intermediate values shall be recorded
  in the audit trail.
- **NFR-2 (Audit trail completeness):** Every lifecycle event — including the exact fordringens art og
  størrelse in the afgørelse at the time of forældelsesbrud — shall be recorded immutably in the audit
  log (CLS). The audit trail covers: all formula inputs and output, all dispatch events, all
  forældelsesbrud events with controlling timestamp, and all ændring events.
- **NFR-3 (GDPR — CPR isolation):** CPR shall never be stored in inddrivelse-service. All eSkattekortet
  calls are routed via Person Registry proxy. Any storage of CPR outside Person Registry shall be
  flagged by the GDPR audit scanner.
- **NFR-4 (eSkattekortet idempotency):** Dispatch calls to eSkattekortet shall be idempotent. An
  idempotency key derived from the afgørelse UUID shall be included in every dispatch request.
- **NFR-5 (Performance):** Lønindeholdelsesprocent calculation shall complete within 100 ms (excluding
  external eSkattekortet data retrieval). Query endpoints: 95th percentile < 500 ms.

---

## Constraints

- **Formula rounding (SKM2015.718.ØLR):** Lønindeholdelsesprocent shall always be rounded DOWN. Any
  implementation rounding to the nearest integer or rounding up is non-compliant.
- **Varsel and forældelsesfrist (G.A.2.4.4.1.2):** The varsel event must not generate a forældelsesbrud
  record. Any implementation that records forældelsesbrud at varsel time is non-compliant.
- **Afgørelse content (G.A.3.1.2.4.2):** The afgørelse must include fordringens art og størrelse for
  each covered fordring. Afgørelser missing this content are not valid forældelsesbrud triggers.
- **Afdragsordning suspension (P061):** While an afdragsordning is active, lønindeholdelse shall be i
  bero. Resumption after misligholdelse requires a new afgørelse — automatic resumption is prohibited.
- **Ændring new afgørelse (G.A.3.1.2.4.4):** An ændring in lønindeholdelsesprocent requires a new
  afgørelse. The prior afgørelse cannot be amended in place.
- **Civilretlige fordringer (G.A.3.1.2.2):** Civilretlige fordringer without eksekutionsfundament must
  not be included in a lønindeholdelse. The eligibility check is mandatory before initiation.
- **Tværgående total cap:** The combined lønindeholdelsesprocent across all employers must not exceed
  the statutory maximum.
- **Henstand (G.A.3.1.2.4.5):** Persons in botilbud may receive henstand instead of lønindeholdelse.
  The system shall flag such cases for caseworker review. Automatic henstand is prohibited.

---

## Out of scope

- Henstand automation for botilbudsbrugere (G.A.3.1.2.4.5) — caseworker workflow only
- Insolvens and gældssanering handling (G.A.3.1.2.6) — separate petition
- Catala encoding of the lønindeholdelsesprocent formula — Petition 072 (P062 Catala companion, Tier A)
- Full retroactive timeline replay for prior periods — tracked in TB-038
- Automatic forældelsesbrud trigger for i bero beyond 1 year via batch job — tracked in TB-044
- Rentegodtgørelse as alternative to retroactive dækning reassignment (GIL § 18 l) — tracked in TB-039

---

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| `LoenindeholdelseProcEntberegningService` (FR-2, FR-3, FR-4) | `opendebt-inddrivelse-service/src/main/java/.../service/LoenindeholdelseProcEntberegningService.java` |
| `TabeltrekLookupService` (Gæld.bekendtg. § 11, stk. 1 table) | `opendebt-inddrivelse-service/src/main/java/.../service/TabeltrekLookupService.java` |
| `ESkattekortetClient` (FR-5, FR-11) | `opendebt-inddrivelse-service/src/main/java/.../integration/ESkattekortetClient.java` |
| `LoenindeholdelseEntity` (extended from P007, FR-6–FR-12) | `opendebt-inddrivelse-service/src/main/java/.../domain/LoenindeholdelseEntity.java` |
| `VarselEntity` / `VarselService` (FR-6) | `opendebt-inddrivelse-service/src/main/java/.../domain/VarselEntity.java` |
| `AfgoerelseEntity` / `AfgoerelseService` (FR-7, FR-8) | `opendebt-inddrivelse-service/src/main/java/.../domain/AfgoerelseEntity.java` |
| `UnderretningTrackingService` (FR-8 — forældelsesbrud) | `opendebt-inddrivelse-service/src/main/java/.../service/UnderretningTrackingService.java` |
| `BetalingsevnevurderingService` (shared with P061, FR-9) | `opendebt-inddrivelse-service/src/main/java/.../service/BetalingsevnevurderingService.java` |
| `AendringService` (FR-10) | `opendebt-inddrivelse-service/src/main/java/.../service/AendringService.java` |
| `TvaergaaendeLoenindeholdelseService` (FR-11) | `opendebt-inddrivelse-service/src/main/java/.../service/TvaergaaendeLoenindeholdelseService.java` |
| `LoenindeholdelseController` (FR-13 API endpoints) | `opendebt-inddrivelse-service/src/main/java/.../controller/LoenindeholdelseController.java` |
| Portal Thymeleaf templates (FR-14) | `opendebt-sagsbehandler-portal/src/main/resources/templates/loenindeholdelse/` |
| OpenAPI 3.1 spec (FR-13) | `opendebt-inddrivelse-service/src/main/resources/openapi/loenindeholdelse.yaml` |
| Danish message bundle additions | `opendebt-sagsbehandler-portal/src/main/resources/messages_da.properties` |
| English message bundle additions | `opendebt-sagsbehandler-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition062-loenindeholdelse-fuld-spec.feature` |
| Catala companion | Petition 072 (separate petition, Tier A) |

---

## Definition of done

- `LoenindeholdelseProcEntberegningService` passes all calculation test cases (FR-2, FR-3, FR-4),
  including frikort edge case and statutory maximum cap.
- Fixed-point arithmetic is used throughout the calculation; no `double` or `float` in the computation
  path.
- `ESkattekortetClient` dispatches with idempotency key; retries on transient failure; alerts caseworker
  queue on final failure.
- Varsel generation records sent timestamp and response deadline; no forældelsesbrud event is emitted.
- Afgørelse includes fordringens art og størrelse for every covered fordring; dispatched via Digital Post.
- `UnderretningTrackingService` records a forældelsesbrud event for every fordring in the afgørelsen when
  Digital Post confirms delivery.
- Lønindeholdelse set to i bero when active afdragsordning exists; resumption requires new afgørelse.
- Tværgående dispatch coordinates across employers per G.A.2.3.2.4; total percentage capped at statutory
  maximum.
- All API endpoints enforce `CASEWORKER`/`SUPERVISOR` role; CPR never persisted outside Person Registry.
- All new message keys present in both DA and EN message bundles.
- `behave --dry-run` passes on `petitions/petition062-loenindeholdelse-fuld-spec.feature`.
