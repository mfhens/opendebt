# Petition 058 Outcome Contract

## Petition reference

**Petition 058:** Modregning i udbetalinger fra det offentlige + Korrektionspulje (G.A.2.3.3–2.3.4)  
**Legal basis:** GIL §§ 4 stk. 5–11, 7 stk. 1–2, 8b, 9a, 17 stk. 1; Nemkonto § 16 stk. 1;
Gæld.bekendtg. § 7 stk. 4; Kildeskattelov §§ 62, 62A; Lov om børne- og ungeydelse § 11 stk. 2  
**G.A. snapshot:** v3.16 (2026-03-28)  
**Depends on:** Petition 057 (`DaekningsRaekkefoeigenService`), TB-040 (active fordringer API)  
**Depended on by:** Petition 059 (forældelse interruption), petition 062 (pro-rata distribution)

---

## Observable outcomes by functional requirement

### FR-1: Automatic payment interception workflow

**Preconditions**
- A `PublicDisbursementEvent` has been received from Nemkonto carrying a `nemkontoReferenceId`,
  `debtorPersonId`, `disbursementAmount`, and `paymentType`.
- The debtor has active fordringer in at least one of the three tiers.
- No prior `ModregningEvent` exists for this `nemkontoReferenceId`.

**Trigger**
- The `PublicDisbursementEventConsumer` processes the event.

**Expected three-tier ordering behaviour**

Tier 1 — Fordringer under opkrævning hos den udbetalende myndighed:
- The system applies the disbursement amount to tier-1 fordringer first.
- Tier-1 fordringer are fully consumed before any amount reaches tier-2.
- If the disbursement is fully consumed by tier-1, the workflow ends; `DaekningsRaekkefoeigenService`
  is not called.

Tier 2 — Fordringer under inddrivelse hos RIM:
- Residual after tier-1 is applied to tier-2 fordringer.
- For full tier-2 coverage, each fordring receives its full outstanding amount.
- For partial coverage, `DaekningsRaekkefoeigenService` (P057) determines the GIL § 4 allocation
  across fordringer and their interest sub-positions.
- `DaekningsRaekkefoeigenService` is invoked exactly once per disbursement event for tier-2 ordering.

Tier 3 — Andre fordringer under opkrævning:
- Residual after tier-2 is applied to tier-3 fordringer in ascending registration-date order.
- Only fordringer registered in RIM's fordringregister are eligible.

Residual payout:
- If all tiers are covered and a surplus remains, the surplus is returned to the debtor via
  Nemkonto. `ModregningEvent.residualPayoutAmount` is set to the surplus value.

**Expected persistence behaviour**
- A single `ModregningEvent` is persisted for the disbursement event, with all tier amounts,
  `receiptDate`, `decisionDate`, `noticeDelivered`, `tier2WaiverApplied = false`,
  `renteGodtgoerelseNonTaxable = true`, and `klageFristDato`.
- A `SET_OFF` `CollectionMeasureEntity` referencing `ModregningEvent.id` is created for each
  fordring that receives any coverage.
- Double-entry ledger entries are generated for every coverage allocation (ADR-0018).

**Expected notification behaviour**
- `NotificationService` is invoked and a Digital Post notice is dispatched via a transactional
  outbox after the transaction commits.
- The notice lists each covered fordring and the rentegodtgørelse start date.
- `ModregningEvent.noticeDelivered` reflects the delivery outcome.

**Expected idempotency behaviour**
- Re-processing the same `nemkontoReferenceId` does not create a second `ModregningEvent` and
  returns a 200 response referencing the existing event.

**Failure conditions (FR-1)**
- A tier-2 fordring receives coverage while a tier-1 fordring for the same paying authority
  remains outstanding.
- `DaekningsRaekkefoeigenService` is called for a disbursement fully consumed by tier-1.
- A `CollectionMeasureEntity` is created without a `ModregningEvent.id` reference.
- A duplicate `ModregningEvent` is created for the same `nemkontoReferenceId`.
- The Digital Post notice is dispatched before the transaction commits (outbox violation).
- `renteGodtgoerelseNonTaxable` is absent or `false` on any `ModregningEvent`.

---

### FR-2: Modregningsrækkefølge waiver (GIL § 4, stk. 11)

**Preconditions**
- A `ModregningEvent` exists with `tier2WaiverApplied = false`.
- The caller holds OAuth2 scope `modregning:waiver`.

**Trigger**
- `POST /debtors/{debtorId}/modregning-events/{eventId}/tier2-waiver` is called with a
  valid `waiverReason` and `caseworkerId`.

**Expected waiver behaviour**
- `tier2WaiverApplied` is set to `true` on the `ModregningEvent`.
- The three-tier ordering engine re-runs for this event, skipping tier-2.
- Each `CollectionMeasureEntity` for covered fordringer carries `waiverApplied = true` and
  the `caseworkerId`.
- A CLS audit log entry is written with `gilParagraf = "GIL § 4, stk. 11"`,
  `caseworkerId`, `waiverReason`, and `modregningEventId`.

**Expected authorization behaviour**
- A caller without `modregning:waiver` scope receives HTTP 403.
- A caller with the scope receives HTTP 200 with the updated `ModregningEvent`.

**Failure conditions (FR-2)**
- A caller without `modregning:waiver` scope is permitted to submit a waiver.
- The CLS audit log entry is missing `gilParagraf = "GIL § 4, stk. 11"`.
- The ordering engine does not re-run after the waiver is applied.
- `waiverApplied = true` is absent from the `CollectionMeasureEntity` after a waiver.

---

### FR-3: Korrektionspulje management

**Preconditions**
- An `OffsettingReversalEvent` has been emitted from P053, identifying a previously offset
  fordring that has been written down or cancelled.
- A surplus amount exists after the write-down.

**Trigger**
- The `OffsettingReversalEventConsumer` processes the event.

**Expected Step 1 behaviour — residual same-fordring coverage**
- The surplus is first applied to any uncovered portion of the same fordring (including renter
  in P057 sub-position order), per Gæld.bekendtg. § 7, stk. 4.
- If the same fordring is already fully covered, Step 1 produces no allocation.

**Expected Step 2 behaviour — gendækning**
- The remaining surplus (after Step 1) is applied to other fordringer under inddrivelse using
  the P057 `DaekningsRaekkefoeigenService` ordering.
- Fordringer covered by the same `inddrivelsesindsats` type are prioritised.
- No Digital Post notice is sent for gendækning.
- When RIM opts out of gendækning (correctionPoolTarget = `DMI`, or DMI-originated fordring,
  or retroactive partial coverage), gendækning is skipped and all surplus proceeds to Step 3.

**Expected Step 3 behaviour — KorrektionspuljeEntry**
- Surplus not consumed by Steps 1–2 is persisted as a `KorrektionspuljeEntry` with:
  `surplusAmount`, `correctionPoolTarget`, `originEventId`, `boerneYdelseRestriction`, and
  `renteGodtgoerelseStartDate`.
- `boerneYdelseRestriction = true` if the original disbursement `paymentType` was
  `BOERNE_OG_UNGEYDELSE`.

**Expected settlement behaviour — monthly job**
- Pool entries with `surplusAmount ≥ 50.00 DKK` are settled in the monthly
  `KorrektionspuljeSettlementJob` run.
- Pool entries with `surplusAmount < 50.00 DKK` are skipped in monthly runs and settled only
  in the annual run.
- At settlement: the surplus + accrued `renteGodtgoerelseAccrued` is treated as a new
  independent Nemkonto payment and processed through FR-1 (three-tier modregning).
- Transporter/udlæg restrictions from the original payment do NOT transfer to the settled
  amount (except for transporter notified before 1 October 2021).
- A `KorrektionspuljeEntry` with `boerneYdelseRestriction = true` retains the børne-og-
  ungeydelse restriction after settlement — it is NOT treated as unrestricted.

**Failure conditions (FR-3)**
- A `KorrektionspuljeEntry` is created for surplus amounts that could have been gendækket.
- A pool entry with `surplusAmount = 45.00 DKK` is settled in the monthly run.
- `boerneYdelseRestriction = true` is not preserved after pool settlement.
- Transporter/udlæg restrictions from the original payment apply to the settled amount
  (where no pre-1-October-2021 transporter is present).
- `renteGodtgoerelseStartDate` is missing from a `KorrektionspuljeEntry`.
- Gendækning is performed without delegating to `DaekningsRaekkefoeigenService` (P057).

---

### FR-4: Rentegodtgørelse computation

**Preconditions**
- A `ModregningEvent` has been created with a `receiptDate`.
- `BusinessConfigService` key `rentelov.refRate` is configured with the current semi-annual
  reference rate.

**Trigger**
- `RenteGodtgoerelseService.computeStartDate(event)` and
  `RenteGodtgoerelseService.computeRate(referenceDate)` are called during FR-1 processing.

**Expected standard-case behaviour**
- Rate = `BusinessConfigService.get("rentelov.refRate")` MINUS 4.0 percentage points,
  floored at 0 %.
- Start date = 1st of the calendar month immediately following `receiptDate`.
- Both rate and start date are recorded on the `ModregningEvent`.

**Expected 5-banking-day exception behaviour**
- If `decisionDate` is within 5 banking days of `receiptDate` (inclusive, using
  `DanishBankingCalendar`), `renteGodtgoerelseAccrued` = 0.00 DKK and no start date is
  recorded on the `ModregningEvent` for this event.

**Expected kildeskattelov § 62/62A exception behaviour**
- For `paymentType = OVERSKYDENDE_SKAT`, `renteGodtgoerelseStartDate` is set to
  September 1st of the year after `indkomstAar`, regardless of `receiptDate`.
- If the standard start date (1st of following month) is LATER than 1 September year+1,
  the standard date applies.

**Expected rate-change-effective-date behaviour**
- When a new `rentelov.refRate` is published, the new rate takes effect 5 banking days after
  the publication date. Accruals before that effective date use the prior rate.

**Failure conditions (FR-4)**
- `renteGodtgoerelseAccrued > 0.00 DKK` when the decision was within 5 banking days of receipt.
- A `OVERSKYDENDE_SKAT` event starts rentegodtgørelse before 1 September year+1.
- The rate includes DMI-era rates or does not subtract 4 percentage points from the reference.
- `renteGodtgoerelseNonTaxable` is `false` or absent on any event.
- The rate changes take effect immediately on publication rather than 5 banking days later.

---

### FR-5: Klage (appeal) deadline tracking

**Preconditions**
- A `ModregningEvent` has been persisted with `noticeDelivered` set (true or false).

**Trigger**
- `klageFristDato` is computed at modregning decision time and stored on the `ModregningEvent`.

**Expected klage deadline computation**
- `noticeDelivered = true`: `klageFristDato` = notice delivery date + 3 calendar months.
- `noticeDelivered = false`: `klageFristDato` = `decisionDate` + 1 calendar year.

**Expected read-model API behaviour**
- `GET /debtors/{debtorId}/modregning-events` returns HTTP 200 with a paginated array.
- Each entry in the array contains: `eventId`, `decisionDate`, `totalOffsetAmount`,
  `tier1Amount`, `tier2Amount`, `tier3Amount`, `residualPayoutAmount`, `klageFristDato`,
  `noticeDelivered`, `tier2WaiverApplied`.
- Returns HTTP 404 if the debtor does not exist.
- Returns HTTP 403 if the caller lacks access to the debtor.

**Expected caseworker portal behaviour**
- Events with `klageFristDato` within 14 calendar days are displayed with an amber indicator.
- Events with `klageFristDato` in the past are displayed with a red indicator.
- Events with `klageFristDato` more than 14 days away have no special indicator.

**Failure conditions (FR-5)**
- `klageFristDato` is computed as 3 months from `decisionDate` when `noticeDelivered = true`
  (instead of from the notice delivery date).
- `klageFristDato` is absent from any `ModregningEvent`.
- `GET /debtors/{id}/modregning-events` omits `klageFristDato` from any response entry.
- The portal does not highlight events within 14 days.

---

## Acceptance criteria

**AC-1:** A `PublicDisbursementEvent` with disbursement amount 10 000 DKK against a debtor
with 3 000 DKK tier-1, 5 000 DKK tier-2, and 4 000 DKK tier-3 fordringer produces:
`tier1Amount = 3 000`, `tier2Amount = 5 000`, `tier3Amount = 2 000`,
`residualPayoutAmount = 0` on the resulting `ModregningEvent`, with all three tiers covered
before residual (FR-1). *(Gherkin scenario: tre-tier modregning — blandet dækning)*

**AC-2:** A `PublicDisbursementEvent` fully consumed by tier-1 fordringer does not result
in a call to `DaekningsRaekkefoeigenService` and produces `tier2Amount = 0`,
`tier3Amount = 0` (FR-1). *(Gherkin scenario: tier-1 fuld dækning)*

**AC-3:** When tier-2 fordringer are only partially coverable by the remaining disbursement
residual, `DaekningsRaekkefoeigenService` is called once with the residual amount and the
GIL § 4 allocation is applied across fordringer and their interest sub-positions (FR-1).
*(Gherkin scenario: tier-2 delvis dækning — P057 delegering)*

**AC-4:** A `SET_OFF` `CollectionMeasureEntity` referencing `ModregningEvent.id` is persisted
for every fordring that receives any coverage (FR-1). *(Gherkin scenario: SET_OFF measure
per covered fordring)*

**AC-5:** Re-processing the same `nemkontoReferenceId` does not create a second
`ModregningEvent`; the system returns HTTP 200 referencing the existing event (FR-1,
NFR-4). *(Gherkin scenario: idempotent genbehandling — duplikat nemkontoReferenceId)*

**AC-6:** A caseworker with `modregning:waiver` scope successfully applies a tier-2 waiver;
the ordering engine re-runs skipping tier-2; the CLS audit log entry carries
`gilParagraf = "GIL § 4, stk. 11"` (FR-2). *(Gherkin scenario: sagsbehandler fravælger
tier-2 prioritet)*

**AC-7:** A caller without `modregning:waiver` scope receives HTTP 403 on the waiver
endpoint (FR-2). *(Gherkin scenario: manglende waiver-scope giver 403)*

**AC-8:** An `OffsettingReversalEvent` surplus is first applied to the same fordring's
uncovered portion; remaining surplus is gendækket against other fordringer; only the
non-gendækket remainder becomes a `KorrektionspuljeEntry` (FR-3). *(Gherkin scenario:
gendækning efter fordring-nedskrivning)*

**AC-9:** A `KorrektionspuljeEntry` with `surplusAmount = 45.00 DKK` is NOT settled in
the monthly `KorrektionspuljeSettlementJob` run and IS settled in the annual run (FR-3).
*(Gherkin scenario: korrektionspulje beløb under 50 DKK — kun årsafregning)*

**AC-10:** At monthly settlement, the settled `KorrektionspuljeEntry` amount is re-applied
via FR-1 (three-tier modregning), without transporter/udlæg restrictions from the original
payment (FR-3). *(Gherkin scenario: korrektionspulje månedlig afregning — Nemkonto
udbetaling)*

**AC-11:** A `KorrektionspuljeEntry` with `boerneYdelseRestriction = true` retains the
børne-og-ungeydelse restriction after monthly settlement and is NOT treated as an
unrestricted Nemkonto payment (FR-3). *(Gherkin scenario: børne-og-ungeydelse
begrænsning bevaret efter korrektionspulje)*

**AC-12:** When `decisionDate` is within 5 banking days of `receiptDate`,
`renteGodtgoerelseAccrued = 0.00 DKK` and no rentegodtgørelse start date is recorded
(FR-4). *(Gherkin scenario: 5-bankdags-undtagelse — ingen rentegodtgørelse)*

**AC-13:** For a `paymentType = OVERSKYDENDE_SKAT` event with `indkomstAar = 2024`,
`renteGodtgoerelseStartDate = 2025-09-01`, regardless of `receiptDate` (FR-4).
*(Gherkin scenario: kildeskattelov § 62 særlig startdato)*

**AC-14:** `renteGodtgoerelseNonTaxable = true` is set on every `ModregningEvent`, and the
Digital Post notice informs the debtor accordingly (FR-4, NFR-3). *(Implicit across all FR-1
scenarios — verified by field presence check in each scenario)*

**AC-15:** When `noticeDelivered = true` on delivery date 2025-03-15,
`klageFristDato = 2025-06-15`; when `noticeDelivered = false` and
`decisionDate = 2025-03-15`, `klageFristDato = 2026-03-15` (FR-5). *(Gherkin scenario:
klage-deadline registreres korrekt på modregning-event)*

**AC-16:** `GET /debtors/{debtorId}/modregning-events` returns HTTP 200 with all required
fields including `klageFristDato`, `noticeDelivered`, and `tier2WaiverApplied` (FR-5).
*(Gherkin scenario: klage-deadline registreres korrekt på modregning-event)*

**AC-17:** The caseworker portal highlights a `klageFristDato` 10 days away with an amber
indicator (FR-5). *(Gherkin scenario: klage-deadline registreres korrekt på modregning-event)*

**AC-18:** All new i18n labels for modregning tier labels, korrektionspulje status labels,
and klage-deadline indicators are present in both `messages_da.properties` and
`messages_en_GB.properties`.
> **Verification method:** AC-18 is verified by the CI bundle-lint check (build fails if
> any key present in `messages_da.properties` is absent from `messages_en_GB.properties`
> and vice versa), not by a Gherkin scenario.

---

## Deliverables table with AC cross-references

| Deliverable | AC coverage |
|-------------|-------------|
| `ModregningService` | AC-1 through AC-5 |
| `ModregningsRaekkefoeigenEngine` | AC-1, AC-2, AC-3, AC-6 |
| `RenteGodtgoerelseService` | AC-12, AC-13, AC-14 |
| `KorrektionspuljeService` | AC-8, AC-9, AC-11 |
| `KorrektionspuljeSettlementJob` | AC-9, AC-10, AC-11 |
| `ModregningEvent` entity | AC-1 through AC-5, AC-12 through AC-16 |
| `KorrektionspuljeEntry` entity | AC-8, AC-9, AC-10, AC-11 |
| `PublicDisbursementEventConsumer` | AC-1 through AC-5 |
| `OffsettingReversalEventConsumer` | AC-8 |
| `ModregningController` (waiver + read model) | AC-6, AC-7, AC-15, AC-16 |
| OpenAPI spec | AC-6, AC-7, AC-15, AC-16 |
| Sagsbehandler portal view template | AC-17 |
| Sagsbehandler portal view controller | AC-17 |
| Digital Post notice template | AC-14 |
| Liquibase migrations | All ACs (underlying persistence) |
| DA + EN i18n message bundles | AC-18 |

---

## Failure conditions (summary)

- A tier-2 fordring receives coverage while a tier-1 fordring for the same paying authority remains outstanding.
- `DaekningsRaekkefoeigenService` is called for a disbursement fully consumed by tier-1.
- A `CollectionMeasureEntity` is created without a `ModregningEvent.id` reference.
- A duplicate `ModregningEvent` is created for the same `nemkontoReferenceId`.
- The Digital Post notice is dispatched before the database transaction commits.
- `renteGodtgoerelseNonTaxable` is `false` or absent on any `ModregningEvent`.
- A caseworker without `modregning:waiver` scope can submit a tier-2 waiver.
- The CLS audit log entry for a waiver is missing `gilParagraf = "GIL § 4, stk. 11"`.
- Gendækning is performed without delegating to `DaekningsRaekkefoeigenService` (P057).
- A pool entry with `surplusAmount = 45.00 DKK` is settled in the monthly job run.
- `boerneYdelseRestriction = true` is not preserved after pool settlement.
- Transporter/udlæg restrictions apply to a settled korrektionspulje amount (absent pre-2021 transporter).
- `renteGodtgoerelseAccrued > 0.00 DKK` when the decision was within 5 banking days of receipt.
- A `OVERSKYDENDE_SKAT` event starts rentegodtgørelse before 1 September year+1.
- `klageFristDato` is computed from `decisionDate` when `noticeDelivered = true` (instead of delivery date).
- `klageFristDato` is absent from any `ModregningEvent` or API response entry.
- The caseworker portal does not highlight events within 14 days with an amber indicator.
- Any AC-18 i18n key is missing from the DA or EN message bundle.
- `behave --dry-run` fails on the feature file.

---

## Definition of done (outcome-contract view)

- The three-tier modregningsrækkefølge engine produces a deterministic, auditable
  `ModregningEvent` for every `PublicDisbursementEvent` it processes.
- Every coverage allocation has a corresponding `SET_OFF` `CollectionMeasureEntity` and
  double-entry ledger entries.
- `RenteGodtgoerelseService` correctly applies the 5-banking-day exception and the
  kildeskattelov § 62/62A exception; `renteGodtgoerelseNonTaxable = true` on all events.
- The korrektionspulje workflow correctly sequences Steps 1–3; the settlement job enforces
  the 50 DKK threshold and the børne-og-ungeydelse restriction.
- `klageFristDato` is correctly computed for both notice-delivered and notice-failed paths
  on every `ModregningEvent`.
- All acceptance criteria AC-1 through AC-17 are covered by at least one Gherkin scenario.
- AC-18 is verified by the CI bundle-lint check.
- `behave --dry-run` passes on `petitions/petition058-modregning-korrektionspulje.feature`.
