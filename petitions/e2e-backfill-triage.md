# E2E Playwright backfill — triage matrix

This matrix supports the ADR 0034 backfill: which petitions warrant browser-level Playwright coverage versus service-layer tests only.

**Legend:** `now` = Wave 1–2 scope in this iteration; `later` = backlog; `skip` = not a browser E2E target (Cucumber/API/M2M suffice).

| Petition | Title (short) | UI / browser | Specs YAML | Wave |
|----------|---------------|--------------|------------|------|
| petition022 | Citizen landing page | yes | added (`petitions/specs/petition022-specs.yaml`) | **now** |
| petition012–014 | Creditor BFF, a11y | yes | later | later |
| petition023–026 | Citizen auth / debt UI | yes | later | later |
| petition029 | Creditor claims lists / counts | yes | added (`petitions/specs/petition029-specs.yaml`) | **Wave 2 (in progress)** |
| petition030–038 | Creditor portal (remaining) | yes | later | later |
| petition001 | OCR payment matching | no | skip | skip |
| petition008–010 | Creditor master data / binding | no | skip | skip |
| petition015–018 | Drools validation rules | no | skip | skip |

**Wave 1 (this delivery):** petition022 — GREEN Playwright tests in `opendebt-e2e/tests/petition022-citizen-landing.spec.ts`, CI excludes `@backlog` tests.

**Wave 2:** petition029 — Playwright + Keycloak + demo-login (`opendebt-e2e/tests/petition029-creditor-claims.spec.ts`, `tests/helpers/creditor-portal-auth.ts`). CI maps `keycloak` → `127.0.0.1` in `/etc/hosts`. Next: petition030–038 or petition012–014.
