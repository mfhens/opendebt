# User validation contract — petition029

| ID | Assertion | Evidence |
|----|-----------|----------|
| VAL-P029-01 | Unauthenticated GET /fordringer returns OAuth redirect (no Keycloak follow from API) | `opendebt-e2e/tests/petition029-creditor-claims.spec.ts` |
| VAL-P029-02 | After Keycloak + demo creditor, recovery list shows 13 column headers and HTMX table | same |
| VAL-P029-03 | Zero-balance list shell and 13 columns | same |
| VAL-P029-04 | Claims counts page heading and form | same |
| VAL-P029-05 | Skip link and main landmark on claims list | same |

Optional: run `user-testing-flow-validator` and store output under `petitions/validation/petition029/user-testing/flows/main.json`.
