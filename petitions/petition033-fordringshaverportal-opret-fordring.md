# Petition 033: Fordringshaverportal -- Opret fordring (portal claim creation wizard)

## Summary

The Fordringshaverportal shall provide a multi-step wizard for creditors to create and submit new claims for debt recovery, including debtor validation, claim data entry governed by the creditor agreement, and receipt display upon submission.

## Context and motivation

Small creditors that cannot use the M2M path need a manual submission channel through the portal (petition 002, requirement 3). The legacy portal used NyMF `CreateAndUpdateClaimClient` for submission. In OpenDebt, the portal submits to `debt-service` through the BFF (petition 012). Debtor validation uses `person-registry` instead of CSRP/ES.

**Supersedes:** Fordringshaverportal petition 007 (old).
**References:** Petition 002 (fordring creation), Petition 003 (lifecycle), Petition 008 (creditor data model), Petition 009 (creditor master data), Petition 012 (BFF), Petition 015-018 (validation rules).

## Functional requirements

### Claim creation wizard

1. The portal shall provide a multi-step form for creating a new claim.
2. The wizard shall guide the creditor through: debtor identification, claim details entry, review, and submission.
3. The wizard shall only be accessible to users with `CREDITOR_EDITOR` role AND `allow_portal_actions` AND `allow_create_recovery_claims` permissions from the creditor agreement.

### Step 1: Debtor validation

4. The creditor shall select debtor type: CPR, CVR, SE, or AKR.
5. For CPR debtors:
   - The creditor enters a CPR number and a name.
   - The BFF calls `person-registry` to verify the CPR.
   - The first and last name from input shall be compared with person-registry response (case-insensitive, accent-stripped).
   - Verification succeeds only if both first and last names match.
   - CPR lookups shall be throttled per user per birth date to prevent abuse.
6. For CVR/SE debtors:
   - The creditor enters an SE number or CVR number.
   - The BFF calls `person-registry` (or external CVR service) to verify.
   - Company information is returned if valid.
7. For AKR debtors:
   - The creditor enters the AKR number (alternative contact register).

### Step 2: Claim data entry

8. The form shall present fields based on the creditor agreement's allowed claim types:
   - Fordringstype (from allowed types in agreement)
   - Beloeb (amount -- restgaeld at overdragelse)
   - Hovedstol (original principal)
   - Fordringshaver-reference (unique reference number)
   - Beskrivelse (free text, max 100 characters, no PII per GDPR)
   - Fordringsperiode (from-to dates) -- if required by fordringstype
   - Stiftelsesdato -- if required by fordringstype
   - Forfaldsdato -- if required by fordringstype
   - Sidste rettidige betalingsdato (SRB) -- if required by fordringstype
   - Foraeldelsesdato (expiry date) -- required
   - Bobehandling (estate processing flag) -- required for portal submissions
   - Domsdato, forligsdato -- optional
   - Rentevalg (interest rule, rate code, rate) -- optional
   - Fordringsnote -- optional
   - Kundenote (debtor note) -- optional
9. Field visibility and requirement levels shall be driven by the fordringstype configuration in the creditor agreement.
10. Interest rule options shall be limited to those allowed by the creditor agreement.

### Step 3: Review and submission

11. A review step shall display a summary of all entered data before submission.
12. The creditor shall confirm submission explicitly.
13. The BFF shall submit the claim to `debt-service`.
14. `debt-service` evaluates the claim against indrivelsesparathed rules (petitions 015-018).

### Submission outcomes

15. If the claim is accepted (UDFOERT), a receipt page shall display:
    - The assigned fordrings-ID
    - Processing status
    - Summary of submitted data
16. If the claim is rejected (AFVIST), the wizard shall display validation errors with error codes and Danish descriptions, allowing the creditor to correct and resubmit.
17. If the claim enters hearing (HOERING), the wizard shall inform the creditor that the claim is pending review and can be found in the hearing list (petition 031).

### Creditor agreement integration

18. The creditor agreement shall be loaded from `creditor-service` through the BFF.
19. The agreement determines:
    - Allowed claim types
    - Allowed debtor types
    - Interest rules and rates
    - Whether portal actions are allowed
20. The agreement shall be cacheable with a manual refresh option.

### Access control

21. The "Opret fordring" navigation item shall only appear when the user has `CREDITOR_EDITOR` role AND the creditor agreement allows portal actions and claim creation.
22. All claim submissions shall be logged to the audit log with the full payload (excluding PII).

## Layout and accessibility

23. The wizard shall use the SKAT standardlayout.
24. Breadcrumb: Forside > Opret fordring.
25. The wizard shall use a step indicator showing current step and total steps.
26. Each form step shall use accessible form patterns: visible labels, field descriptions, inline validation errors with `aria-describedby`.
27. The CPR input field shall have appropriate `inputmode` and pattern for numeric entry.
28. The beskrivelse field shall display a character counter (max 100).
29. The review step shall be a read-only summary, not an editable form.
30. All text shall use message bundles (petition 021).

## Data source mapping

| Old source | OpenDebt source |
|---|---|
| NyMF CreateAndUpdateClaimClient.createClaim() | `debt-service` POST /api/v1/debts |
| NyMF ReceiptFetcher | `debt-service` GET /api/v1/debts/{id}/receipts |
| PSRM ClaimantAgreement | `creditor-service` GET /api/v1/creditors/{id}/agreement |
| CSRP PersonStamoplysningerMultiHent | `person-registry` GET /api/v1/persons/verify-cpr |
| ES VirksomhedStamOplysningSamlingHent | `person-registry` or external CVR service |

## Constraints and assumptions

- The BFF handles creditor resolution and access control before forwarding to backend services.
- Debtor CPR validation requires person-registry to expose a verification endpoint (petition 023).
- The GDPR constraint on beskrivelse (no PII) is enforced at the form level with a warning and at the backend validation level.
- This petition defines the portal wizard, not the debt-service API contract or validation rule implementation.

## Out of scope

- M2M claim creation (petition 011)
- Detailed validation rule implementation (petitions 015-018)
- Related claim (underfordring) creation in the wizard
- Backend debt-service API design
