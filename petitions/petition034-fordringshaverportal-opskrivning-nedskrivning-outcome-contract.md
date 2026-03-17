# Petition 034 Outcome Contract

## Acceptance criteria

1. From the claim detail page (petition 030), an editor can initiate a write-up or write-down on an existing claim.
2. The update form is accessible via action buttons on the claim detail page.
3. Updates are submitted to `debt-service` through the BFF.
4. The portal supports eight update types governed by creditor agreement permission flags:
   - **Write-downs:** NEDSKRIV, NEDSKRIVNING_INDBETALING, NEDSKRIVNING_ANNULLERET_OPSKRIVNING_REGULERING, NEDSKRIVNING_ANNULLERET_OPSKRIVNING_INDBETALING.
   - **Write-ups:** OPSKRIVNING_REGULERING, OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING, OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING, FEJLAGTIG_HOVEDSTOL_INDBERETNING.
5. Available update types are filtered based on the creditor agreement's permission flags (e.g., `allow_write_down`, `allow_write_up_adjustment`).
6. Each update type requires: beloeb (amount), virkningsdato (effective date), and aarsag/aarsagskode (reason).
7. Payment-related updates (NEDSKRIVNING_INDBETALING and cancellation variants) additionally require debtor selection when the claim has multiple debtors.
8. For payment-related update types, the creditor selects the debtor from the claim's debtor list.
9. The portal displays censored debtor identifiers in the debtor selection.
10. The BFF resolves the actual debtor ID from the claim's haeftelsesstruktur before forwarding to `debt-service`.
11. After a successful update, a receipt displays the operation result: aktions-ID, status, and beloeb.
12. Debtor identifiers in the response are censored before display.
13. Only users with `CREDITOR_EDITOR` role and `allow_portal_actions` may submit updates.
14. Each specific update type requires the corresponding permission flag from the creditor agreement.
15. All update operations are logged to the audit log.
16. Update forms use the SKAT standardlayout.
17. The update form is presented either inline on the detail page or as a separate page with breadcrumb: Forside > Fordringer > [Fordrings-ID] > Opskriv/Nedskriv.
18. Forms use accessible patterns: labels, validation feedback, confirmation before submission.
19. Debtor selection uses a radio button group with censored identifiers.
20. All user-facing text uses message bundles (petition 021) with Danish and English translations.

## Definition of done

- The creditor portal provides action buttons on the claim detail page (petition 030) to initiate write-ups and write-downs.
- The update form supports all eight update types, filtered by the creditor agreement's permission flags.
- Each update form captures beloeb, virkningsdato, and aarsag/aarsagskode.
- Payment-related updates require debtor selection from the claim's debtor list when multiple debtors exist.
- Debtor identifiers are censored in the selection (CPR: first 6 digits + `****`).
- The BFF resolves actual debtor IDs from the claim's haeftelsesstruktur before forwarding to `debt-service`.
- Updates are submitted to `debt-service` through the BFF; the portal does not call `debt-service` directly.
- A receipt page displays the operation result (aktions-ID, status, beloeb) after a successful update; debtor identifiers are censored.
- Only users with `CREDITOR_EDITOR` role and `allow_portal_actions` may access update forms.
- Each update type requires its corresponding permission flag from the creditor agreement.
- All update operations are logged to the audit log.
- The SKAT standardlayout is applied with breadcrumb: Forside > Fordringer > [Fordrings-ID] > Opskriv/Nedskriv.
- Forms use accessible patterns: visible labels, validation feedback, and confirmation before submission.
- Debtor selection uses a radio button group with censored identifiers.
- All user-facing text is externalized to message bundles with Danish and English translations.
- The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Success metrics

| Metric | Target |
|--------|--------|
| Action buttons visible on claim detail for authorized editors | 100% |
| All 8 update types supported | 100% |
| Update type filtering by creditor agreement permission flags | 100% |
| Update form captures beloeb, virkningsdato, aarsag/aarsagskode | 100% |
| Payment-related updates require debtor selection for multi-debtor claims | 100% |
| Debtor identifiers censored in selection and response | 100% |
| BFF resolves debtor ID from haeftelsesstruktur before forwarding | 100% |
| Receipt displays aktions-ID, status, beloeb | 100% |
| CREDITOR_EDITOR with allow_portal_actions can submit updates | 100% |
| Users without required role or permission flags denied access | 100% |
| Audit log records all update operations | 100% |
| SKAT standardlayout with correct breadcrumb | 100% |
| Accessible form patterns: labels, validation, confirmation | All forms |
| Radio button group for debtor selection | Payment-related updates |
| Message bundle coverage for DA and EN | All user-facing text |

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| Write-up form template (Thymeleaf) | `creditor-portal/src/main/resources/templates/claims/adjustment/write-up.html` |
| Write-down form template (Thymeleaf) | `creditor-portal/src/main/resources/templates/claims/adjustment/write-down.html` |
| Update receipt template (Thymeleaf) | `creditor-portal/src/main/resources/templates/claims/adjustment/receipt.html` |
| Claim adjustment controller | `creditor-portal/src/main/java/.../controller/ClaimAdjustmentController.java` |
| BFF debt-service client (adjustment methods) | `creditor-portal/src/main/java/.../client/DebtServiceClient.java` |
| Claim adjustment DTOs | `creditor-portal/src/main/java/.../dto/ClaimAdjustmentDto.java` |
| Adjustment receipt DTO | `creditor-portal/src/main/java/.../dto/AdjustmentReceiptDto.java` |
| Danish message bundle entries | `creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle entries | `creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition034-fordringshaverportal-opskrivning-nedskrivning.feature` |

## Failure conditions

- The claim detail page does not display action buttons for initiating write-ups or write-downs.
- The update form is accessible to users without `CREDITOR_EDITOR` role or without `allow_portal_actions`.
- An update type is available to a user whose creditor agreement does not include the corresponding permission flag.
- Any of the eight update types is missing from the portal.
- The update form does not capture beloeb, virkningsdato, or aarsag/aarsagskode.
- Payment-related updates do not require debtor selection when the claim has multiple debtors.
- Debtor identifiers are displayed uncensored in the selection or response.
- The BFF does not resolve the actual debtor ID from the haeftelsesstruktur before forwarding to `debt-service`.
- Updates are submitted directly to `debt-service` from the portal instead of going through the BFF.
- The receipt does not display aktions-ID, status, or beloeb after a successful update.
- Update operations are not logged to the audit log.
- The page does not use the SKAT standardlayout or the breadcrumb is incorrect.
- The form lacks accessible patterns (labels, validation feedback, confirmation before submission).
- Debtor selection does not use a radio button group or does not display censored identifiers.
- Any user-facing text is hardcoded in templates rather than using message bundles.
- English translation is missing for any user-facing text on the update forms.
- The BFF client uses `WebClient.create()` instead of injected `WebClient.Builder`.
