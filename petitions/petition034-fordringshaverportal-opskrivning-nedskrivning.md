# Petition 034: Fordringshaverportal -- Opskrivning og nedskrivning

## Summary

The Fordringshaverportal shall allow creditors to submit write-ups (opskrivninger) and write-downs (nedskrivninger) on existing claims, including payment-related adjustments and cancellation scenarios.

## Context and motivation

After a claim is accepted for recovery, the creditor may need to adjust the claim amount. Write-ups increase the amount (e.g., additional charges), write-downs decrease it (e.g., partial payments received). The legacy portal submitted these to NyMF. In OpenDebt, adjustments are submitted to `debt-service` through the BFF.

**Supersedes:** Fordringshaverportal petition 008 (old).
**References:** Petition 003 (fordring lifecycle), Petition 008 (creditor data model -- permission flags), Petition 012 (BFF), Petition 030 (claim detail).

## Functional requirements

### Update claim submission

1. From the claim detail page (petition 030), an editor shall be able to initiate a write-up or write-down.
2. The update form shall be accessible via action buttons on the claim detail page.
3. Updates shall be submitted to `debt-service` through the BFF.

### Supported update types

4. The portal shall support the following update types (governed by creditor agreement permission flags from petition 008):

   **Write-downs (nedskrivninger):**
   - NEDSKRIV -- standard write-down
   - NEDSKRIVNING_INDBETALING -- write-down due to payment received by creditor
   - NEDSKRIVNING_ANNULLERET_OPSKRIVNING_REGULERING -- cancel a write-up by write-down (adjustment)
   - NEDSKRIVNING_ANNULLERET_OPSKRIVNING_INDBETALING -- cancel a write-up by write-down (payment)

   **Write-ups (opskrivninger):**
   - OPSKRIVNING_REGULERING -- standard write-up (adjustment)
   - OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING -- reverse a write-down by write-up (adjustment)
   - OPSKRIVNING_ANNULLERET_NEDSKRIVNING_INDBETALING -- cancel a write-down by write-up (payment)
   - FEJLAGTIG_HOVEDSTOL_INDBERETNING -- incorrect principal correction

5. Available update types shall be filtered based on the creditor agreement's permission flags (e.g., `allow_write_down`, `allow_write_up_adjustment`, etc.).

### Update form fields

6. Each update type shall require:
   - Beloeb (amount)
   - Virkningsdato (effective date)
   - Aarsag/aarsagskode (reason)
7. Payment-related updates (NEDSKRIVNING_INDBETALING and cancellation variants) shall additionally require debtor selection when the claim has multiple debtors.

### Debtor identification for payment write-downs

8. For payment-related update types, the creditor selects the debtor from the claim's debtor list.
9. The portal shall display censored debtor identifiers in the selection.
10. The BFF shall resolve the actual debtor ID from the claim's haeftelsesstruktur before forwarding to `debt-service`.

### Receipt display

11. After a successful update, a receipt shall display the operation result (aktions-ID, status, beloeb).
12. Debtor identifiers in the response shall be censored before display.

### Access control

13. Only users with `CREDITOR_EDITOR` role and `allow_portal_actions` may submit updates.
14. Each specific update type requires the corresponding permission flag from the creditor agreement.
15. All update operations shall be logged to the audit log.

## Layout and accessibility

16. Update forms shall use the SKAT standardlayout.
17. The update form shall be presented either inline on the detail page or as a separate page with breadcrumb: Forside > Fordringer > [Fordrings-ID] > Opskriv/Nedskriv.
18. Forms shall use accessible patterns: labels, validation feedback, confirmation before submission.
19. Debtor selection shall use a radio button group with censored identifiers.
20. All text shall use message bundles (petition 021).

## Data source mapping

| Old source | OpenDebt source |
|---|---|
| NyMF CreateAndUpdateClaimClient.updateClaim() | `debt-service` PUT /api/v1/debts/{id}/adjustments |
| NyMF ReceiptFetcher | `debt-service` GET /api/v1/debts/{id}/receipts/{deliveryId} |
| PSRM DKClaimDetails (for debtor resolution) | `debt-service` GET /api/v1/debts/{id}/details |

## Constraints and assumptions

- The BFF enforces that the user's creditor agreement permits the requested update type.
- Debtor resolution for payment write-downs requires the BFF to fetch claim details from debt-service and match the selected debtor.
- This petition defines the portal flow, not the backend API for adjustments.

## Out of scope

- Tilbagekald (withdrawal) flow
- Genindsendelse (resubmission) flow
- Backend adjustment processing logic
