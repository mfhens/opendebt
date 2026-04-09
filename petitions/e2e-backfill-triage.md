# E2E Playwright backfill — triage matrix

This matrix supports the ADR 0034 backfill: which petitions warrant browser-level Playwright coverage versus service-layer tests only.

**Legend:** `now` = Wave 1–2 scope in this iteration; `later` = backlog; `skip` = not a browser E2E target (Cucumber/API/M2M suffice).

| Petition | Title (short) | UI / browser | Specs YAML | Wave |
|----------|---------------|--------------|------------|------|
| petition022 | Citizen landing page | yes | added (`petitions/specs/petition022-specs.yaml`) | **now** |
| petition012–014 | Creditor BFF, a11y | yes | later | later |
| petition023–026 | Citizen auth / debt UI | yes | later | later |
| petition029–038 | Creditor portal pages | yes | later | later |
| petition001 | OCR payment matching | no | skip | skip |
| petition008–010 | Creditor master data / binding | no | skip | skip |
| petition015–018 | Drools validation rules | no | skip | skip |

**Wave 1 (this delivery):** petition022 — GREEN Playwright tests in `opendebt-e2e/tests/petition022-citizen-landing.spec.ts`, CI excludes `@backlog` tests.

**Wave 2 (next):** Extend to petition029+ creditor portal lists, or petition012–014, reusing fixtures and `baseURL` patterns from Wave 1.
