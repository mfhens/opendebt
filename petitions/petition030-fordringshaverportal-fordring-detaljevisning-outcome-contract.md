# Petition 030 Outcome Contract

## Acceptance criteria

1. The creditor portal serves a claim detail view displaying claim information including fordringstype, fordringskategori, fordringshaver-beskrivelse, modtagelsesdato, periode (from-to dates), stiftelsesdato, fordrings-ID, obligations-ID, and fordringshaver-reference.
2. When the claim has a single debtor, the detail view additionally displays forfaldsdato, foraeldelsesdato, sidste rettidige betalingsdag, and retsdato (if applicable).
3. The detail view displays the fordringskategori (HOVEDFORDRING or sub-category) and related obligations-ID for sub-claims.
4. The detail view displays a financial breakdown table with rows per debt category showing: original amount, write-off amount, payment amount, and balance.
5. The financial breakdown table includes rows for inddrivelsesrenter (recovery interest), inddrivelsesomkostninger (collection charges from related claims), opkraevningsrenter sendt til inddrivelse (collection interest sent for recovery), and a total current balance row.
6. The detail view displays: renteregel (interest rule), rentesats (interest rate), ekstra rentesats (if applicable), total gaeld (total debt), seneste rentetilskrivningsdato (latest interest accrual date), oprindelig hovedstol (original principal), modtaget beloeb (received amount), fordringssaldo (claim balance), samlet fordringshaver-saldo (total claimant balance), and beloeb indsendt til inddrivelse (with and without write-ups).
7. The detail view displays a list of all write-ups on the claim, each showing: aktions-ID, reference-aktions-ID, formtype, aarsag (reason), beloeb (amount), virkningsdato (effective date), and skyldner-ID.
8. Annulled write-ups are visually flagged in the write-ups list.
9. Write-ups are sorted by aktions-ID.
10. The detail view displays a list of all write-downs on the claim, each showing: aktions-ID, reference-aktions-ID, formtype, aarsagskode (reason code), beloeb, virkningsdato, and skyldner-ID.
11. If the claim has related claims (underfordringer), the detail view lists them with summary information.
12. Each related claim is clickable and navigates to its own detail view.
13. The detail view lists all debtors associated with the claim via the haeftelsesstruktur.
14. Each debtor shows their identifier (CPR censored, CVR, SE, or AKR); CPR numbers are always censored before display.
15. When there is a single debtor, court decisions (dom) and settlements (forlig) are displayed with their dates.
16. The portal provides a mechanism to fetch receipts (kvitteringer) for claim operations, retrieved from `debt-service` using the delivery ID.
17. If a zero-balance claim is past 60 days, a message informs the user that detailed data is no longer available.
18. Service errors from `debt-service` are displayed as user-friendly Danish error messages.
19. The detail view is accessible only to authenticated users with `CREDITOR_VIEWER` or `CREDITOR_EDITOR` roles.
20. The detail page uses the SKAT standardlayout with breadcrumb showing: Forside > Fordringer > [Fordrings-ID].
21. Long sections (write-ups, write-downs, related claims) use collapsible `<details>`/`<summary>` elements.
22. Financial tables use semantic HTML with proper `scope` attributes (`<table>`, `<thead>`, `<tbody>`, `<th scope>`).
23. All monetary amounts use Danish formatting (comma decimal separator, 2 decimal places).
24. All dates are formatted as `dd.MM.yyyy`.
25. All user-facing text uses message bundles (petition 021) with Danish and English translations.
26. Claim data is loaded from `debt-service` through the creditor-portal BFF; the portal does not query `debt-service` directly.
27. The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.

## Definition of done

- The creditor portal renders a claim detail view with all sections: claim information, financial breakdown, write-ups, write-downs, related claims, debtor information, and decisions.
- Claim information displays fordringstype, fordringskategori, fordringshaver-beskrivelse, modtagelsesdato, periode, stiftelsesdato, fordrings-ID, obligations-ID, and fordringshaver-reference.
- Single-debtor-only fields (forfaldsdato, foraeldelsesdato, sidste rettidige betalingsdag, retsdato) are conditionally displayed.
- The financial breakdown table shows rows per debt category (original amount, write-off, payment, balance), recovery interest, collection charges, collection interest sent for recovery, and a total current balance row.
- Additional financial fields (interest rule, interest rate, total debt, original principal, claim balance, etc.) are all displayed.
- Write-ups list displays all required columns (aktions-ID, reference-aktions-ID, formtype, reason, amount, effective date, debtor-ID), sorted by aktions-ID, with annulled write-ups visually flagged.
- Write-downs list displays all required columns (aktions-ID, reference-aktions-ID, formtype, reason code, amount, effective date, debtor-ID).
- Related claims (underfordringer) are listed with summary info and each is clickable to navigate to its own detail view.
- All debtors are listed from the haeftelsesstruktur with identifiers; CPR numbers are censored (first 6 digits + `****`).
- Court decisions and settlements are displayed with dates when there is a single debtor.
- Receipt retrieval is available for claim operations using the delivery ID.
- Zero-balance claims older than 60 days display an appropriate message instead of full details.
- Service errors from `debt-service` render as user-friendly Danish error messages.
- Role-based access control restricts visibility to `CREDITOR_VIEWER` and `CREDITOR_EDITOR` roles.
- The SKAT standardlayout is applied with breadcrumb: Forside > Fordringer > [Fordrings-ID].
- Write-ups, write-downs, and related claims sections use collapsible `<details>`/`<summary>` elements.
- Financial tables use semantic HTML with proper scope attributes.
- Monetary amounts use Danish locale formatting (comma decimal separator, 2 decimal places).
- Dates are formatted as `dd.MM.yyyy`.
- All user-facing text is externalized to message bundles with Danish and English translations.
- Claim data is loaded through the BFF, not directly from `debt-service`.
- The BFF client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Success metrics

| Metric | Target |
|--------|--------|
| Claim information section displays all required fields | 100% |
| Single-debtor fields conditionally displayed | Correct for single and multi-debtor claims |
| Financial breakdown table shows all rows and columns | 100% |
| Additional financial fields all displayed | 100% |
| Write-ups list shows all 7 columns, sorted by aktions-ID | 100% |
| Annulled write-ups visually flagged | 100% |
| Write-downs list shows all 7 columns | 100% |
| Related claims listed and clickable | 100% |
| All debtors displayed from haeftelsesstruktur | 100% |
| CPR censoring: no full CPR displayed in any view | 100% |
| Decisions section shown only for single-debtor claims | 100% |
| Receipt retrieval works via delivery ID | 100% |
| Zero-balance 60-day message displayed correctly | 100% |
| Service errors shown as user-friendly Danish messages | 100% |
| CREDITOR_VIEWER and CREDITOR_EDITOR can access detail view | 100% |
| Unauthenticated or unauthorized users denied access | 100% |
| SKAT standardlayout with correct breadcrumb | 100% |
| Collapsible sections for write-ups, write-downs, related claims | 100% |
| Semantic HTML in financial tables | All tables |
| Monetary amounts in Danish locale format | 100% |
| Dates formatted as dd.MM.yyyy | 100% |
| Message bundle coverage for DA and EN | All user-facing text |

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| Claim detail page (Thymeleaf template) | `creditor-portal/src/main/resources/templates/claims/detail.html` |
| Claim detail section fragments | `creditor-portal/src/main/resources/templates/claims/fragments/` |
| Claim detail controller | `creditor-portal/src/main/java/.../controller/ClaimDetailController.java` |
| BFF claim detail client methods | `creditor-portal/src/main/java/.../client/DebtServiceClient.java` |
| Claim detail DTOs | `creditor-portal/src/main/java/.../dto/ClaimDetailDto.java` |
| Write-up DTO | `creditor-portal/src/main/java/.../dto/WriteUpDto.java` |
| Write-down DTO | `creditor-portal/src/main/java/.../dto/WriteDownDto.java` |
| Financial breakdown DTO | `creditor-portal/src/main/java/.../dto/FinancialBreakdownDto.java` |
| Danish message bundle entries | `creditor-portal/src/main/resources/messages_da.properties` |
| English message bundle entries | `creditor-portal/src/main/resources/messages_en_GB.properties` |
| Gherkin feature file | `petitions/petition030-fordringshaverportal-fordring-detaljevisning.feature` |

## Failure conditions

- Any required claim information field (fordringstype, fordringskategori, modtagelsesdato, stiftelsesdato, fordrings-ID, obligations-ID, fordringshaver-reference) is missing from the detail view.
- Single-debtor-only fields (forfaldsdato, foraeldelsesdato, sidste rettidige betalingsdag) are displayed for multi-debtor claims or missing for single-debtor claims.
- The financial breakdown table is missing any required row (debt category, recovery interest, collection charges, collection interest, total balance) or column (original amount, write-off, payment, balance).
- Any additional financial field (interest rule, interest rate, total debt, original principal, claim balance, etc.) is missing.
- Write-ups list is missing any of the 7 required columns or is not sorted by aktions-ID.
- Annulled write-ups are not visually flagged.
- Write-downs list is missing any of the 7 required columns.
- Related claims are not listed when present, or are not clickable to navigate to their detail view.
- Debtors are not listed from the haeftelsesstruktur.
- CPR numbers are displayed in full (not censored) in any view.
- Decisions section is shown for multi-debtor claims or missing for single-debtor claims with decisions.
- Receipt retrieval does not work or does not use the delivery ID.
- Zero-balance claim past 60 days does not show the expected message.
- Service errors are displayed as raw technical messages instead of user-friendly Danish text.
- A user without `CREDITOR_VIEWER` or `CREDITOR_EDITOR` role can access the detail view.
- An unauthenticated user can access the detail view.
- The page does not use the SKAT standardlayout or the breadcrumb is incorrect.
- Write-ups, write-downs, or related claims sections are not collapsible.
- Financial tables lack semantic HTML (`<table>`, `<thead>`, `<th scope>`) or proper scope attributes.
- Monetary amounts are not formatted with comma decimal separator and 2 decimal places.
- Dates are not formatted as `dd.MM.yyyy`.
- Any user-facing text is hardcoded in the template rather than using message bundles.
- English translation is missing for any user-facing text on the claim detail page.
- The BFF client uses `WebClient.create()` instead of injected `WebClient.Builder`.
- Claim data is fetched directly from `debt-service` by the portal instead of going through the BFF.
