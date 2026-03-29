# Petition 053 Outcome Contract

## Petition reference

**Petition 053:** Fordringshaverportal — Opskrivning og nedskrivning (fuld G.A.-komplient specifikation)
**Supersedes:** Petition 034 (retained as baseline for FR-2, FR-3, FR-8)
**Legal basis:** G.A.1.4.3, G.A.1.4.4, G.A.2.3.4.4, Gæld.bekendtg. § 7 stk. 1–2, GIL § 18 k

> **Baseline implementation note:** FR-2 (RENTE rejection) and FR-3 (høring banner) were
> partially implemented in the compliance-fixes sprint and are retained as acceptance criteria
> here for completeness. FR-8 (permission-based type filtering) was implemented in petition 034.
> The remaining P053 scope is FR-1, FR-4, FR-5, FR-6, FR-7, and FR-9.

---

## Observable outcomes by functional requirement

### FR-1: Nedskrivning — controlled reason selection
*(Gæld.bekendtg. § 7, stk. 2 / G.A.1.4.4 — P053 scope remaining)*

**Preconditions**
- User holds role `CREDITOR_EDITOR` and permission `allow_portal_actions`.
- The creditor agreement grants the nedskrivning permission flag.
- The fordring is under inddrivelse.

**Trigger**
- User navigates to the nedskrivning (write-down) adjustment form.

**Expected portal behaviour**
- The reason field is rendered as a `<select>` dropdown containing exactly three options:
  `NED_INDBETALING`, `NED_FEJL_OVERSENDELSE`, `NED_GRUNDLAG_AENDRET`.
- Free-text input for the reason is not present.
- On form submission with no reason selected, the form is rejected client-side before the BFF
  call is made and the reason field is marked invalid.
- On form submission with an option outside the three listed above, the form is rejected.
- A valid selection results in the `WriteDownReasonCode` being forwarded to debt-service as part
  of the adjustment message.

**Expected backend behaviour**
- `debt-service` independently validates that `WriteDownDto.reasonCode` is one of
  `NED_INDBETALING`, `NED_FEJL_OVERSENDELSE`, `NED_GRUNDLAG_AENDRET`.
- A request missing `reasonCode` or carrying an unrecognised value returns HTTP 422.
- The free-text `WriteDownDto.reason` field is no longer accepted as the sole reason carrier.

**Expected user messaging (i18n keys)**

| Key | Usage |
|-----|-------|
| `adjustment.reason.ned.indbetaling` | Dropdown label: "Direkte indbetaling til fordringshaver" |
| `adjustment.reason.ned.fejl_oversendelse` | Dropdown label: "Fejlagtig oversendelse til inddrivelse" |
| `adjustment.reason.ned.grundlag_aendret` | Dropdown label: "Opkrævningsgrundlaget har ændret sig" |
| `adjustment.validation.reason.required` | Inline validation error when no reason is selected |

---

### FR-2: Opskrivning — opkrævningsrente exception
*(G.A.1.4.3, 3. pkt. — baseline implemented in compliance-fixes sprint)*

**Preconditions**
- User holds role `CREDITOR_EDITOR` and permission `allow_portal_actions`.
- The fordring has claim category `RENTE` (opkrævningsrente).

**Trigger**
- User navigates to the adjustment form and selects or is routed to the
  `OPSKRIVNING_REGULERING` path.

**Expected portal behaviour**
- The portal rejects the `OPSKRIVNING_REGULERING` path and does not display the write-up form.
- A user-facing message is shown instructing the fordringshaver to submit a ny rentefordring
  via the claim creation flow (petition 002) instead.
- The form does not submit to debt-service.

**Expected backend behaviour**
- `debt-service` independently rejects a write-up of type `OPSKRIVNING_REGULERING` on a
  `RENTE`-category claim, returning HTTP 422.

**Expected user messaging (i18n keys)**

| Key | Usage |
|-----|-------|
| `adjustment.validation.type.rentefordring` | Error message explaining rentefordring requirement (already present in `messages_da.properties`) |

---

### FR-3: Opskrivning — høring timing banner
*(G.A.1.4.3, Gæld.bekendtg. § 7 stk. 1, 4. pkt. — baseline implemented in compliance-fixes sprint)*

**Preconditions**
- User navigates to the adjustment form for a fordring.
- The fordring's `lifecycleState` is `"HOERING"`.

**Trigger**
- The adjustment form page is rendered (GET request to the form URL).

**Expected portal behaviour**
- A persistent informational banner is displayed before the form fields.
- The banner text explains that an opskrivning submitted while in høring is only considered
  received when the høring is resolved, and that interest on the upwritten amount begins at
  that time.
- The banner carries `role="status"` for WCAG 2.1 AA compliance.
- The form remains submittable; the banner is purely informational.
- The banner is shown for both write-up and write-down operations.

**Expected backend behaviour**
- `debt-service` applies the høring timing rule: the opskrivningsfordring's registration
  timestamp is set to the time of høring resolution, not the portal submission time.

**Expected user messaging (i18n keys)**

| Key | Usage |
|-----|-------|
| `adjustment.info.hoering` | Banner text explaining høring timing rule (already present in `messages_da.properties`) |

---

### FR-4: Retroactive nedskrivning — user guidance
*(G.A.1.4.4 — P053 scope remaining)*

**Preconditions**
- User is on the nedskrivning adjustment form.

**Trigger**
- The value entered in the `virkningsdato` field is earlier than the current calendar date
  (server-side and client-side check both applicable).

**Expected portal behaviour**
- An inline advisory message appears immediately below the `virkningsdato` field.
- The advisory text explains that all dækninger since that date will be reassigned and renter
  recalculated, and that processing may take extra time.
- The advisory references G.A.1.4.4.
- The advisory uses `aria-live="polite"` for WCAG 2.1 AA compliance.
- The advisory does not block form submission.

**Expected backend behaviour**
- `debt-service` logs retroactive nedskrivninger (where `virkningsdato < today`) separately
  (e.g. via a dedicated log marker or metric) to enable operational monitoring.

**Expected user messaging (i18n keys)**

| Key | Usage |
|-----|-------|
| `adjustment.info.retroaktiv.virkningsdato` | Inline advisory text below virkningsdato when date is in the past |

---

### FR-5: Annulleret nedskrivning — backdated opskrivning type description
*(G.A.1.4.3, Gæld.bekendtg. § 7 stk. 1, 5. pkt. — P053 scope remaining)*

**Preconditions**
- User is on the opskrivning adjustment form.
- User selects or the form is pre-selected to type `OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING`.

**Trigger**
- The type `OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING` is displayed or selected.

**Expected portal behaviour**
- The form displays a type description text beneath the type selector (or alongside it) that
  explains the backdating effect: the opskrivningsfordring is treated as received at the same
  time as the original fordring that was incorrectly nedskrevet, not at the time of portal
  submission.
- The text references G.A.1.4.3.

**Expected backend behaviour**
- No additional backend behaviour beyond what is already implemented for this type.

**Expected user messaging (i18n keys)**

| Key | Usage |
|-----|-------|
| `adjustment.type.description.omgjort_nedskrivning_regulering` | Type description text explaining backdating to original fordring reception time |

---

### FR-6: Cross-system retroactive nedskrivning — suspension advisory
*(G.A.1.4.4, GIL § 18 k — P053 scope remaining)*

**Preconditions**
- A nedskrivning submission has been completed successfully.
- The submitted `virkningsdato` predates the fordring's PSRM registration date (i.e. the fordring
  was previously held in another inddrivelsessystem such as DMI).

**Trigger**
- The receipt page is rendered after a successful cross-system retroactive nedskrivning.

**Expected portal behaviour**
- The receipt page displays an advisory informing the fordringshaver that RIM may temporarily
  suspend inddrivelsen while the correction is being processed if it cannot be completed
  immediately (GIL § 18 k).
- The advisory states that the suspension will be lifted automatically when the correction is
  registered.
- The advisory is displayed in addition to the standard receipt confirmation; it does not
  replace it.

**Expected backend behaviour**
- `debt-service` provides sufficient data in the adjustment response (or the existing claim
  detail) to allow the portal to determine whether `virkningsdato` predates the PSRM
  registration date.

**Expected user messaging (i18n keys)**

| Key | Usage |
|-----|-------|
| `adjustment.info.suspension.krydssystem` | Advisory text on receipt page referencing GIL § 18 k |

---

### FR-7: Scope exclusion — interne opskrivningskoder fjernet fra portalen
*(G.A.2.3.4.4 — P053 scope remaining)*

**Preconditions**
- Portal renders any opskrivning form or type-selection element.

**Trigger**
- Any rendering of the opskrivning form, type dropdown, or reason-code selection.

**Expected portal behaviour**
- The codes `DINDB`, `OMPL`, and `AFSK` are absent from all form elements, dropdowns, and
  selectable option lists in the portal.
- The `WriteUpReasonCode` enum (if retained) does not contain `DINDB`, `OMPL`, or `AFSK`.
  If the enum becomes empty, it is deleted entirely.
- No user-visible label or option corresponds to any RIM-internal opskrivningskode.

**Expected backend behaviour**
- `debt-service` rejects any write-up request carrying `DINDB`, `OMPL`, or `AFSK` in the
  reason code field, returning HTTP 422.

**Expected user messaging (i18n keys)**
- None (absence requirement — no user-facing message is defined for these codes).

---

### FR-8: Update type availability per creditor agreement
*(Retained from petition 034 — baseline implemented)*

**Preconditions**
- User has role `CREDITOR_EDITOR` and `allow_portal_actions`.

**Trigger**
- User navigates to the adjustment form for a fordring.

**Expected portal behaviour**
- Available adjustment types are filtered by the creditor agreement's
  `grantedAdjustmentPermissions` flags.
- FR-1 through FR-7 constraints apply on top of, and do not replace, this permission-based
  filtering.

**Expected backend behaviour**
- `debt-service` independently enforces permission-based type availability.

**Expected user messaging (i18n keys)**
- Per petition 034 (no new keys).

---

### FR-9: Backend enforcement independent of portal
*(G.A.1.4.3, G.A.1.4.4, Gæld.bekendtg. § 7 — P053 scope remaining)*

**Preconditions**
- Any caller (portal BFF or direct API client) submits an adjustment request to `debt-service`.

**Trigger**
- An HTTP POST to the debt-service adjustment endpoint.

**Expected portal behaviour**
- Not applicable (backend-only requirement).

**Expected backend behaviour**
- FR-1 enforcement: `WriteDownDto` without a valid `WriteDownReasonCode` → HTTP 422.
- FR-2 enforcement: Write-up of type `OPSKRIVNING_REGULERING` on a `RENTE`-category claim
  → HTTP 422.
- FR-3 enforcement: Høring timing rule applied server-side regardless of portal submission.
- FR-7 enforcement: Write-up request carrying `DINDB`, `OMPL`, or `AFSK` → HTTP 422.
- All backend validations apply independently of portal-side validation.

**Expected user messaging (i18n keys)**
- None at the backend layer; error responses use structured problem-detail bodies.

---

## Acceptance criteria

1. The nedskrivning form presents reason as a dropdown with exactly three options:
   `NED_INDBETALING`, `NED_FEJL_OVERSENDELSE`, `NED_GRUNDLAG_AENDRET` (FR-1).
2. Submitting the nedskrivning form without a selected reason code is rejected by the portal with
   a validation error before the BFF call is made (FR-1).
3. The selected `WriteDownReasonCode` is forwarded to debt-service in the adjustment message (FR-1).
4. Navigating to `OPSKRIVNING_REGULERING` for a `RENTE`-category claim displays a rejection
   message instead of the form; the form is not submitted (FR-2, baseline).
5. When the fordring's `lifecycleState` is `"HOERING"`, a persistent informational banner using
   `role="status"` is displayed on the adjustment form before the form fields; the form remains
   submittable (FR-3, baseline).
6. Entering a `virkningsdato` earlier than today on the nedskrivning form causes an inline
   advisory with `aria-live="polite"` to appear; submission is not blocked (FR-4).
7. When adjustment type is `OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING`, a backdating
   description is shown alongside the type selector (FR-5).
8. When a completed nedskrivning submission has a `virkningsdato` predating the fordring's PSRM
   registration date, the receipt page displays the cross-system suspension advisory referencing
   GIL § 18 k (FR-6).
9. `DINDB`, `OMPL`, and `AFSK` do not appear anywhere in the portal's opskrivning form,
   dropdowns, or option lists; `WriteUpReasonCode` does not contain these values (FR-7).
10. Available adjustment types continue to be governed by the creditor agreement's permission
    flags; FR-1 through FR-7 add legal constraints on top of this filtering (FR-8, baseline).
11. `debt-service` returns HTTP 422 for any adjustment request missing a valid `WriteDownReasonCode`
    (FR-9 / FR-1 backend).
12. `debt-service` returns HTTP 422 for a write-up of `OPSKRIVNING_REGULERING` on a `RENTE` claim
    (FR-9 / FR-2 backend).
13. `debt-service` returns HTTP 422 for any write-up request carrying `DINDB`, `OMPL`, or `AFSK`
    (FR-9 / FR-7 backend).
14. All new reason-code labels are internationalised via Spring message bundles in both
    `messages_da.properties` and `messages_en_GB.properties`.
    > **Verification method:** AC-14 is verified by the CI bundle-lint check (which fails the
    > build if any key present in `messages_da.properties` is absent from `messages_en_GB.properties`
    > and vice versa), not by a Gherkin scenario. This exemption is explicit; AC-17 does not apply
    > to AC-14.
15. The høring banner meets WCAG 2.1 AA (`role="status"`); the retroactive advisory meets WCAG 2.1
    AA (`aria-live="polite"`).
16. All adjustment submissions — successful or failed — are logged to the audit log (CLS).
17. Every acceptance criterion is covered by at least one Gherkin scenario.

---

## Definition of done

- `WriteDownDto.reason` (free-text) is replaced by `WriteDownDto.reasonCode` (`WriteDownReasonCode` enum).
- `WriteDownReasonCode` enum contains exactly `NED_INDBETALING`, `NED_FEJL_OVERSENDELSE`, `NED_GRUNDLAG_AENDRET`.
- The nedskrivning `form.html` renders the reason as a `<select>` dropdown; no free-text input is present.
- Portal rejects nedskrivning submission without a valid reason code before the BFF call.
- `debt-service` validates `WriteDownReasonCode` at the API boundary and returns 422 on violation.
- `DINDB`, `OMPL`, `AFSK` are removed from `WriteUpReasonCode`; enum deleted if empty.
- `debt-service` returns 422 for any write-up carrying a removed RIM-internal code.
- Inline retroactive advisory appears below `virkningsdato` when date < today; does not block submission.
- `OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING` type description text is shown in form.
- Cross-system suspension advisory appears on receipt when `virkningsdato` < PSRM registration date.
- `adjustment.info.retroaktiv.virkningsdato`, `adjustment.type.description.omgjort_nedskrivning_regulering`,
  and `adjustment.info.suspension.krydssystem` are present in both DA and EN message bundles.
- `adjustment.reason.ned.indbetaling`, `adjustment.reason.ned.fejl_oversendelse`,
  `adjustment.reason.ned.grundlag_aendret`, and `adjustment.validation.reason.required` are present
  in both DA and EN message bundles.
- Høring banner retains `role="status"`; retroactive advisory uses `aria-live="polite"`.
- All adjustment operations (success and failure) are logged to the audit log.
- `behave --dry-run` passes on `petitions/petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.feature`.

---

## Success metrics

| Metric | Target |
|--------|--------|
| Nedskrivning dropdown contains exactly 3 legal reason codes | 100% |
| Nedskrivning submission without reason code blocked at portal | 100% |
| Nedskrivning submission with valid reason code forwarded to debt-service | 100% |
| debt-service 422 on missing/invalid WriteDownReasonCode | 100% |
| RENTE + OPSKRIVNING_REGULERING rejected in portal and backend | 100% |
| Høring banner displayed with `role="status"` when lifecycleState = HOERING | 100% |
| Retroactive advisory displayed with `aria-live="polite"` when virkningsdato < today | 100% |
| Backdating description shown for OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING | 100% |
| Cross-system suspension advisory on receipt page when applicable | 100% |
| DINDB, OMPL, AFSK absent from portal and rejected by backend | 100% |
| New i18n keys present in DA and EN message bundles | All 7 new keys |
| Audit log entries for all adjustment operations | 100% |

---

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| `WriteDownReasonCode` enum | `opendebt-creditor-portal/src/main/java/.../dto/WriteDownReasonCode.java` |
| Updated `WriteDownDto` (reasonCode field replaces reason) | `opendebt-creditor-portal/src/main/java/.../dto/WriteDownDto.java` |
| Updated `WriteUpReasonCode` (DINDB/OMPL/AFSK removed) | `opendebt-creditor-portal/src/main/java/.../dto/WriteUpReasonCode.java` |
| Updated nedskrivning form template (dropdown reason) | `opendebt-creditor-portal/src/main/resources/templates/claims/adjustment/form.html` |
| Updated `ClaimAdjustmentController` (retroactive advisory, FR-5 description, FR-6 receipt) | `opendebt-creditor-portal/src/main/java/.../controller/ClaimAdjustmentController.java` |
| `WriteDownReasonCode` enum (debt-service) | `opendebt-debt-service/src/main/java/.../dto/WriteDownReasonCode.java` |
| Updated `WriteDownDto` (debt-service) | `opendebt-debt-service/src/main/java/.../dto/WriteDownDto.java` |
| Backend validation logic (FR-9 enforcement) | `opendebt-debt-service/src/main/java/.../service/ClaimAdjustmentService.java` |
| Danish message bundle additions (7 new keys) | `opendebt-creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle additions (7 new keys) | `opendebt-creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition053-fordringshaverportal-opskrivning-nedskrivning-fuld-spec.feature` |

---

## Failure conditions

- The nedskrivning form presents a free-text field instead of a dropdown for the reason.
- The dropdown contains fewer or more than three reason-code options.
- A nedskrivning submission with no reason selected reaches the BFF without portal rejection.
- `debt-service` accepts a `WriteDownDto` with a missing or unrecognised `reasonCode`.
- `DINDB`, `OMPL`, or `AFSK` appears in any portal form element, dropdown, or option list.
- `debt-service` accepts a write-up request carrying `DINDB`, `OMPL`, or `AFSK`.
- `WriteUpReasonCode` still contains `DINDB`, `OMPL`, or `AFSK` after this petition is done.
- A `RENTE`-category claim can be submitted through `OPSKRIVNING_REGULERING` without rejection.
- The høring banner is absent when `lifecycleState == "HOERING"`, or is missing `role="status"`.
- The retroactive advisory does not appear when `virkningsdato < today`, or blocks submission.
- The retroactive advisory does not carry `aria-live="polite"`.
- The backdating description is absent when type is `OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING`.
- The cross-system suspension advisory is absent from the receipt page when applicable.
- Any of the 7 new i18n keys is missing from DA or EN message bundles.
- Adjustment operations (successful or failed) are not logged to the audit log.
- `behave --dry-run` fails on the feature file.
