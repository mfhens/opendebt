# Implementation Specification — P053: Opskrivning og nedskrivning (fuld G.A.-komplient)

**Spec ID:** SPEC-P053  
**Petition:** petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.md  
**Outcome contract:** petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec-outcome-contract.md  
**Feature file:** petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.feature  
**Solution architecture:** design/solution-architecture-p053-opskrivning-nedskrivning.md  
**Component map:** petitions/petition053_map.yaml  
**Status:** Ready for implementation  
**Legal basis:** G.A.1.4.3, G.A.1.4.4, G.A.2.3.4.4, Gæld.bekendtg. § 7 stk. 1–2, GIL § 18 k  
**Supersedes:** Petition 034 (opskrivning og nedskrivning — basic portal flow)

> **Implementation delta:** FR-2, FR-3, and FR-8 are already implemented baselines.
> The new code scope is **FR-1, FR-4, FR-5, FR-6, FR-7, and FR-9** only.

---

## Baseline — resolved A2 gate

`DebtEntity.receivedAt` (`LocalDateTime`, JPA column `received_at`) is the PSRM registration date.
It is set to `LocalDateTime.now()` in `ClaimLifecycleServiceImpl` when the claim is accepted for
collection. No new entity field or database migration is required. FR-6 uses it as:

```java
virkningsdato.isBefore(debt.getReceivedAt().toLocalDate())
```

---

## Module and package reference

| Module | Base package |
|--------|-------------|
| `opendebt-creditor-portal` | `dk.ufst.opendebt.creditor` |
| `opendebt-debt-service` | `dk.ufst.opendebt.debtservice` |

All file paths in this document are relative to the repository root.

---

## SPEC-FR-1 — Nedskrivning reason dropdown

**Source:** Petition 053 FR-1 · Outcome contract FR-1 · Gæld.bekendtg. § 7 stk. 2

### 1.1 Portal — new enum `WriteDownReasonCode`

**Action:** CREATE  
**File:** `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/dto/WriteDownReasonCode.java`

```java
package dk.ufst.opendebt.creditor.dto;

public enum WriteDownReasonCode {
    /** Direkte indbetaling til fordringshaver — Gæld.bekendtg. § 7 stk. 2 nr. 1 */
    NED_INDBETALING,
    /** Fejlagtig oversendelse til inddrivelse — Gæld.bekendtg. § 7 stk. 2 nr. 2 */
    NED_FEJL_OVERSENDELSE,
    /** Opkrævningsgrundlaget har ændret sig — Gæld.bekendtg. § 7 stk. 2 nr. 3 */
    NED_GRUNDLAG_AENDRET
}
```

Constants are legally anchored in statute. They must not be extended without a legislative
amendment to Gæld.bekendtg. § 7 stk. 2.

### 1.2 Portal — modify `ClaimAdjustmentRequestDto`

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/dto/ClaimAdjustmentRequestDto.java`

**Remove** the field:

```java
@NotBlank(message = "{adjustment.validation.reason.required}")
private String reason;
```

**Add** the field:

```java
private WriteDownReasonCode writeDownReasonCode;
```

No `@NotNull` annotation on the field itself. The null-check for write-down direction is
performed explicitly in `ClaimAdjustmentController.submitAdjustment()` via a conditional guard
(see §1.4), not via Bean Validation, because the constraint is direction-dependent.

> **Note:** The solution architecture §3.1 suggested `@NotNull(message = '{adjustment.validation.reason.required}')` on the field itself. This is superseded by the conditional controller guard specified here, which correctly handles the direction-dependent constraint without a custom cross-field validator.

The existing fields `adjustmentType`, `amount`, `effectiveDate`, and `debtorIndex` are unchanged.

### 1.3 Debt-service — new enum `WriteDownReasonCode`

**Action:** CREATE  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/dto/WriteDownReasonCode.java`

```java
package dk.ufst.opendebt.debtservice.dto;

public enum WriteDownReasonCode {
    /** Direkte indbetaling til fordringshaver — Gæld.bekendtg. § 7 stk. 2 nr. 1 */
    NED_INDBETALING,
    /** Fejlagtig oversendelse til inddrivelse — Gæld.bekendtg. § 7 stk. 2 nr. 2 */
    NED_FEJL_OVERSENDELSE,
    /** Opkrævningsgrundlaget har ændret sig — Gæld.bekendtg. § 7 stk. 2 nr. 3 */
    NED_GRUNDLAG_AENDRET
}
```

Values are identical to the portal enum (Decision 2 — dual enum, no shared extraction). Parity
is enforced by code review convention.

### 1.4 Portal — `ClaimAdjustmentController` changes (GET + POST handlers)

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/controller/ClaimAdjustmentController.java`

**In `showAdjustmentForm` (GET handler):**

Add the following model attribute when `direction == WRITE_DOWN`:

```java
if (direction == ClaimAdjustmentType.Direction.WRITE_DOWN) {
    model.addAttribute("writeDownReasonCodes", WriteDownReasonCode.values());
}
```

**In `submitAdjustment` (POST handler):**

After form binding and before calling `debtServiceClient.submitAdjustment()`, add the
direction-conditional reason code guard:

```java
if (direction == ClaimAdjustmentType.Direction.WRITE_DOWN
        && adjustmentForm.getWriteDownReasonCode() == null) {
    bindingResult.rejectValue(
        "writeDownReasonCode",
        "adjustment.validation.reason.required",
        new Object[]{},
        "Vælg venligst en årsag til nedskrivningen"
    );
}
if (bindingResult.hasErrors()) {
    return reloadFormWithErrors(id, adjustmentForm, direction, model, session);
}
```

**In `reloadFormWithErrors` helper:**

Add `writeDownReasonCodes` model attribute for `WRITE_DOWN` direction:

```java
if (direction == ClaimAdjustmentType.Direction.WRITE_DOWN) {
    model.addAttribute("writeDownReasonCodes", WriteDownReasonCode.values());
}
```

### 1.5 Portal — `form.html` dropdown

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/resources/templates/claims/adjustment/form.html`

**Remove** the existing free-text `<input type="text">` for `reason` inside the write-down block.

**Add** a `<select>` dropdown bound to `writeDownReasonCode`, inside a `th:block th:if="${direction != 'WRITE_UP'}"` block:

```html
<!-- Write-down: controlled reason dropdown (FR-1 / Gæld.bekendtg. § 7 stk. 2) -->
<th:block th:if="${direction != 'WRITE_UP'}">
  <div class="skat-form-group">
    <label for="writeDownReasonCode"
           class="skat-label"
           th:text="#{adjustment.label.reason}">Årsag / årsagskode</label>
    <select id="writeDownReasonCode"
            th:field="*{writeDownReasonCode}"
            class="skat-select"
            th:classappend="${#fields.hasErrors('writeDownReasonCode')} ? 'skat-input--error'"
            aria-describedby="writeDownReasonCode-error"
            th:attr="aria-invalid=${#fields.hasErrors('writeDownReasonCode')} ? 'true' : 'false'"
            required="required">
      <option value="" th:text="#{adjustment.reason.placeholder}">-- Vælg årsag --</option>
      <option th:each="rc : ${writeDownReasonCodes}"
              th:value="${rc.name()}"
              th:text="#{|adjustment.reason.ned.${rc.name().toLowerCase().replace('ned_', '')}|}"
              th:selected="${adjustmentForm.writeDownReasonCode == rc}">Årsag</option>
    </select>
    <span id="writeDownReasonCode-error"
          class="skat-error-message"
          role="alert"
          th:if="${#fields.hasErrors('writeDownReasonCode')}"
          th:text="${#fields.errors('writeDownReasonCode')[0]}">Fejl</span>
  </div>
</th:block>
```

i18n key resolution for the `<option>` elements:

| Enum constant | Resolved i18n key |
|---------------|-------------------|
| `NED_INDBETALING` | `adjustment.reason.ned.indbetaling` |
| `NED_FEJL_OVERSENDELSE` | `adjustment.reason.ned.fejl_oversendelse` |
| `NED_GRUNDLAG_AENDRET` | `adjustment.reason.ned.grundlag_aendret` |

The existing `adjustment.label.reason` key is reused as the label. No additional label key is
introduced.

### 1.6 Debt-service — `WriteDownDto`

**Action:** CREATE (or MODIFY if a partial baseline exists)  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/dto/WriteDownDto.java`

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `reasonCode` | `WriteDownReasonCode` | Yes (for WRITE_DOWN) | Replaces any prior `String reason` free-text field |
| `amount` | `BigDecimal` | Yes | Must be `> 0` |
| `effectiveDate` | `LocalDate` | Yes | ISO-8601 |
| `debtorId` | `String` (UUID) | Conditional | Required for payment-related types with multiple debtors |

The `String reason` free-text field is not present in this DTO.

> **Note:** SA §3.1 listed `WriteDownDto.java (MODIFY)` for FR-1. After codebase inspection,
> `WriteDownDto` is a petition 030 data-display DTO not used in the adjustment submission flow
> (`DebtServiceClient.submitAdjustment()` takes `ClaimAdjustmentRequestDto`). No modification to
> `WriteDownDto` is required by this petition.

### 1.7 Acceptance criteria (FR-1)

| AC | Criterion |
|----|-----------|
| AC-1 | Nedskrivning form renders a `<select>` with exactly three `<option>` elements: `NED_INDBETALING`, `NED_FEJL_OVERSENDELSE`, `NED_GRUNDLAG_AENDRET`. |
| AC-2 | No free-text `<input>` for reason is present on the nedskrivning form. |
| AC-3 | Submitting without selecting a reason code → form reloaded with validation error using key `adjustment.validation.reason.required`; BFF call not made. |
| AC-4 | Submitting with an unrecognised reason code → form reloaded with validation error; BFF call not made. |
| AC-5 | Submitting with a valid reason code → `WriteDownReasonCode` forwarded in `ClaimAdjustmentRequestDto.writeDownReasonCode`; debt-service accepts and returns success receipt. |
| AC-11 | `debt-service` returns HTTP 422 for `WriteDownDto` missing `reasonCode` or carrying an unrecognised value. |

---

## SPEC-FR-4 — Retroactive nedskrivning advisory

**Source:** Petition 053 FR-4 · Outcome contract FR-4 · G.A.1.4.4

### 4.1 Portal — `ClaimAdjustmentController` changes

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/controller/ClaimAdjustmentController.java`

**In `submitAdjustment` (POST handler)**, after form binding (whether or not there are errors),
add the retroactive advisory flag:

```java
if (direction == ClaimAdjustmentType.Direction.WRITE_DOWN
        && adjustmentForm.getEffectiveDate() != null
        && adjustmentForm.getEffectiveDate().isBefore(LocalDate.now())) {
    model.addAttribute("retroaktivAdvisoryActive", true);
}
```

This check runs on every POST for a write-down with a past `effectiveDate`, regardless of whether
other validation errors exist. The advisory never blocks submission; `bindingResult.rejectValue()`
is **not** called for this condition.

The `retroaktivAdvisoryActive` flag is **not set** on the GET handler. The advisory is visible
only after a POST reload (the user has not entered a date on GET).

**In `reloadFormWithErrors` helper:**

Re-evaluate and set `retroaktivAdvisoryActive` identically to the POST handler check above.

### 4.2 Portal — `form.html` advisory block

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/resources/templates/claims/adjustment/form.html`

Insert immediately after the `effectiveDate` field's error span:

```html
<!-- FR-4 / G.A.1.4.4: Inline advisory when virkningsdato is retroactive -->
<div id="retroaktiv-advisory"
     th:if="${retroaktivAdvisoryActive}"
     class="skat-alert skat-alert--warning"
     aria-live="polite">
  <p th:text="#{adjustment.info.retroaktiv.virkningsdato}">
    Virkningsdato er i fortiden. Gældsstyrelsen er forpligtet til at omfordele alle
    dækninger foretaget efter denne dato og genberegne renter for den berørte periode.
    Behandlingen kan tage ekstra tid. Se G.A.1.4.4.
  </p>
</div>
```

WCAG 2.1 AA: The `aria-live="polite"` attribute is required on this element.

### 4.3 Debt-service — retroactive log marker

**Action:** MODIFY  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/service/impl/ClaimAdjustmentServiceImpl.java`

In `processAdjustment()`, after validation passes and before persisting, emit a log marker when
the effective date is in the past:

```java
if (request.getEffectiveDate() != null
        && request.getEffectiveDate().isBefore(LocalDate.now())) {
    log.warn("RETROACTIVE_NEDSKRIVNING claimId={} virkningsdato={}",
             claimId, request.getEffectiveDate());
}
```

The log entry uses `WARN` level. The structured marker `{event: "retroactive_nedskrivning",
claimId, virkningsdato}` enables operational monitoring / log aggregation queries. No HTTP 422
is returned. The adjustment proceeds normally after this marker.

### 4.4 Acceptance criteria (FR-4)

| AC | Criterion |
|----|-----------|
| AC-6 | Entering a `virkningsdato` earlier than today and POSTing the nedskrivning form → advisory div with `aria-live="polite"` rendered below the `effectiveDate` field using key `adjustment.info.retroaktiv.virkningsdato`. |
| AC-6b | `virkningsdato` = today or future → advisory div is absent from the rendered page. |
| AC-6c | Advisory present → form submission is not blocked; the form can be submitted and accepted. |

> **Test note:** The advisory is observable only in the server-rendered POST response, not on
> client-side date field change. Gherkin step definitions must trigger a form POST and inspect
> the resulting HTML response.

---

## SPEC-FR-5 — Backdated type description

**Source:** Petition 053 FR-5 · Outcome contract FR-5 · Gæld.bekendtg. § 7 stk. 1, 5. pkt.

### 5.1 Portal — `form.html` type description block

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/resources/templates/claims/adjustment/form.html`

Insert immediately after the `adjustmentType` `<select>` element's closing tag:

```html
<!-- FR-5 / Gæld.bekendtg. § 7 stk. 1, 5. pkt.: Backdating description -->
<div th:if="${adjustmentForm.adjustmentType != null
             and adjustmentForm.adjustmentType.name() == 'OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING'}"
     class="skat-alert skat-alert--info">
  <p th:text="#{adjustment.type.description.omgjort_nedskrivning_regulering}">
    Opskrivning af annulleret nedskrivning: opskrivningsfordringen anses for modtaget
    på samme tidspunkt som den fordring, der fejlagtigt blev nedskrevet (G.A.1.4.3).
  </p>
</div>
```

No controller change is required. The `adjustmentForm.adjustmentType` field is already bound
from the existing form model; the template evaluates it declaratively.

No new backend behaviour is introduced by FR-5.

### 5.2 Acceptance criteria (FR-5)

| AC | Criterion |
|----|-----------|
| AC-7 | When `adjustmentType == OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING` is selected or displayed → type description div rendered using key `adjustment.type.description.omgjort_nedskrivning_regulering`. |
| AC-7b | For any other `adjustmentType` (e.g. `OPSKRIVNING_REGULERING`) → type description div is absent. |

---

## SPEC-FR-6 — Cross-system retroactive suspension advisory

**Source:** Petition 053 FR-6 · Outcome contract FR-6 · G.A.1.4.4 · GIL § 18 k  
**Architecture decision:** Decision 1 (flag approach — raw PSRM date not exposed to portal)

### 6.1 Debt-service — new `ClaimAdjustmentResponseDto`

**Action:** CREATE  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/dto/ClaimAdjustmentResponseDto.java`

| Field | Type | Description |
|-------|------|-------------|
| `actionId` | `String` | PSRM action identifier |
| `status` | `String` | Processing status (e.g., `ACCEPTED`, `PENDING_HOERING`) |
| `amount` | `BigDecimal` | Adjustment amount processed |
| `crossSystemRetroactiveApplies` | `boolean` | `true` iff `virkningsdato < debt.getReceivedAt().toLocalDate()` |

### 6.2 Debt-service — GIL § 18 k evaluation in `ClaimAdjustmentServiceImpl`

**Action:** MODIFY / CREATE  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/service/impl/ClaimAdjustmentServiceImpl.java`

In `processAdjustment()`, after all FR-9 validations pass (§9), evaluate the GIL § 18 k flag:

```java
boolean crossSystemRetroactiveApplies =
    request.getEffectiveDate() != null
    && request.getEffectiveDate().isBefore(debt.getReceivedAt().toLocalDate());
```

Where:
- `request.getEffectiveDate()` is the `LocalDate` from `WriteDownDto.effectiveDate`
- `debt` is the `DebtEntity` loaded by `claimId`
- `debt.getReceivedAt()` returns `LocalDateTime` → `.toLocalDate()` gives the comparison date

Set `crossSystemRetroactiveApplies` in the returned `ClaimAdjustmentResponseDto`. The raw
`debt.getReceivedAt()` value is **not** included in the response DTO.

No HTTP 422 is returned for this condition. The flag is purely informational.

### 6.3 Portal — `AdjustmentReceiptDto` field addition

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/dto/AdjustmentReceiptDto.java`

Add field:

```java
private boolean crossSystemRetroactiveApplies;
```

This field is deserialized from `ClaimAdjustmentResponseDto.crossSystemRetroactiveApplies` in
the JSON response from `DebtServiceClient.submitAdjustment()`.

### 6.4 Portal — `ClaimAdjustmentController` — propagate flag to flash

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/controller/ClaimAdjustmentController.java`

In `submitAdjustment` (POST handler), after successfully calling
`debtServiceClient.submitAdjustment()` and obtaining the `AdjustmentReceiptDto`, add the receipt
to the flash attributes as already done. The `crossSystemRetroactiveApplies` field is carried
inside `AdjustmentReceiptDto`; no separate flash attribute is required.

### 6.5 Portal — `receipt.html` suspension advisory block

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/resources/templates/claims/adjustment/receipt.html`

Add a conditional advisory block after the standard receipt confirmation section:

```html
<!-- FR-6 / GIL § 18 k: Cross-system suspension advisory -->
<div th:if="${receipt.crossSystemRetroactiveApplies}"
     class="skat-alert skat-alert--warning"
     role="note">
  <p th:text="#{adjustment.info.suspension.krydssystem}">
    Restanceinddrivelsesmyndigheden kan suspendere inddrivelsen af denne fordring
    midlertidigt, mens korrektionen behandles, hvis korrektionen ikke kan gennemføres
    straks (GIL § 18 k). Suspensionen ophæves automatisk, når korrektionen er registreret.
  </p>
</div>
```

The advisory is rendered in addition to, not instead of, the standard receipt confirmation.
The standard confirmation is always shown; this advisory is conditional on
`receipt.crossSystemRetroactiveApplies == true`.

### 6.6 Acceptance criteria (FR-6)

| AC | Criterion |
|----|-----------|
| AC-8 | Nedskrivning with `virkningsdato` before `debt.getReceivedAt().toLocalDate()` → `ClaimAdjustmentResponseDto.crossSystemRetroactiveApplies == true` → `AdjustmentReceiptDto.crossSystemRetroactiveApplies == true` → receipt page renders suspension advisory using key `adjustment.info.suspension.krydssystem`. |
| AC-8b | Nedskrivning with `virkningsdato` on or after `debt.getReceivedAt().toLocalDate()` → `crossSystemRetroactiveApplies == false` → suspension advisory absent from receipt page. |
| AC-8c | Standard receipt confirmation is always displayed regardless of `crossSystemRetroactiveApplies`. |

---

## SPEC-FR-7 — Remove RIM-internal reason codes

**Source:** Petition 053 FR-7 · Outcome contract FR-7 · G.A.2.3.4.4  
**Architecture decision:** Decision 3 (ADR 0031 bounded deviation)

### 7.1 Portal — delete `WriteUpReasonCode.java`

**Action:** DELETE  
**File:** `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/dto/WriteUpReasonCode.java`

The enum currently contains `DINDB`, `OMPL`, `AFSK`. All three are RIM-internal initiation codes
(G.A.2.3.4.4) that are definitionally not portal-accessible. After removing all three, the enum
is empty and the file is deleted entirely.

**Before deletion, perform a usage sweep** to confirm that `WriteUpReasonCode` is not referenced
outside `ClaimAdjustmentController` and `form.html`. No other usages are expected per SA
assumption A4.

### 7.2 Portal — `ClaimAdjustmentController` — remove `WriteUpReasonCode` references

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/controller/ClaimAdjustmentController.java`

**In `showAdjustmentForm` (GET handler):**

Remove:

```java
model.addAttribute("allowedReasonCodes", WriteUpReasonCode.allCodes());
```

No replacement model attribute is added for write-up. The write-up form no longer presents
a reason dropdown.

**In `reloadFormWithErrors` helper:**

Remove the corresponding `WriteUpReasonCode.allCodes()` model attribute line.

Remove the write-up reason code allowlist validation block (the block that checks whether the
submitted reason code is in `WriteUpReasonCode.allCodes()`).

### 7.3 Portal — `form.html` — remove write-up reason dropdown

**Action:** MODIFY  
**File:** `opendebt-creditor-portal/src/main/resources/templates/claims/adjustment/form.html`

Remove the entire `<select>` block for write-up reason codes that is bound to `allowedReasonCodes`.
This block is inside a conditional for write-up direction. After FR-7, no reason dropdown exists
for write-up submissions.

Also remove the dead keys `adjustment.reason.AFSK`, `adjustment.reason.DINDB`, and
`adjustment.reason.OMPL` from both `messages_da.properties` and `messages_en_GB.properties`.
These keys exist solely for the write-up reason dropdown that FR-7 removes; they have no remaining
reference after deletion of the dropdown.

### 7.4 Debt-service — denylist enforcement in `ClaimAdjustmentServiceImpl`

**Action:** MODIFY / CREATE  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/service/impl/ClaimAdjustmentServiceImpl.java`

Denylist: `{DINDB, OMPL, AFSK}`

In `processAdjustment()`, before business logic, validate the denylist for write-up requests:

```java
private static final Set<String> RIM_INTERNAL_CODES =
    Set.of("DINDB", "OMPL", "AFSK");

// In processAdjustment():
if (request.getWriteUpReasonCode() != null
        && RIM_INTERNAL_CODES.contains(request.getWriteUpReasonCode())) {
    clsAuditService.record(claimId, adjustmentType, null, FAILURE, creditorId, now());
    throw new CreditorValidationException(
        "Reason code is reserved for RIM-internal operations (G.A.2.3.4.4)"
    );
}
```

HTTP 422 response shape:

```json
{
  "type": "https://opendebt.ufst.dk/problems/validation-failure",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Reason code is reserved for RIM-internal operations (G.A.2.3.4.4)"
}
```

### 7.5 Acceptance criteria (FR-7)

| AC | Criterion |
|----|-----------|
| AC-9 | The portal opskrivning form and all dropdowns contain no option with code `DINDB`, `OMPL`, or `AFSK`. |
| AC-9b | `WriteUpReasonCode.java` does not exist in the portal module after this delivery. |
| AC-13 | `debt-service` returns HTTP 422 for any write-up request carrying `DINDB`, `OMPL`, or `AFSK` as `writeUpReasonCode`. |

---

## SPEC-FR-1/FR-7/FR-9 — Debt-service request DTO

**Source:** Petition 053 FR-1, FR-2, FR-7, FR-9

### 9.0 New debt-service `ClaimAdjustmentRequestDto`

**Action:** CREATE  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/dto/ClaimAdjustmentRequestDto.java`  
**Traceability:** FR-1, FR-2, FR-7, FR-9

`DebtServiceClient` (portal-side) calls `POST /api/v1/debts/{id}/adjustments` with the portal's
`ClaimAdjustmentRequestDto`. The debt-service must bind a corresponding request DTO from that JSON
body. This is a **separate class** in `dk.ufst.opendebt.debtservice.dto` — it is **not** the
portal's same-named class in `dk.ufst.opendebt.creditor.dto`.

| Field | Type | Required | Notes |
|---------------------|------------------------------|-------------|------------------------------------------------------|
| `adjustmentType` | `String` | Yes | `ClaimAdjustmentType` enum name (`WRITE_UP`/`WRITE_DOWN` routing) |
| `amount` | `BigDecimal` | Yes | Must be `> 0` |
| `effectiveDate` | `LocalDate` (ISO-8601) | Yes | Virkningsdato |
| `writeDownReasonCode` | `WriteDownReasonCode` (enum) | Conditional | Required when `adjustmentType` direction == `WRITE_DOWN` (FR-1) |
| `writeUpReasonCode` | `String` | Conditional | Must not be `DINDB`, `OMPL`, or `AFSK` (FR-7) |
| `debtorId` | `String` (UUID) | Conditional | Required for payment-related types with multiple debtors |

---

## SPEC-FR-9 — Backend enforcement independent of portal

**Source:** Petition 053 FR-9 · Outcome contract FR-9 · G.A.1.4.3, G.A.1.4.4, Gæld.bekendtg. § 7

FR-9 is the umbrella requirement that all FR-1..FR-7 constraints are enforced at the
`debt-service` API boundary independently of portal-side validation. Each rule below is a
distinct legal constraint and must be evaluated independently (not short-circuit-combined).

### 9.1 New service interface

**Action:** CREATE  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/service/ClaimAdjustmentService.java`

```java
package dk.ufst.opendebt.debtservice.service;

import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentRequestDto;
import dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentResponseDto;
import java.util.UUID;

public interface ClaimAdjustmentService {
    // ClaimAdjustmentRequestDto here is dk.ufst.opendebt.debtservice.dto.ClaimAdjustmentRequestDto
    // (the debt-service DTO specified in §9.0), not the portal's same-named class.
    ClaimAdjustmentResponseDto processAdjustment(
        UUID claimId,
        ClaimAdjustmentRequestDto request
    );
}
```

### 9.2 New REST controller (debt-service)

**Action:** CREATE  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/controller/ClaimAdjustmentController.java`

```java
@RestController
@RequestMapping("/api/v1/debts/{id}/adjustments")
public class ClaimAdjustmentController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimAdjustmentResponseDto submitAdjustment(
        @PathVariable UUID id,
        @RequestBody @Valid ClaimAdjustmentRequestDto request
    );
}
```

- **Auth:** `CREDITOR` or `ADMIN` role required.
- **Consumes:** `application/json`
- **Produces:** `application/json`
- **Success:** HTTP 201 + `ClaimAdjustmentResponseDto` body
- **Validation failure:** HTTP 422 + ProblemDetail body

> **Note on existing endpoint:** `DebtController.writeDown()` (`POST /{id}/write-down`) is an
> internal caseworker/admin path. It is not the creditor-portal adjustment endpoint. The new
> `ClaimAdjustmentController` formalises the `/api/v1/debts/{id}/adjustments` path.

### 9.3 Validation rules in `ClaimAdjustmentServiceImpl`

**Action:** CREATE / MODIFY  
**File:** `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/service/impl/ClaimAdjustmentServiceImpl.java`

All rules below are evaluated independently. Each produces a separate HTTP 422 with a distinct
`detail` message. No short-circuit evaluation across rules.

| Rule | FR | Trigger condition | Response |
|------|----|-------------------|----------|
| WriteDownReasonCode required | FR-1 | `direction == WRITE_DOWN && (request.writeDownReasonCode == null \|\| not in {NED_INDBETALING, NED_FEJL_OVERSENDELSE, NED_GRUNDLAG_AENDRET})` | HTTP 422, `detail: "WriteDownReasonCode is required and must be one of the legal values"` |
| RENTE + OPSKRIVNING_REGULERING | FR-2 | `adjustmentType == OPSKRIVNING_REGULERING && claim.claimCategory == RENTE` | HTTP 422, `detail: "RENTE claims must use a rentefordring, not an opskrivningsfordring (G.A.1.4.3)"` |
| RIM-internal reason codes | FR-7 | `writeUpReasonCode ∈ {DINDB, OMPL, AFSK}` | HTTP 422, `detail: "Reason code is reserved for RIM-internal operations (G.A.2.3.4.4)"` |
| Høring timing rule | FR-3 | `claim.lifecycleState == HOERING` | Set opskrivningsfordring receipt timestamp = høring resolution time. No HTTP 422. |
| Retroactive log marker | FR-4 | `request.effectiveDate < LocalDate.now()` | Log WARN with structured marker. No HTTP 422. |
| GIL § 18 k evaluation | FR-6 | `request.effectiveDate < debt.getReceivedAt().toLocalDate()` | Set `crossSystemRetroactiveApplies = true` in response. No HTTP 422. |

**FR-6 comparison expression (exact):**

```java
boolean crossSystemRetroactiveApplies =
    request.getEffectiveDate() != null
    && request.getEffectiveDate().isBefore(debt.getReceivedAt().toLocalDate());
```

### 9.4 CLS audit pattern

Called on **both** success and failure paths. A failed CLS call must not suppress the adjustment
response — log and continue.

```java
clsAuditService.record(
    claimId,
    adjustmentType,      // ClaimAdjustmentType enum value
    reasonCode,          // WriteDownReasonCode or null
    outcome,             // SUCCESS or FAILURE
    creditorId,
    Instant.now()
);
```

On validation failure, the CLS call is made from the exception handler or finally block before
throwing `CreditorValidationException`.

### 9.5 HTTP 422 error response shape

All HTTP 422 responses use RFC 7807 ProblemDetail format:

```json
{
  "type": "https://opendebt.ufst.dk/problems/validation-failure",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "<rule-specific detail message as listed in §9.3>"
}
```

### 9.6 Acceptance criteria (FR-9)

| AC | Criterion |
|----|-----------|
| AC-11 | `debt-service` returns HTTP 422 for `WriteDownDto` with `reasonCode == null` (direct API call, no portal). |
| AC-11b | `debt-service` returns HTTP 422 for `WriteDownDto` with `reasonCode == "UNKNOWN_CODE"`. |
| AC-12 | `debt-service` returns HTTP 422 for write-up of type `OPSKRIVNING_REGULERING` on a `RENTE`-category claim. |
| AC-13 | `debt-service` returns HTTP 422 for write-up with `writeUpReasonCode ∈ {DINDB, OMPL, AFSK}`. |
| AC-9d | `debt-service` accepts write-down with `reasonCode == NED_GRUNDLAG_AENDRET` and returns HTTP 201. |
| AC-16 | CLS audit entry created for all adjustment submissions (success and failure). |

---

## SPEC-i18n — New message keys

**Source:** Outcome contract FR-1, FR-4, FR-5, FR-6 · Solution architecture §8

**Files to modify:**
- `opendebt-creditor-portal/src/main/resources/messages_da.properties`
- `opendebt-creditor-portal/src/main/resources/messages_en_GB.properties`

Both files must receive all 7 new keys. The CI bundle-lint check fails the build if any key
present in `messages_da.properties` is absent from `messages_en_GB.properties` or vice versa.

| Key | DA value | EN value |
|-----|----------|----------|
| `adjustment.reason.ned.indbetaling` | `Direkte indbetaling til fordringshaver` | `Direct payment to the creditor` |
| `adjustment.reason.ned.fejl_oversendelse` | `Fejlagtig oversendelse til inddrivelse` | `Erroneous referral for debt collection` |
| `adjustment.reason.ned.grundlag_aendret` | `Opkrævningsgrundlaget har ændret sig` | `The assessment basis has changed` |
| `adjustment.validation.reason.required` | `Vælg venligst en årsag til nedskrivningen` _(existing key — current value `Årsag er påkrævet.` replaced with `Vælg venligst en årsag til nedskrivningen`)_ | `Please select a reason for the write-down` |
| `adjustment.info.retroaktiv.virkningsdato` | `Virkningsdato er i fortiden. Gældsstyrelsen er forpligtet til at omfordele alle dækninger foretaget efter denne dato og genberegne renter for den berørte periode. Behandlingen kan tage ekstra tid. Se G.A.1.4.4.` | `The effective date is in the past. The Danish Debt Collection Authority is obliged to reassign all coverages made after this date and recalculate interest for the affected period. Processing may take additional time. See G.A.1.4.4.` |
| `adjustment.type.description.omgjort_nedskrivning_regulering` | `Opskrivning af annulleret nedskrivning: opskrivningsfordringen anses for modtaget på samme tidspunkt som den fordring, der fejlagtigt blev nedskrevet (G.A.1.4.3).` | `Write-up reversing a cancelled write-down: the write-up claim is treated as received at the same time as the claim that was incorrectly written down (G.A.1.4.3).` |
| `adjustment.info.suspension.krydssystem` | `Restanceinddrivelsesmyndigheden kan suspendere inddrivelsen af denne fordring midlertidigt, mens korrektionen behandles, hvis korrektionen ikke kan gennemføres straks (GIL § 18 k). Suspensionen ophæves automatisk, når korrektionen er registreret.` | `The Danish Debt Collection Authority may temporarily suspend collection of this claim while the correction is being processed, if it cannot be completed immediately (GIL § 18 k). The suspension will be lifted automatically when the correction has been registered.` |

> **Not a new key:** `adjustment.validation.reason.required` already exists in
> `messages_da.properties` as `Årsag er påkrævet.` Its DA value is **replaced** with the more
> specific write-down wording `Vælg venligst en årsag til nedskrivningen`. Both bundles must
> receive the updated value.

> **Not a new key:** `adjustment.label.reason` (`Årsag / årsagskode`) already exists and is
> reused as the label for the write-down dropdown. No new label key is introduced.

---

## API interface contract

**Endpoint:** `POST /api/v1/debts/{id}/adjustments`  
**Auth:** `CREDITOR` or `ADMIN` role  
**Consumes:** `application/json`  
**Produces:** `application/json`

### Request body schema

```json
{
  "adjustmentType": "WRITE_DOWN",
  "amount": 500.00,
  "effectiveDate": "2022-12-01",
  "writeDownReasonCode": "NED_INDBETALING",
  "writeUpReasonCode": null,
  "debtorId": "optional-debtor-uuid"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `adjustmentType` | `String` (enum name) | Yes | Must be a valid `ClaimAdjustmentType` value |
| `amount` | `BigDecimal` | Yes | `> 0.00` |
| `effectiveDate` | `String` (ISO-8601 date) | Yes | Must not be null |
| `writeDownReasonCode` | `String` (enum name) | Conditional | Required when `adjustmentType` direction is WRITE_DOWN; must be `NED_INDBETALING`, `NED_FEJL_OVERSENDELSE`, or `NED_GRUNDLAG_AENDRET` |
| `writeUpReasonCode` | `String` | Conditional | Must NOT be `DINDB`, `OMPL`, or `AFSK`; null or absent is valid for write-up |
| `debtorId` | `String` (UUID) | Conditional | Required for payment-related types with multiple debtors |

### Success response — HTTP 201

```json
{
  "actionId": "ACT-20240101-00042",
  "status": "ACCEPTED",
  "amount": 500.00,
  "crossSystemRetroactiveApplies": true
}
```

### Error response — HTTP 422

```json
{
  "type": "https://opendebt.ufst.dk/problems/validation-failure",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "<rule-specific message>"
}
```

---

## Deliverables checklist

| # | File | Action | FR |
|---|------|--------|----|
| D-1 | `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/dto/WriteDownReasonCode.java` | CREATE | FR-1 |
| D-2 | `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/dto/ClaimAdjustmentRequestDto.java` | MODIFY — replace `String reason` with `WriteDownReasonCode writeDownReasonCode` | FR-1 |
| D-3 | `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/dto/WriteUpReasonCode.java` | DELETE — remove DINDB, OMPL, AFSK; enum empty → file deleted | FR-7 |
| D-4 | `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/dto/AdjustmentReceiptDto.java` | MODIFY — add `boolean crossSystemRetroactiveApplies` | FR-6 |
| D-5 | `opendebt-creditor-portal/src/main/java/dk/ufst/opendebt/creditor/controller/ClaimAdjustmentController.java` | MODIFY — see §1.4, §4.1, §6.4, §7.2 | FR-1,4,6,7 |
| D-6 | `opendebt-creditor-portal/src/main/resources/templates/claims/adjustment/form.html` | MODIFY — see §1.5, §4.2, §5.1, §7.3 | FR-1,4,5,7 |
| D-7 | `opendebt-creditor-portal/src/main/resources/templates/claims/adjustment/receipt.html` | MODIFY — see §6.5 | FR-6 |
| D-8 | `opendebt-creditor-portal/src/main/resources/messages_da.properties` | MODIFY — ADD 6 new keys; UPDATE value of `adjustment.validation.reason.required` (existing key, value replaced); REMOVE dead keys `adjustment.reason.AFSK`, `adjustment.reason.DINDB`, `adjustment.reason.OMPL` | FR-1,4,5,6,7 |
| D-9 | `opendebt-creditor-portal/src/main/resources/messages_en_GB.properties` | MODIFY — ADD 6 new keys; UPDATE value of `adjustment.validation.reason.required` (existing key, value replaced); REMOVE dead keys `adjustment.reason.AFSK`, `adjustment.reason.DINDB`, `adjustment.reason.OMPL` | FR-1,4,5,6,7 |
| D-10a | `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/dto/ClaimAdjustmentRequestDto.java` | CREATE — debt-service request DTO (see §9.0); distinct from portal's same-named class | FR-1, FR-2, FR-7, FR-9 |
| D-10 | `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/dto/WriteDownReasonCode.java` | CREATE | FR-1,9 |
| D-11 | `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/dto/WriteDownDto.java` | CREATE (or MODIFY if partial baseline) | FR-1,9 |
| D-12 | `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/dto/ClaimAdjustmentResponseDto.java` | CREATE | FR-6,9 |
| D-13 | `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/service/ClaimAdjustmentService.java` | CREATE | FR-9 |
| D-14 | `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/service/impl/ClaimAdjustmentServiceImpl.java` | CREATE — all FR-9 validation rules, GIL §18k flag, retroactive log marker, CLS audit | FR-1,2,3,4,6,7,9 |
| D-15 | `opendebt-debt-service/src/main/java/dk/ufst/opendebt/debtservice/controller/ClaimAdjustmentController.java` | CREATE — `POST /api/v1/debts/{id}/adjustments` | FR-9 |
| D-16 | `api-specs/openapi-debt-service.yaml` | MODIFY — add `POST /api/v1/debts/{id}/adjustments` path; add `ClaimAdjustmentRequestDto` and `ClaimAdjustmentResponseDto` schemas (including `crossSystemRetroactiveApplies`). **Required per ADR 0004 (API-First) before implementation begins.** | FR-9, FR-1, FR-6 |

---

## Traceability matrix

| FR | Gherkin scenarios (feature file) | Portal deliverable | Debt-service deliverable |
|----|----------------------------------|--------------------|--------------------------|
| FR-1 | Scenarios 001–005 (FR-1 block) | D-1, D-2, D-5, D-6 | D-10a, D-10, D-11, D-14 |
| FR-4 | Scenarios 006–008 (FR-4 block) | D-5, D-6, D-8, D-9 | D-14 (log marker) |
| FR-5 | Scenarios 009–010 (FR-5 block) | D-6, D-8, D-9 | none |
| FR-6 | Scenarios 011–012 (FR-6 block) | D-4, D-5, D-7, D-8, D-9 | D-12, D-14, D-16 |
| FR-7 | Scenario 013 (FR-7 block) | D-3, D-5, D-6 | D-14 |
| FR-9 | Scenarios 014–022 (FR-9 block) | none | D-13, D-14, D-15, D-16 |
| AC-16 (CLS audit) | CLS scenarios (AC-16 block) | none | D-14 |
| AC-14 (i18n) | CI bundle-lint (not Gherkin) | D-8, D-9 | none |

> **Scenario numbering** above corresponds to the order of scenarios in
> `petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.feature` as filed.

---

## Out of scope (explicit exclusions)

The following items are **not specified here** and must not be implemented as part of this petition:

| Item | Reference | Tracked in |
|------|-----------|------------|
| Full retroactive timeline replay (dækning reassignment + interest recalculation) | G.A.1.4.4, GIL § 18 l | TB-038 |
| Rentegodtgørelse — Gældsstyrelsen's discretionary prospective virkningsdato option | GIL § 18 l, G.A.1.4.4 | TB-039 |
| Lønindeholdelse limitation wording at confirmation step | G.A.3.1.2.1.3 | Retained from P034 |
| Client-side JS advisory for retroactive virkningsdato (without POST round-trip) | — | Additive; not architecturally required |
| Interne opskrivninger (DINDB/OMPL/AFSK) as portal-accessible actions | G.A.2.3.4.4 | Definitionally excluded |

---

## Validation checklist

- [x] Every FR in implementation delta (FR-1, FR-4, FR-5, FR-6, FR-7, FR-9) has a specification section
- [x] Every specification traces to petition FR, outcome-contract AC, and/or Gherkin scenario
- [x] All interfaces are testable and unambiguous (exact field names, types, comparison expressions)
- [x] Non-functional requirements included only where petition specifies them (WCAG aria attributes, i18n)
- [x] Zero items beyond petition, requirements-doc, outcome-contract, and SA
- [x] Every specification enables implementation or testing
- [x] No vague language ("should", "might", "could") — all requirements use "must" or exact code
- [x] No invented features or constraints
- [x] Baseline FRs (FR-2, FR-3, FR-8) are not re-specified here; existing implementation is retained
