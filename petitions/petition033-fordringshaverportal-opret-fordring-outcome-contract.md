# Petition 033 Outcome Contract

## Acceptance criteria

1. The creditor portal provides a multi-step wizard for creating and submitting new claims for debt recovery.
2. The wizard guides the creditor through four steps: debtor identification, claim details entry, review, and submission.
3. The wizard is only accessible to users with `CREDITOR_EDITOR` role AND the creditor agreement flags `allow_portal_actions` AND `allow_create_recovery_claims`.
4. In Step 1 the creditor selects debtor type: CPR, CVR, SE, or AKR.
5. For CPR debtors, the creditor enters a CPR number and a name; the BFF calls `person-registry` to verify the CPR; first and last name from input are compared case-insensitively and accent-stripped with the person-registry response; verification succeeds only if both names match.
6. CPR lookups are throttled per user per birth date to prevent abuse.
7. For CVR/SE debtors, the creditor enters an SE or CVR number; the BFF calls `person-registry` (or external CVR service) to verify; company information is returned if valid.
8. For AKR debtors, the creditor enters the AKR number (alternative contact register).
9. In Step 2 the claim data entry form presents fields based on the creditor agreement's allowed claim types: fordringstype, beloeb, hovedstol, fordringshaver-reference, beskrivelse (free text, max 100 characters, no PII per GDPR), fordringsperiode, stiftelsesdato, forfaldsdato, sidste rettidige betalingsdato, foraeldelsesdato, bobehandling, domsdato, forligsdato, rentevalg, fordringsnote, and kundenote.
10. Field visibility and requirement levels are driven by the fordringstype configuration in the creditor agreement.
11. Interest rule options are limited to those allowed by the creditor agreement.
12. In Step 3 a review step displays a read-only summary of all entered data before submission.
13. The creditor explicitly confirms submission before data is sent.
14. The BFF submits the claim to `debt-service`, which evaluates it against indrivelsesparathed rules (petitions 015-018).
15. If the claim is accepted (UDFOERT), a receipt page displays the assigned fordrings-ID, processing status, and a summary of submitted data.
16. If the claim is rejected (AFVIST), the wizard displays validation errors with error codes and Danish descriptions, allowing the creditor to correct and resubmit.
17. If the claim enters hearing (HOERING), the wizard informs the creditor that the claim is pending review and can be found in the hearing list (petition 031).
18. The creditor agreement is loaded from `creditor-service` through the BFF.
19. The creditor agreement determines: allowed claim types, allowed debtor types, interest rules and rates, and whether portal actions are allowed.
20. The creditor agreement is cacheable with a manual refresh option.
21. The "Opret fordring" navigation item only appears when the user has `CREDITOR_EDITOR` role AND the creditor agreement allows portal actions and claim creation.
22. All claim submissions are logged to the audit log with the full payload (excluding PII).
23. The wizard uses the SKAT standardlayout.
24. Breadcrumb: Forside > Opret fordring.
25. The wizard uses a step indicator showing current step and total steps.
26. Each form step uses accessible form patterns: visible labels, field descriptions, inline validation errors with `aria-describedby`.
27. The CPR input field has appropriate `inputmode` and pattern for numeric entry.
28. The beskrivelse field displays a character counter (max 100).
29. The review step is a read-only summary, not an editable form.
30. All user-facing text uses message bundles (petition 021) with Danish and English translations.

## Definition of done

- The creditor portal renders a four-step claim creation wizard: debtor identification, claim data entry, review, and submission.
- Step 1 supports CPR, CVR, SE, and AKR debtor types with verification against `person-registry` through the BFF.
- CPR verification matches first and last names case-insensitively and accent-stripped; lookups are throttled per user per birth date.
- CVR/SE verification returns company information; AKR entry is accepted without external verification.
- Step 2 presents claim data entry fields driven by the creditor agreement's fordringstype configuration; field visibility and requirement levels reflect the agreement.
- Interest rule options are limited to those allowed by the creditor agreement.
- The beskrivelse field enforces a 100-character limit with a character counter and GDPR PII warning.
- Step 3 presents a read-only review summary and requires explicit confirmation before submission.
- Submission to `debt-service` is routed through the BFF; the portal does not call `debt-service` directly.
- UDFOERT submissions display a receipt page with fordrings-ID, status, and submitted data summary.
- AFVIST submissions display validation errors with error codes and Danish descriptions; the creditor can correct and resubmit.
- HOERING submissions inform the creditor that the claim is pending review and direct them to the hearing list.
- The creditor agreement is loaded from `creditor-service` through the BFF and is cacheable with manual refresh.
- The "Opret fordring" navigation item is conditionally shown based on role and agreement permissions.
- All claim submissions are logged to the audit log (excluding PII).
- The wizard uses the SKAT standardlayout with breadcrumb: Forside > Opret fordring.
- A step indicator shows the current step and total steps.
- All form steps use accessible patterns: visible labels, field descriptions, inline validation with `aria-describedby`.
- The CPR input has numeric `inputmode` and pattern.
- All user-facing text is externalized to message bundles with Danish and English translations.
- The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Success metrics

| Metric | Target |
|--------|--------|
| Wizard renders all four steps in correct order | 100% |
| CPR verification validates first and last name match (case-insensitive, accent-stripped) | 100% accuracy |
| CPR lookup throttling prevents abuse per user per birth date | Enforced |
| CVR/SE verification returns valid company information | 100% accuracy |
| AKR debtor entry accepted without external verification | 100% |
| Claim data entry fields match creditor agreement fordringstype configuration | 100% |
| Interest rule options limited to agreement-allowed rules | 100% |
| Beskrivelse enforces 100-character limit with counter | 100% |
| Review step displays read-only summary of all entered data | 100% |
| UDFOERT submission displays receipt with fordrings-ID, status, summary | 100% |
| AFVIST submission displays validation errors with codes and descriptions | 100% |
| HOERING submission informs creditor and directs to hearing list | 100% |
| Navigation item hidden when user lacks role or agreement permissions | 100% |
| Audit log records all submissions (excluding PII) | 100% |
| SKAT standardlayout with correct breadcrumb | 100% |
| Step indicator shows current step and total steps | 100% |
| Accessible form patterns: labels, descriptions, aria-describedby | All form steps |
| CPR input uses numeric inputmode and pattern | 100% |
| Message bundle coverage for DA and EN | All user-facing text |

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| Wizard step templates (Thymeleaf) | `creditor-portal/src/main/resources/templates/claims/create/` |
| Step 1: debtor identification template | `creditor-portal/src/main/resources/templates/claims/create/step-debtor.html` |
| Step 2: claim data entry template | `creditor-portal/src/main/resources/templates/claims/create/step-details.html` |
| Step 3: review template | `creditor-portal/src/main/resources/templates/claims/create/step-review.html` |
| Step 4: receipt/result template | `creditor-portal/src/main/resources/templates/claims/create/step-result.html` |
| Claim creation controller | `creditor-portal/src/main/java/.../controller/ClaimCreateController.java` |
| BFF person-registry client | `creditor-portal/src/main/java/.../client/PersonRegistryClient.java` |
| BFF creditor-service client | `creditor-portal/src/main/java/.../client/CreditorServiceClient.java` |
| BFF debt-service client (claim submission) | `creditor-portal/src/main/java/.../client/DebtServiceClient.java` |
| Claim creation DTOs | `creditor-portal/src/main/java/.../dto/ClaimCreateDto.java` |
| Debtor verification DTOs | `creditor-portal/src/main/java/.../dto/DebtorVerificationDto.java` |
| Danish message bundle entries | `creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle entries | `creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition033-fordringshaverportal-opret-fordring.feature` |

## Failure conditions

- The wizard does not present all four steps (debtor identification, claim data entry, review, submission) in the correct order.
- A user without `CREDITOR_EDITOR` role can access the wizard.
- A user whose creditor agreement does not allow portal actions or claim creation can access the wizard.
- The "Opret fordring" navigation item is visible when the user lacks the required role or agreement permissions.
- CPR verification does not call `person-registry` through the BFF.
- CPR name matching is case-sensitive or does not strip accents.
- CPR lookups are not throttled per user per birth date.
- CVR/SE verification does not validate the entered number against `person-registry` or external CVR service.
- Claim data entry fields do not reflect the creditor agreement's fordringstype configuration.
- Interest rule options include rules not allowed by the creditor agreement.
- The beskrivelse field allows more than 100 characters or does not display a character counter.
- The review step is editable instead of read-only.
- The creditor is not required to confirm submission explicitly before data is sent.
- The BFF submits the claim directly to `debt-service` from the portal instead of routing through the BFF.
- UDFOERT submissions do not display a receipt with the fordrings-ID, processing status, and data summary.
- AFVIST submissions do not display validation errors with error codes and Danish descriptions.
- AFVIST submissions do not allow the creditor to correct and resubmit.
- HOERING submissions do not inform the creditor about the pending review or the hearing list.
- The creditor agreement is not loaded from `creditor-service` through the BFF.
- Claim submissions are not logged to the audit log, or PII is included in the audit log.
- The page does not use the SKAT standardlayout or the breadcrumb is incorrect.
- The step indicator is missing or does not show the current step and total steps.
- Form steps lack accessible patterns (visible labels, field descriptions, inline validation, aria-describedby).
- The CPR input does not use numeric inputmode or pattern.
- Any user-facing text is hardcoded in templates rather than using message bundles.
- English translation is missing for any user-facing text in the wizard.
- The BFF client uses `WebClient.create()` instead of injected `WebClient.Builder`.
