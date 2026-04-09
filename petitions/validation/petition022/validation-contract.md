# User validation contract — petition022

Maps Phase 6.5 (`user-testing-flow-validator`) assertion IDs to the outcome contract and Playwright coverage.

| ID | Assertion | Evidence |
|----|-----------|----------|
| VAL-P022-01 | Landing responds 200 with debt overview heading | `opendebt-e2e/tests/petition022-citizen-landing.spec.ts` |
| VAL-P022-02 | Seven FAQ items present | same |
| VAL-P022-03 | Self-service CTA uses HTTPS URL from configuration | same |
| VAL-P022-04 | Daily interest / snapshot notice visible | same |
| VAL-P022-05 | Debt errors section present | same |
| VAL-P022-06 | Danish and English headings | same |
| VAL-P022-07 | Accessibility statement `/was` returns 200 | same |
| VAL-P022-08 | Skip link and main landmark | same |
| VAL-P022-09 | Language selector present | same |
| VAL-P022-10 | Footer link to accessibility statement | same |
| VAL-P022-11 | External links are absolute HTTPS (configured) | same |

After `user-testing-flow-validator` runs, evidence is written to `petitions/validation/petition022/user-testing/flows/main.json` (conductor default).
