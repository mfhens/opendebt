# Petition 058: Modregning i udbetalinger fra det offentlige + Korrektionspulje (G.A.2.3.3–2.3.4)

## Summary

Implement the full automated set-off workflow whereby RIM (Restanceinddrivelsesmyndighed /
Gældsstyrelsen / OpenDebt) intercepts public disbursements via the Nemkonto system and applies
them against a debtor's outstanding fordringer in the three-tier modregningsrækkefølge mandated
by GIL § 7, stk. 1. Alongside modregning, implement the korrektionspulje (GIL § 4, stk. 5–10)
that handles surplus amounts arising when previously offset debts are written down or cancelled.

This petition fully specifies and delivers:

- The automatic `PublicDisbursementEvent` interception and three-tier ordering engine (FR-1)
- A caseworker waiver of tier-2 priority (GIL § 4, stk. 11) with a dedicated API (FR-2)
- Korrektionspulje lifecycle: gendækning, pool entry creation, monthly settlement, and the
  < 50 DKK annual-only exception (FR-3)
- Rentegodtgørelse computation, including the 5-banking-day exception and the kildeskattelov
  § 62/62A special start date (FR-4)
- Klage (appeal) deadline tracking on every modregning event (FR-5)

**Service owner:** `opendebt-debt-service` (ADR-0027)  
**Depends on:** Petition 057 (`DaekningsRaekkefoeigenService` — tier-2 partial coverage and
gendækning ordering), TB-040 (`GET /internal/debtors/{debtorId}/fordringer/active` — now done)  
**Depended on by:** Petition 059 (forældelse — prescription interruption by modregning),
petition 062 (pro-rata distribution — uses same tier-2 ordering)  
**G.A. snapshot:** v3.16 (2026-03-28)  
**Catala companion:** None required (Tier B — workflow and priority logic, not complex date
arithmetic)

---

## Context and motivation

Under GIL §§ 7–8b, when a public authority owes a debtor a payment (e.g. a tax refund, social
benefit, or housing subsidy), RIM has a statutory obligation to intercept that payment via the
Nemkonto system and apply it against the debtor's outstanding fordringer. This mechanism —
*modregning i udbetalinger fra det offentlige* — is one of the primary inddrivelse tools
available to RIM and must be executed in a strictly defined three-tier sequence.

Petition 007 registered the `SET_OFF` collection-measure type and introduced the
`OffsettingService` stub. That stub has no business logic. The caseworker portal has no
modregning workflow. There is no korrektionspulje implementation. Rentegodtgørelse is
unimplemented (recorded as TB-039). This petition closes all of those gaps in a single,
legally grounded specification.

### Why now?

- The `OffsettingService` stub blocks any modregning-related automation. Every public
  disbursement that arrives must currently be handled manually, creating operational risk and
  inconsistent treatment across debtors.
- Petition 059 (forældelse) requires that a modregning event correctly interrupts the
  forældelsesfrist for the covered fordringer. That interruption point is undefined until P058
  establishes the modregning decision timestamp.
- The korrektionspulje is a mandatory legal construct. Without it, surplus from reversed
  coverages has no home: it cannot be refunded immediately (the pool must settle) and it
  cannot be held indefinitely (interest accrues). Both failure modes create PSRM audit
  violations.
- Petition 053 (opskrivning/nedskrivning) delivers the write-down events that generate
  OffsettingReversalEvents — but there is no receiver for those events today.

### Domain terms

| Danish | English / technical | Definition |
|--------|---------------------|------------|
| Modregning | Set-off / offsetting | Interception of a public disbursement and application against outstanding debts |
| Modregningsrækkefølge | Set-off priority order | Three-tier legal sequence (GIL § 7, stk. 1, nr. 1–3) governing which fordringer are covered first |
| Udbetalende myndighed | Paying authority | The public body making the disbursement (tax authority, municipality, etc.) |
| Tier 1 | Priority tier 1 | Fordringer under opkrævning hos den udbetalende myndighed |
| Tier 2 | Priority tier 2 | Fordringer under inddrivelse hos RIM |
| Tier 3 | Priority tier 3 | Andre fordringer under opkrævning, in registration order in RIM's register |
| Rentegodtgørelse | Interest compensation | Interest payable to the debtor on amounts held pending modregning decision (GIL § 8b) |
| Korrektionspulje | Correction pool | Pool collecting surplus amounts from reversed coverages for deferred settlement (GIL § 4, stk. 5–10) |
| Gendækning | Re-coverage | Application of a correction-pool surplus to another fordring using P057 ordering |
| Inddrivelsesindsats | Enforcement action | A debt recovery measure type (e.g. SET_OFF, WAGE_GARNISHMENT, ATTACHMENT) |
| PublicDisbursementEvent | Public disbursement event | Inbound event from Nemkonto signalling an interceptable public payment |
| OffsettingReversalEvent | Offsetting reversal event | Internal event emitted when a previously offset fordring is written down or cancelled |
| ModregningEvent | Set-off event | The persisted record of a modregning decision, including fordringer covered, amounts, and klage deadline |
| KorrektionspuljeEntry | Correction pool entry | A pool balance record for one surplus amount awaiting settlement |
| Klage | Appeal | Administrative appeal against the modregning decision (3-month window from notice, GIL § 17) |
| Underretning | Notice | Mandatory Digital Post notification to debtor after each modregning decision (GIL § 9a) |
| Partshøring | Party hearing | Pre-decision right to be heard; not required for automated modregning (GIL § 9a, stk. 1) |
| Rentelov § 5 | Interest Act § 5 | Statutory source for the reference interest rate used to compute rentegodtgørelse |
| SET_OFF | Collection measure type | The `CollectionMeasure.measureType` value for modregning (P007) |
| correctionPoolTarget | Pool routing flag | `PSRM` or `DMI` — routes surplus to the correct correction pool (DMI routing is a flag only; not implemented) |

---

## Legal basis

| Reference | Content relevant to P058 |
|-----------|--------------------------|
| GIL § 7, stk. 1, nr. 1–3 | Three-tier modregningsrækkefølge: (1) opkrævning hos udbetalende myndighed, (2) inddrivelse hos RIM, (3) andre fordringer under opkrævning |
| GIL § 7, stk. 2, 1. pkt. | Partial tier-2 coverage: apply GIL § 4 dækningsrækkefølge (P057) |
| GIL § 4, stk. 5 | Gendækning: surplus applied to other debts under inddrivelse using GIL § 4 ordering |
| GIL § 4, stk. 6 | Opt-out from gendækning for: (a) modregning surplus with debt-under-collection, (b) DMI-originated fordringer, (c) partially covered retroactively |
| GIL § 4, stk. 7 | Korrektionspulje: non-gendækket surplus transferred; settled monthly (< 50 DKK: annually) |
| GIL § 4, stk. 7, nr. 2 | Rentegodtgørelse on korrektionspulje balance — not taxable income |
| GIL § 4, stk. 7, nr. 3 | Børne- og ungeydelse restriction preserved after korrektionspulje settlement |
| GIL § 4, stk. 7, nr. 4 | Transporter/udlæg on original payment do NOT carry over to korrektionspulje surplus |
| GIL § 4, stk. 9 | DMI korrektionspulje routing — modelled as a flag only |
| GIL § 4, stk. 10 | Gæld.bekendtg. § 7, stk. 4 — residual coverage of same-fordring before gendækning |
| GIL § 4, stk. 11 | RIM may waive tier-2 priority to allow tier-1 or tier-3 fordringer to be covered first |
| GIL § 8b | Rentegodtgørelse: rate = rentelov § 5, stk. 1+2 MINUS 4 percentage points; semi-annual publication |
| GIL § 8b, stk. 2, 3. pkt. | Rentegodtgørelse is NOT taxable income |
| GIL § 8b, stk. 4 | Debtor cannot invoke right to modregning against Nemkonto-intercepted amounts |
| GIL § 9a | Notice requirement: debtor must be informed of modregning decision via Digital Post |
| GIL § 9a, stk. 1 | No partshøring required before automated modregning |
| GIL § 17, stk. 1 | Klage deadline: 3 months from notice; 1 year from decision if notice could not be delivered |
| Kildeskattelov § 62/62A | Income tax refund: rentegodtgørelse starts not before 1 September in year after income year |
| Lov om børne- og ungeydelse § 11, stk. 2 | Special modregning restrictions on child-benefit payments |
| Nemkonto § 16, stk. 1 | Basis for automatic interception of eligible public disbursements |
| Gæld.bekendtg. § 7, stk. 4 | Step 1 of correction pool: residual coverage of same fordring (incl. renter) before gendækning |
| G.A.2.3.3 (v3.16, 2026-03-28) | G.A. specification of the full modregning procedure |
| G.A.2.3.4 (v3.16, 2026-03-28) | G.A. specification of the korrektionspulje procedure |

---

## PSRM reference context

In PSRM, when a `PublicDisbursementEvent` arrives from Nemkonto:

1. PSRM queries the debtor's active fordringer across all three tiers.
2. It applies the tier-1 amount first (udbetalende myndighed handles its own fordringer).
3. Residual is applied to tier-2 fordringer using the GIL § 4 rule engine (P057).
4. Any remaining residual is applied to tier-3 fordringer in registration order.
5. Any excess beyond all fordringer is paid out to the debtor via Nemkonto.
6. A `ModregningEvent` is persisted for the entire transaction.
7. A `SET_OFF` `CollectionMeasureEntity` is created for each covered fordring (P007 model).
8. Double-entry ledger entries are generated (ADR-0018).
9. A Digital Post notice is sent to the debtor (GIL § 9a).

When a fordring is subsequently written down or cancelled (P053), an
`OffsettingReversalEvent` is emitted. This triggers the korrektionspulje workflow:
Step 1 → gendækning of residual same-fordring, Step 2 → gendækning of other fordringer
(P057 ordering), Step 3 → create `KorrektionspuljeEntry` for any remaining surplus.

### PSRM field mappings

| OpenDebt field | PSRM concept | GIL/bekendtg. reference |
|----------------|--------------|-------------------------|
| `ModregningEvent.disbursementAmount` | Samlet tilgodehavende fra udbetalende myndighed | GIL § 7, stk. 1 |
| `ModregningEvent.paymentType` | Type af udbetaling (overskydende skat, dagpenge, etc.) | Nemkonto § 16, stk. 1 |
| `ModregningEvent.tier1Amount` | Dækning via tier-1 fordringer | GIL § 7, stk. 1, nr. 1 |
| `ModregningEvent.tier2Amount` | Dækning via RIM-inddrivelse fordringer | GIL § 7, stk. 1, nr. 2 |
| `ModregningEvent.tier3Amount` | Dækning via tier-3 fordringer | GIL § 7, stk. 1, nr. 3 |
| `ModregningEvent.residualPayoutAmount` | Restbeløb udbetalt til debitor | GIL § 7, stk. 1 (excess) |
| `ModregningEvent.renteGodtgoerelseStartDate` | Første dag for rentegodtgørelse | GIL § 8b |
| `ModregningEvent.tier2WaiverApplied` | RIM-fravalg af tier-2 prioritet | GIL § 4, stk. 11 |
| `ModregningEvent.noticeDelivered` | Underretning leveret / fejlet | GIL § 9a |
| `ModregningEvent.klageFristDato` | Klagedeadline | GIL § 17, stk. 1 |
| `KorrektionspuljeEntry.surplusAmount` | Puljeret restbeløb | GIL § 4, stk. 7 |
| `KorrektionspuljeEntry.correctionPoolTarget` | PSRM eller DMI puljeretning | GIL § 4, stk. 9 |
| `KorrektionspuljeEntry.renteGodtgoerelseAccrued` | Påløbne renter på puljekrav | GIL § 4, stk. 7, nr. 2 |
| `KorrektionspuljeEntry.boerneYdelseRestriction` | Bevaret børne-/ungeydelse-begrænsning | GIL § 4, stk. 7, nr. 3 |
| `CollectionMeasureEntity.measureType = SET_OFF` | Modregningsindsats (P007) | GIL § 7 |

---

## Functional requirements

### FR-1: Automatic payment interception workflow

When a `PublicDisbursementEvent` is received from Nemkonto, the system must execute the
complete modregning workflow atomically.

**FR-1.1 — Tier-1 fordring resolution**  
The system queries tier-1 fordringer (fordringer under opkrævning hos den udbetalende
myndighed) that have been pre-registered by the paying authority with RIM. The tier-1 amount
is deducted first. If the disbursement is fully consumed by tier-1, the transaction concludes
with no tier-2 or tier-3 processing.

**FR-1.2 — Tier-2 fordring resolution (RIM inddrivelse)**  
The residual after tier-1 is applied to fordringer under inddrivelse hos RIM. For full
coverage, each fordring is covered in full in GIL § 4 ordering (P057). For partial coverage,
`DaekningsRaekkefoeigenService` (P057) determines the allocation across fordringer and their
interest sub-positions. A `SET_OFF` `CollectionMeasureEntity` is created for each fordring
that receives any coverage.

**FR-1.3 — Tier-3 fordring resolution**  
Any residual after tier-2 is applied to tier-3 fordringer (andre fordringer under opkrævning
registered with RIM) in ascending registration order (the date the paying authority registered
the fordring with RIM). Only fordringer explicitly registered in RIM's fordringregister are
eligible.

**FR-1.4 — Residual payout**  
If all fordringer across all three tiers are fully covered and a surplus remains, the surplus
is returned to the debtor via Nemkonto. A `residualPayoutAmount` is recorded on the
`ModregningEvent`.

**FR-1.5 — Rentegodtgørelse liability start**  
At the moment RIM receives the disbursement amount, the system records `receiptDate` on the
`ModregningEvent`. Rentegodtgørelse starts from the 1st of the month following `receiptDate`,
unless the modregning decision is made within 5 banking days of `receiptDate` (FR-4 governs
the rate and exceptions).

**FR-1.6 — SET_OFF collection measure**  
For each fordring covered in whole or in part, the system creates a `CollectionMeasureEntity`
with `measureType = SET_OFF` and references the `ModregningEvent.id`.

**FR-1.7 — Double-entry ledger**  
Every coverage allocation produces a debit to the debtor's fordring account and a credit to
the receiving fordringshaver account (ADR-0018). The ledger entries reference the
`ModregningEvent.id` and the GIL § 7 tier applied.

**FR-1.8 — Digital Post notice**  
Within the same transaction window, the system invokes `NotificationService` to send a
Digital Post notice to the debtor (GIL § 9a). The notice identifies:
- the total disbursement amount intercepted,
- each fordring covered (fordringshaver, amount, fordring reference),
- the rentegodtgørelse basis and start date,
- the klage deadline (computed per FR-5).

Modregning is legally valid even if the notice cannot be delivered. If delivery fails, the
`noticeDelivered` flag on `ModregningEvent` is set to `false` and the klage deadline is
extended to 1 year (FR-5).

**FR-1.9 — ModregningResult response**  
The service returns a `ModregningResult` containing: total offset amount, list of fordringer
covered per tier (fordringId, amount covered, tier), rentegodtgørelse basis amount and start
date, residual payout amount, and klage deadline.

**Acceptance criteria (FR-1):**
- A `PublicDisbursementEvent` with amount 10 000 DKK against a debtor with 3 000 DKK in
  tier-1, 5 000 DKK in tier-2, 4 000 DKK in tier-3 produces: tier-1 fully covered (3 000),
  tier-2 fully covered (5 000), tier-3 partially covered (2 000), residual payout = 0.
- A `PublicDisbursementEvent` fully consumed by tier-1 does not invoke
  `DaekningsRaekkefoeigenService`.
- Each covered fordring has a `SET_OFF` `CollectionMeasureEntity` referencing the
  `ModregningEvent.id`.
- `ModregningEvent.noticeDelivered = false` when Digital Post delivery fails.

---

### FR-2: Modregningsrækkefølge waiver (GIL § 4, stk. 11)

RIM may, for a specific disbursement event, elect to waive tier-2 priority and allow tier-1
or tier-3 fordringer to be covered before or instead of tier-2 fordringer.

**FR-2.1 — Caseworker waiver decision API**  
Endpoint: `POST /debtors/{debtorId}/modregning-events/{eventId}/tier2-waiver`  
Body: `{ "waiverReason": "<free text>", "caseworkerId": "<UUID>" }`  
Effect: sets `tier2WaiverApplied = true` on the `ModregningEvent` and re-runs the
ordering engine, skipping tier-2 for this event. The caseworker must hold the
`modregning:waiver` OAuth2 scope.

**FR-2.2 — Waiver recorded on CollectionMeasure**  
When a waiver is applied, the `CollectionMeasureEntity` for each covered fordring carries
`waiverApplied = true` and a reference to the caseworker who approved it.

**FR-2.3 — Waiver audit log**  
The waiver decision is written to the CLS audit log with `gilParagraf = "GIL § 4, stk. 11"`,
`caseworkerId`, `waiverReason`, and the `ModregningEvent.id`.

**Acceptance criteria (FR-2):**
- A caseworker with scope `modregning:waiver` can submit a waiver; the event is re-processed
  skipping tier-2.
- A caller without `modregning:waiver` scope receives HTTP 403.
- The CLS audit log entry for the waiver carries `gilParagraf = "GIL § 4, stk. 11"`.

---

### FR-3: Korrektionspulje management

When a previously offset fordring is written down or cancelled (an `OffsettingReversalEvent`
is received from P053), the system processes the resulting surplus through the three-step
korrektionspulje workflow.

**FR-3.1 — Step 1: Residual coverage of same fordring**  
Before any gendækning, the surplus is applied to any remaining uncovered portion of the same
fordring (including its renter sub-positions in P057 order), as required by Gæld.bekendtg.
§ 7, stk. 4. If the same fordring is fully covered after this step, the remaining surplus
proceeds to FR-3.2.

**FR-3.2 — Step 2: Gendækning (re-coverage)**  
The remaining surplus is applied to other fordringer under inddrivelse using the P057
`DaekningsRaekkefoeigenService` ordering. Gendækning is limited:

- (a) fordringer covered by the same `inddrivelsesindsats` type are covered first,
  then other fordringer;
- (b) RIM may opt out of gendækning for: surplus from modregning with a debt-under-collection,
  DMI-originated fordringer (correctionPoolTarget = `DMI`), or fordringer partially covered
  retroactively (GIL § 4, stk. 6).

No Digital Post notice is required for gendækning.

**FR-3.3 — Step 3: KorrektionspuljeEntry creation**  
Surplus not consumed by gendækning is placed in a `KorrektionspuljeEntry`. The entry carries:
`surplusAmount`, `correctionPoolTarget` (`PSRM` or `DMI`), `originEventId` (the reversed
`ModregningEvent.id`), `boerneYdelseRestriction` (preserved if the original disbursement
was børne- og ungeydelse, GIL § 4, stk. 7, nr. 3), and `renteGodtgoerelseStartDate`.

**FR-3.4 — Monthly settlement job**  
A scheduled `KorrektionspuljeSettlementJob` (same pattern as `InterestAccrualJob`) runs
monthly (configurable). For each `KorrektionspuljeEntry` with `correctionPoolTarget = PSRM`:

1. If `surplusAmount < 50 DKK`: mark the entry for annual-only settlement; skip this monthly
   run.
2. Otherwise: treat the settled amount as a new independent Nemkonto payment. Invoke FR-1
   to apply the settled amount against the debtor's active fordringer (it loses its "origin
   nature" after settlement — transporter/udlæg restrictions from the original payment do NOT
   carry over, per GIL § 4, stk. 7, nr. 4, except for transporter notified before
   1 October 2021).
3. Add accrued `renteGodtgoerelseAccrued` to the settlement and clear the entry.

**FR-3.5 — Børne-og-ungeydelse restriction**  
If `boerneYdelseRestriction = true`, the settled korrektionspulje amount retains the
special modregning restrictions of lov om børne- og ungeydelse § 11, stk. 2 — it is NOT
treated as an unrestricted Nemkonto payment (exception to the general "origin-nature lost"
rule of GIL § 4, stk. 7).

**FR-3.6 — Rentegodtgørelse on pool balance**  
Rentegodtgørelse accrues on each `KorrektionspuljeEntry` from:
- `renteGodtgoerelseStartDate` = the day after the reversed `ModregningEvent`'s decision
  date, if surplus originated from a modregning reversal.
- The payment date, if surplus originated from a non-modregning collection reversal.

Rentegodtgørelse stops accruing when the pool entry is settled. After settlement, the
transferred amount enters a new forrentning period under Nemkonto § 16, stk. 1 rules
(5-banking-day window applies again — FR-4).

**Acceptance criteria (FR-3):**
- A reversed modregning event produces a `KorrektionspuljeEntry` carrying the correct
  `surplusAmount` after gendækning is exhausted.
- A pool entry with `surplusAmount = 45.00 DKK` is NOT settled in the monthly job; it is
  settled in the annual-only run.
- After settlement, the settled amount is re-applied via FR-1 without transporter/udlæg
  restrictions from the original payment.
- A pool entry with `boerneYdelseRestriction = true` does not lose its restriction after
  settlement.

---

### FR-4: Rentegodtgørelse computation

**FR-4.1 — Rate formula**  
Rate = rentelov § 5, stk. 1+2 reference rate MINUS 4 percentage points (floor at 0 %).
The reference rate is sourced from `BusinessConfigService` (same pattern as P057
`annualRatePercent`) using the key `rentelov.refRate`. Rates are published semi-annually
(1 January and 1 July). Rate changes take effect 5 banking days after the new rate is
published. The system stores a `RenteGodtgoerelseRateEntry` per validity period.

**FR-4.2 — Standard start date**  
Rentegodtgørelse starts from the 1st of the month AFTER `receiptDate`
(`ModregningEvent.receiptDate`).

**FR-4.3 — 5-banking-day exception**  
If the modregning decision is made within 5 banking days of `receiptDate`, no
rentegodtgørelse accrues for that event. The system uses the `DanishBankingCalendar` (same
service as in P057) to compute banking-day intervals.

**FR-4.4 — Kildeskattelov § 62/62A exception**  
For disbursements with `paymentType = OVERSKYDENDE_SKAT`, rentegodtgørelse does not start
before 1 September in the year following the income year encoded in the
`PublicDisbursementEvent.indkomstAar` field.

**FR-4.5 — Børne-og-ungeydelse**  
For disbursements with `paymentType = BOERNE_OG_UNGEYDELSE`, standard start-date rules
apply. The `boerneYdelseRestriction` flag on the generated `KorrektionspuljeEntry` is
set to `true`.

**FR-4.6 — Non-taxable**  
`ModregningEvent.renteGodtgoerelseNonTaxable = true` is always set. The Digital Post
notice informs the debtor that rentegodtgørelse is not taxable income (GIL § 8b, stk. 2,
3. pkt.).

**Acceptance criteria (FR-4):**
- A modregning decided 3 banking days after `receiptDate` produces
  `renteGodtgoerelseAccrued = 0.00 DKK` (5-banking-day exception).
- A modregning decided 10 banking days after `receiptDate` accrues from the 1st of the
  following month.
- A `paymentType = OVERSKYDENDE_SKAT` event with `indkomstAar = 2024` accrues
  rentegodtgørelse from 2025-09-01, not from the standard start date.
- `renteGodtgoerelseNonTaxable = true` is set on every `ModregningEvent`.

---

### FR-5: Klage (appeal) deadline tracking

**FR-5.1 — Klage deadline computation**  
On every `ModregningEvent`, the field `klageFristDato` is computed as follows:
- If `noticeDelivered = true`: `klageFristDato` = notice delivery date + 3 calendar months.
- If `noticeDelivered = false`: `klageFristDato` = `ModregningEvent.decisionDate` + 1 year.

**FR-5.2 — Read model endpoint**  
Endpoint: `GET /debtors/{id}/modregning-events`  
Returns: a paginated list of `ModregningEventSummary` records, each containing:
`eventId`, `decisionDate`, `totalOffsetAmount`, `tier1Amount`, `tier2Amount`,
`tier3Amount`, `residualPayoutAmount`, `klageFristDato`, `noticeDelivered`, `tier2WaiverApplied`.

**FR-5.3 — Caseworker portal view**  
The caseworker portal includes a `modregning-events` view for each debtor, displaying the
read-model list with `klageFristDato` highlighted in amber if within 14 days, and red if
past, so caseworkers can act before appeal windows expire.

**Acceptance criteria (FR-5):**
- When `noticeDelivered = true` on date 2025-03-15, `klageFristDato = 2025-06-15`.
- When `noticeDelivered = false` and `decisionDate = 2025-03-15`,
  `klageFristDato = 2026-03-15`.
- `GET /debtors/{id}/modregning-events` returns HTTP 200 with all required fields.
- The caseworker portal highlights an event with `klageFristDato` 10 days away in amber.

---

## Non-functional requirements

### NFR-1: Atomicity

The full FR-1 workflow — tier ordering, CollectionMeasure creation, ledger entries, and
Digital Post invocation — executes within a single database transaction. If any step fails,
the entire transaction is rolled back. Digital Post is sent only after the transaction
commits (via a transactional outbox pattern, same as P052).

### NFR-2: Auditability

Every modregning decision — including each fordring covered, the tier applied, the GIL §
reference, and the rentegodtgørelse basis — is written to the CLS audit log. Each CLS log
entry carries `gilParagraf` (e.g. `"GIL § 7, stk. 1, nr. 2"`), `modregningEventId`,
`debtorPersonId`, and `fordringId`.

### NFR-3: GDPR

All domain entities reference debtors exclusively via `person_id` (UUID). No CPR numbers,
names, or other direct identifiers are stored in the P058 domain tables.

### NFR-4: Idempotency

`PublicDisbursementEvent` processing must be idempotent: replaying the same event (identified
by `nemkontoReferenceId`) must not create a duplicate `ModregningEvent` or a duplicate
`SET_OFF` `CollectionMeasureEntity`. The `nemkontoReferenceId` is stored as a unique key on
the `modregning_event` table.

### NFR-5: Performance

The three-tier ordering engine must resolve and persist a `ModregningEvent` covering up to
500 active fordringer within 2 seconds at p99 under normal load (steady state), measured
from event ingestion to transaction commit.

---

## Constraints

- **Service ownership (ADR-0027):** `opendebt-debt-service` owns modregning and
  korrektionspulje. `DaekningsRaekkefoeigenService` is called via internal API
  (`opendebt-payment-service`), not via direct database access.
- **Double-entry bookkeeping (ADR-0018):** Every coverage allocation generates both a debit
  and a credit ledger entry. No single-sided entries are permitted.
- **Tier-2 ordering delegates to P057:** The modregning engine does NOT re-implement the
  GIL § 4 rule. It delegates to `DaekningsRaekkefoeigenService` for all tier-2 ordering.
- **DMI korrektionspulje (GIL § 4, stk. 9):** Modelled as `correctionPoolTarget = DMI` flag
  only. No DMI correction-pool settlement logic is implemented in P058.
- **Tværgående lønindeholdelse korrektionspulje (G.A.2.3.4.3):** Out of scope — separate
  petition.
- **Konkurslov / gældsbrevslov exceptions to modregningsrækkefølge:** Flagged as legal-team
  review item; not implemented in P058.
- **Manual modregning (caseworker-initiated):** Future petition. P058 covers only automatic
  (event-driven) modregning.
- **Børne-og-ungeydelse modregning restrictions (§ 11, stk. 2):** The restriction flag is
  persisted; enforcement of the specific restriction rules (which fordringtyper are eligible
  for modregning against børneydelse) is out of scope for P058 — the flag enables future
  enforcement.

---

## Deliverables

| # | Deliverable | Path / Location |
|---|-------------|-----------------|
| D-1 | `ModregningService` (implements `OffsettingService`) | `opendebt-debt-service/src/main/java/.../service/ModregningService.java` |
| D-2 | `ModregningsRaekkefoeigenEngine` — three-tier ordering logic | `opendebt-debt-service/src/main/java/.../service/ModregningsRaekkefoeigenEngine.java` |
| D-3 | `RenteGodtgoerelseService` — rate and start-date computation | `opendebt-debt-service/src/main/java/.../service/RenteGodtgoerelseService.java` |
| D-4 | `KorrektionspuljeService` — gendækning and pool management | `opendebt-debt-service/src/main/java/.../service/KorrektionspuljeService.java` |
| D-5 | `KorrektionspuljeSettlementJob` — scheduled monthly settlement | `opendebt-debt-service/src/main/java/.../job/KorrektionspuljeSettlementJob.java` |
| D-6 | `ModregningEvent` entity | `opendebt-debt-service/src/main/java/.../domain/ModregningEvent.java` |
| D-7 | `KorrektionspuljeEntry` entity | `opendebt-debt-service/src/main/java/.../domain/KorrektionspuljeEntry.java` |
| D-8 | `RenteGodtgoerelseRateEntry` entity | `opendebt-debt-service/src/main/java/.../domain/RenteGodtgoerelseRateEntry.java` |
| D-9 | `PublicDisbursementEventConsumer` — Kafka/event ingestion | `opendebt-debt-service/src/main/java/.../consumer/PublicDisbursementEventConsumer.java` |
| D-10 | `OffsettingReversalEventConsumer` — korrektionspulje trigger | `opendebt-debt-service/src/main/java/.../consumer/OffsettingReversalEventConsumer.java` |
| D-11 | `ModregningController` — waiver API + read model | `opendebt-debt-service/src/main/java/.../controller/ModregningController.java` |
| D-12 | OpenAPI spec for modregning endpoints | `opendebt-debt-service/src/main/resources/openapi/modregning.yaml` |
| D-13 | Sagsbehandler portal — modregning-events view template | `opendebt-caseworker-portal/src/main/resources/templates/debtor/modregning-events.html` |
| D-14 | Sagsbehandler portal — modregning view controller | `opendebt-caseworker-portal/src/main/java/.../controller/ModregningViewController.java` |
| D-15 | Digital Post notice template (DA) | `opendebt-debt-service/src/main/resources/notifications/modregning-notice-da.html` |
| D-16 | Liquibase migrations (modregning_event, korrektionspulje_entry, rentegodt_rate tables) | `opendebt-debt-service/src/main/resources/db/changelog/` |
| D-17 | Danish i18n message bundle additions | `opendebt-caseworker-portal/src/main/resources/messages_da.properties` |
| D-18 | English i18n message bundle additions | `opendebt-caseworker-portal/src/main/resources/messages_en_GB.properties` |
| D-19 | Gherkin feature file | `petitions/petition058-modregning-korrektionspulje.feature` |

---

## Out of scope

| Item | Reason |
|------|--------|
| DMI korrektionspulje settlement (GIL § 4, stk. 9) | Modelled as routing flag only; full DMI pool mechanics are a future item |
| Tværgående lønindeholdelse korrektionspulje (G.A.2.3.4.3) | Separate petition |
| Konkurslov / gældsbrevslov exceptions to modregningsrækkefølge | Legal-team review required; flagged as future item |
| Manual caseworker-initiated modregning | Future petition |
| Børne-og-ungeydelse restriction enforcement rules (§ 11, stk. 2 detail) | Restriction flag persisted; enforcement logic future petition |
| GIL § 49 DMI paralleldrift modregning rules | DMI-originated payments governed by legacy DMI logic; out of scope |
| Catala encoding of modregningsrækkefølge | Tier B — not required for P058 |

---

## Definition of Done

- [ ] `ModregningService.initiateModregning(debtorPersonId, availableAmount, paymentType)` replaces the stub and processes the three-tier workflow end-to-end
- [ ] Three-tier ordering engine tested for all tier combinations: tier-1 only, tier-2 only, tier-3 only, all-tiers mixed, and residual payout
- [ ] Tier-2 partial coverage delegates to `DaekningsRaekkefoeigenService` (P057) — verified by unit tests mocking the P057 service
- [ ] `SET_OFF` `CollectionMeasureEntity` created per covered fordring, referencing `ModregningEvent.id`
- [ ] Ledger entries generated per ADR-0018 (double-entry) for every coverage allocation
- [ ] Digital Post notice sent via transactional outbox pattern; `noticeDelivered` flag set correctly
- [ ] `klageFristDato` computed correctly for both delivered and non-delivered notice paths
- [ ] Caseworker waiver (`POST tier2-waiver`) enforces `modregning:waiver` scope and writes CLS audit entry with `"GIL § 4, stk. 11"`
- [ ] `KorrektionspuljeService` correctly sequences: (1) residual same-fordring → (2) gendækning → (3) pool entry
- [ ] `KorrektionspuljeSettlementJob` skips entries with `surplusAmount < 50 DKK` in monthly runs and processes them in the annual run
- [ ] `boerneYdelseRestriction` preserved after pool settlement
- [ ] `RenteGodtgoerelseService` applies the 5-banking-day exception and the kildeskattelov § 62/62A special start date
- [ ] `renteGodtgoerelseNonTaxable = true` on all `ModregningEvent` records
- [ ] Idempotency guard: duplicate `nemkontoReferenceId` is rejected without creating a second `ModregningEvent`
- [ ] `GET /debtors/{id}/modregning-events` returns HTTP 200 with all required fields
- [ ] Sagsbehandler portal displays `klageFristDato` with amber/red highlighting
- [ ] All new i18n labels present in both DA and EN message bundles
- [ ] Liquibase migrations add `modregning_event`, `korrektionspulje_entry`, `rentegodt_rate_entry` tables with all required columns
- [ ] CLS audit log entries for each modregning decision include `gilParagraf`, `modregningEventId`, `debtorPersonId`, and `fordringId`
- [ ] `behave --dry-run` passes on `petitions/petition058-modregning-korrektionspulje.feature`
- [ ] Architecture overview updated in `architecture/overview.md`
