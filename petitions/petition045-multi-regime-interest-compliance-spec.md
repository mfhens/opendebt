# Spec: Interest Calculation DRL — Per-Regime Rules (CR-001)

**Parent petition:** petition045  
**CR:** CR-001  
**Component:** opendebt-rules-engine  
**Files modified:**
- `opendebt-rules-engine/src/main/java/dk/ufst/opendebt/rules/model/InterestCalculationRequest.java`
- `opendebt-rules-engine/src/main/resources/rules/interest-calculation.drl`

---

## 1. Model Extension — `InterestCalculationRequest`

Add two fields to the existing `@Data @Builder` class:

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `interestRule` | `String` | yes | Interest regime code, e.g. `"INDR_STD"`. Null means no rule resolved — safe default: rule "NOT_DUE" fires before any rate rule if `daysPastDue <= 0`, otherwise the DRL has no matching rule and `result` remains at its initial zero-state. |
| `annualRate` | `BigDecimal` | yes | Annual rate pre-resolved by the caller via `BusinessConfigService.getDecimalValue(configKey, calculationDate)`. The DRL uses this value verbatim. |

**Time-aware contract:** The caller MUST pass `calculationDate` (not `LocalDate.now()`) as the effective date for the `BusinessConfigService` lookup. This ensures retroactive recalculations (petition039) use the legally correct historical rate.

No changes to `InterestCalculationResult` — existing fields (`interestAmount`, `interestRate`, `rateType`, `legalBasis`, `daysCalculated`) are sufficient.

---

## 2. DRL Rewrite — `interest-calculation.drl`

### 2.1 Salience table

| Salience | Rule name | Trigger condition | Outcome |
|----------|-----------|-------------------|---------|
| 200 | `No Interest Before Due Date` | `daysPastDue <= 0` | `interestAmount=0`, `rateType="NOT_DUE"` |
| 100 | `INDR_EXEMPT: Straffebøder` | `interestRule == "INDR_EXEMPT"` | `interestAmount=0`, `rateType="INDR_EXEMPT"` |
| 90  | `No Interest For Small Amounts` | `principalAmount < 100` | `interestAmount=0`, `rateType="SMALL_AMOUNT"` |
| 50  | `INDR_STD: Standard Rate` | `interestRule == "INDR_STD"`, `annualRate != null` | formula, `rateType="INDR_STD"` |
| 50  | `INDR_TOLD: Told Rate` | `interestRule == "INDR_TOLD"`, `annualRate != null` | formula, `rateType="INDR_TOLD"` |
| 50  | `INDR_TOLD_AFD: Told Afdrag Rate` | `interestRule == "INDR_TOLD_AFD"`, `annualRate != null` | formula, `rateType="INDR_TOLD_AFD"` |
| 50  | `INDR_CONTRACT: Contractual Rate` | `interestRule == "INDR_CONTRACT"`, `annualRate != null` | formula, `rateType="INDR_CONTRACT"` |

The no-interest rules use `no-loop true` to prevent re-firing after `modify`. The salience ordering guarantees exempt/no-due/small-amount guards fire before any rate rule; two rules at salience 50 cannot both fire for the same request because they are mutually exclusive on `interestRule`.

### 2.2 Interest formula (all rate rules)

```
dailyRate  = annualRate / 365  (scale=10, HALF_UP)
interest   = principal × dailyRate × daysPastDue  (scale=2, HALF_UP)
interestRate (for result) = annualRate × 100  (percentage representation)
```

All arithmetic uses `java.math.BigDecimal` with explicit `RoundingMode.HALF_UP`. No `double` arithmetic.

### 2.3 Legal basis strings (verbatim)

| rateType | legalBasis |
|----------|-----------|
| `NOT_DUE` | `"Gæld endnu ikke forfalden"` |
| `INDR_EXEMPT` | `"Gældsinddrivelsesloven § 5, stk. 1; Retsplejeloven § 997, stk. 3"` |
| `SMALL_AMOUNT` | `"Beløb under 100 kr — ingen rente"` |
| `INDR_STD` | `"Gældsinddrivelsesloven § 5, stk. 1-2"` |
| `INDR_TOLD` | `"EU-toldkodeks art. 114; Toldloven § 30a"` |
| `INDR_TOLD_AFD` | `"EU-toldkodeks art. 114 (med afdragsordning)"` |
| `INDR_CONTRACT` | `"Gældsinddrivelsesbekendtgørelsen § 9, stk. 3"` |

### 2.4 No-match behaviour

If `interestRule` is null or contains an unrecognised code and none of the guard rules fire, `result` retains the initial zero-state set by `RulesServiceImpl` (`daysCalculated` pre-set, all others null/zero). Callers should treat a null `rateType` in the result as a configuration error.

---

## 3. Integration Test — `InterestCalculationRulesIntegrationTest`

**Location:** `opendebt-rules-engine/src/test/java/dk/ufst/opendebt/rules/interest/`

Use a real `KieContainer` (loaded from classpath via `KieServices.Factory.get()`) — NOT mocked — so the DRL actually fires.

Test matrix (one `@Test` per row):

| Test | interestRule | annualRate | principal | daysPastDue | expected amount | expected rateType |
|------|-------------|-----------|-----------|-------------|----------------|-------------------|
| `indrStd_producesCorrectInterest` | INDR_STD | 0.0575 | 10000 | 365 | 575.00 | INDR_STD |
| `indrTold_producesCorrectInterest` | INDR_TOLD | 0.0375 | 10000 | 365 | 375.00 | INDR_TOLD |
| `indrToldAfd_producesCorrectInterest` | INDR_TOLD_AFD | 0.0275 | 10000 | 365 | 275.00 | INDR_TOLD_AFD |
| `indrContract_producesCorrectInterest` | INDR_CONTRACT | 0.0800 | 10000 | 365 | 800.00 | INDR_CONTRACT |
| `indrExempt_producesZeroInterest` | INDR_EXEMPT | null | 50000 | 365 | 0.00 | INDR_EXEMPT |
| `notDue_producesZeroInterest` | INDR_STD | 0.0575 | 10000 | 0 | 0.00 | NOT_DUE |
| `smallAmount_producesZeroInterest` | INDR_STD | 0.0575 | 99.99 | 365 | 0.00 | SMALL_AMOUNT |
| `historicalRate_usedVerbatim` | INDR_STD | 0.0775 | 10000 | 365 | 775.00 | INDR_STD |
| `noHardcodedRate_differentAnnualRatesProduceDifferentResults` | INDR_STD | 0.0575 vs 0.0775 | 10000 | 365 | 575.00 ≠ 775.00 | — |

Tolerance: ±0.01 kr using `assertThat(amount).isCloseTo(expected, within(0.01))`.

---

## 4. Out of scope (this CR)

- `InterestAccrualJobHelper` wiring (caller-side change — petition045/046 scope)
- `BusinessConfigService` rate seeding
- Fee entity model (petition045 FR-4 through FR-7)
- OPK_STD regime
