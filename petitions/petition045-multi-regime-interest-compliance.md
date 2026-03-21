# Petition 045: Multi-regime interest and fee compliance per PSRM debt type rules

## Summary

OpenDebt shall correctly apply debt-type-specific interest regimes, fee schedules, and accounting rules as required by Danish legislation and Gældsstyrelsen's business rules. The system currently applies a single global interest rate (5.75% p.a.) to all debts in collection. The PSRM reference model and gældsinddrivelsesloven require differentiated treatment: straffebøder are interest-exempt, told requires NB+2%, contractual rates may override the default, and fees (gebyrer) must be tracked as separate interest-bearing line items with distinct accounting (renter→fordringshaver, gebyrer→staten).

## Context and motivation

An analysis of the current OpenDebt implementation against Gældsstyrelsen's published interest and fee rules (see `docs/psrm-reference/Gældsstyrelsen is responsible for debt collection.md`) identified eight compliance gaps:

| # | Gap | Current state | Required state |
|---|-----|--------------|----------------|
| G1 | Single global interest rate | `opendebt.interest.annual-rate: 0.0575` applied to all debts | Per-debt-type rate determination via `interest_rule` + `interest_rate_code` on DebtEntity |
| G2 | Straffebøder receive interest | No exemption logic | Straffebøder (criminal fines) must be interest-exempt per gældsinddrivelsesloven § 5, stk. 1 |
| G3 | No told interest regime | Only NB+4% available | Told (customs) debts use NB+2% (without afdragsordning) or NB+1% (with afdragsordning) per EUTK art. 114 |
| G4 | No contractual rate override | Fixed rate only | Fordringshaver may request an alternative morarente (e.g., from contract or court judgment) if IT-technically supported |
| G5 | No fee entity model | `feesAmount` is a single BigDecimal on DebtEntity | Fees must be individually tracked: rykkergebyr (65 kr), udlægsafgift (300 kr + 0.5%), lønindeholdelsesgebyr (~100 kr), each with its own accrual date |
| G6 | Fees are not interest-bearing | No interest calculation on fees | Inddrivelsesrente applies to both hovedstol AND inddrivelsesgebyrer per gældsinddrivelsesloven |
| G7 | No separate accounting for renter vs gebyrer | Single interestAmount field | Renter tilfalder fordringshaver; gebyrer (and renter on gebyrer) tilfalder staten — requires separate ledger tracking |
| G8 | Administrative bøder treated same as strafbøder | No distinction | Dagbøder and administrative fines ARE interest-bearing (unlike strafferetlige bøder) |

### Impact on existing components

- **InterestAccrualJob/Helper** — must resolve rate per debt instead of reading a global config property
- **DebtTypeEntity** — must carry interest rule configuration (partially present: `interestApplicable` flag exists but is not used in the batch job)
- **InterestSelectionEmbeddable** — `interest_rule` and `interest_rate_code` fields exist but are never consulted
- **Rules engine (Drools)** — `interest-calculation.drl` has a placeholder "Standard Interest Rate" rule; must be extended with per-type rules
- **CoveragePriorityService** — currently handles interest/principal split; must be extended for fees
- **BookkeepingService** — needs distinct journal categories for renter-to-fordringshaver vs gebyr-to-staten
- **DebtEntity** — `feesAmount` must be replaced or augmented with a fee entity collection

## Functional requirements

### FR-1: Interest rule resolution per debt

1. The InterestAccrualJob shall resolve the applicable interest rate for each debt by consulting the debt's `interest_rule` and `interest_rate_code` (from InterestSelectionEmbeddable) and the debt type's configuration (from DebtTypeEntity).
2. If `DebtTypeEntity.interestApplicable` is false, no interest shall be accrued.
3. The following interest rules shall be supported:

| Interest rule code | Rate formula | Applies to | Legal basis |
|--------------------|-------------|------------|-------------|
| `INDR_STD` | NB udlånsrente + 4% p.a. | Default for all public debts under collection | Gældsinddrivelsesloven § 5, stk. 1-2 |
| `INDR_TOLD` | NB udlånsrente + 2% p.a. (no afdragsordning) | Customs debts (toldskyld) | EUTK art. 114, Toldloven § 30a |
| `INDR_TOLD_AFD` | NB udlånsrente + 1% p.a. (with afdragsordning) | Customs debts with payment plan | EUTK art. 114 |
| `INDR_EXEMPT` | 0% (no interest) | Straffebøder (criminal fines) | Gældsinddrivelsesloven § 5, stk. 1 |
| `INDR_CONTRACT` | Contractual rate from fordringshaver | Debts with agreed morarente | Gældsinddrivelsesloven, fordringshaver request |
| `OPK_STD` | ~0.7% per month (kassekreditrente + 0.7%) | Pre-collection phase (opkrævning) — informational only | Opkrævningsloven § 7 |

4. The contractual rate (`INDR_CONTRACT`) shall use the `additional_interest_rate` field from InterestSelectionEmbeddable as the annual rate.

### FR-2: Straffebøder interest exemption

5. Debts where `DebtTypeEntity.interestApplicable = false` OR where `interest_rule = 'INDR_EXEMPT'` shall never accrue interest.
6. The InterestAccrualJob shall skip these debts without creating zero-amount journal entries.
7. The Drools rule `interest-calculation.drl` shall include a `salience`-prioritized rule for straffebøder returning `noInterest()`.

### FR-3: Administrative bøder vs strafferetlige bøder

8. DebtTypeEntity shall distinguish between strafferetlige bøder (`interestApplicable = false`) and administrative bøder/dagbøder (`interestApplicable = true`).
9. Seed data for debt types shall correctly classify: `STRAF_BOEDE` (no interest), `DAGBOEDE` (interest applies), `ADMIN_BOEDE` (interest applies).

### FR-4: Fee entity model (gebyrer)

10. OpenDebt shall introduce a `FeeEntity` to track individual fees:

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `debt_id` | UUID | Reference to parent debt |
| `fee_type` | Enum | `RYKKER`, `UDLAEG`, `LOENINDEHOLDELSE`, `OTHER` |
| `amount` | BigDecimal | Fee amount |
| `accrual_date` | LocalDate | Date fee was imposed |
| `legal_basis` | String | Legal reference (e.g., "Opkrævningsloven § 6") |
| `paid` | boolean | Whether fee has been fully covered |

11. `DebtEntity.feesAmount` shall remain as a computed/denormalized total, derived from the sum of unpaid `FeeEntity` amounts.
12. Standard fee amounts shall be configurable (see petition 046 for versioned configuration):
    - Rykkergebyr: 65 kr per erindringsskrivelse (Opkrævningsloven § 6)
    - Udlægsafgift: 300 kr + 0.5% of debt over 3,000 kr (Retsafgiftsloven)
    - Lønindeholdelsesgebyr: ~100 kr per lønindeholdelse

### FR-5: Interest on fees

13. Inddrivelsesrente shall be calculated on both hovedstol AND outstanding fees (gebyrer).
14. The InterestAccrualJob shall include unpaid fee amounts in the balance used for daily interest calculation:
    ```
    daily_interest = (outstanding_principal + outstanding_fees) × annual_rate / 365
    ```
15. Interest journal entries shall record the total balance (principal + fees) in `balance_snapshot`.

### FR-6: Separate accounting — renter and gebyrer

16. Interest accrued on the original debt principal (`renter`) shall be tagged as `RENTER_FORDRINGSHAVER` in the bookkeeping ledger.
17. Fees (`gebyrer`) and interest accrued on fees shall be tagged as `GEBYR_STATEN` and `RENTER_GEBYR_STATEN` respectively in the bookkeeping ledger.
18. The `LedgerEntryEntity` shall support a new field `accounting_target` (enum: `FORDRINGSHAVER`, `STATEN`) to enable correct distribution.
19. Reporting and afstemning (reconciliation) endpoints shall distinguish between amounts owed to fordringshaver vs staten.

### FR-7: Coverage priority with fees

20. The dækningsrækkefølge (CoveragePriorityService) shall be extended to allocate payments in this order:
    1. Inddrivelsesrenter (interest) — all interest first
    2. Gebyrer (fees) — then fees
    3. Hovedstol (principal) — then principal
21. The `CoverageAllocation` model shall be extended with a `feesPortion` field.

## PSRM reference context

### Inddrivelsesrenten (hovedregel)
> Rentesats: NB's officielle udlånsrente + 4 procentpoint, reguleres halvårligt. Pr. 2026: 5.75% p.a.
> Beregning: simpel dag-til-dag rente af gældens hovedstol samt inddrivelsesgebyrer.
> Rentestart: 1. i måneden efter oversendelse til Gældsstyrelsen.
_Source: Gældsinddrivelsesloven § 5, stk. 1-2_

### Straffebøder
> Gældsstyrelsen tilskriver IKKE inddrivelsesrenter på straffebøder.
_Source: Gældsinddrivelsesloven § 5, stk. 1; Retsplejeloven § 997, stk. 3_

### Told rente
> NB's udlånsrente + 2%-point (uden afdragsordning) eller + 1%-point (med afdragsordning).
_Source: EU-toldkodeks art. 114; Toldloven § 30a_

### Gebyrer
> Rykkergebyr 65 kr (Opkrævningsloven § 6), udlægsafgift 300 kr + 0.5% (Retsafgiftsloven).
> Renter tilfalder fordringshaver; gebyrer og renter på gebyrer tilfalder staten.
_Source: Gældsinddrivelsesloven; Opkrævningsloven § 6_

### Kontraktuel rente
> Fordringshaver kan anmode Gældsstyrelsen om at anvende en aftalt morarente i stedet for inddrivelsesrenten, hvis satsen kan understøttes it-teknisk.
_Source: Gældsstyrelsens vejledning for fordringshavere, renteregler_

## Constraints and assumptions

- All interest rules resolve to a single annual rate per debt per day. The system does not need to support intra-day rate changes.
- Opkrævningsrente (pre-collection phase) is informational only — OpenDebt tracks the accumulated amount as received from fordringshaver but does not calculate it.
- The NB udlånsrente is an external input, updated semi-annually. Petition 046 (versioned business configuration) provides the mechanism for storing and versioning this rate.
- The fee entity model replaces the simple `feesAmount` field conceptually, but the denormalized field is retained for query performance.
- This petition does NOT cover SU-lån compound interest (renters rente) — SU-gæld under Gældsstyrelsen's collection uses inddrivelsesrente like all other debts.
- Restskat renter (procenttillæg, dag-til-dag rente before collection) are calculated by Skattestyrelsen before overdragelse — OpenDebt receives the computed amount as part of the claim principal. OpenDebt does NOT recalculate restskat renter.

## Existing system building blocks

| Component | Status | Change needed |
|-----------|--------|---------------|
| `InterestAccrualJob` / `InterestAccrualJobHelper` | Done | Resolve rate per debt via interest_rule lookup |
| `DebtTypeEntity` | Done | Already has `interestApplicable` flag — use it; add seed data for bøde types |
| `InterestSelectionEmbeddable` | Done | Already has `interest_rule`, `interest_rate_code`, `additional_interest_rate` — start using them |
| `InterestJournalEntry` | Done | Add `accounting_target` field |
| `CoveragePriorityService` | Done | Extend for 3-way split (interest → fees → principal) |
| `LedgerEntryEntity` | Done | Add `accounting_target` enum field |
| `interest-calculation.drl` | Done | Add per-type rules with salience ordering |
| `FeeEntity` | **New** | New entity for individual fee tracking |
| Versioned rate configuration | **New** | Provided by petition 046 |

## Dependencies

- **Petition 046** (versioned business configuration) — provides the mechanism for storing and versioning the NB udlånsrente and derived rates with validity periods.
- **Petition 043** (batch processing) — the InterestAccrualJob infrastructure that this petition extends.
- **Petition 039** (crossing transactions) — timeline replay must use per-debt rates when recalculating.

## Out of scope

- SU-lån compound interest calculation (renters rente under studietiden) — not applicable during Gældsstyrelsen collection phase.
- Restskat renter calculation (procenttillæg, dag-til-dag rente) — calculated by Skattestyrelsen before overdragelse.
- Opkrævningsrente calculation — calculated by fordringshaver before overdragelse.
- Physical letter generation for rykkerbreve (covered by letter-service/petition004).
- Notification generation when fees are imposed (covered by petition004 underretninger).
- Forældelse rules for different debt types (separate business rule concern).
