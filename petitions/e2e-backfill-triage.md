# E2E Playwright backfill — triage matrix

This matrix supports the ADR 0034 backfill: which petitions warrant browser-level Playwright coverage versus service-layer tests only.

**Legend:** `skip` = not a browser E2E target (Cucumber/API/M2M suffice).

| Petition | Title (short) | UI / browser | Specs YAML (`petitions/specs/`) | Playwright wave |
|----------|---------------|--------------|-----------------------------------|-----------------|
| petition022 | Citizen landing | yes | `petition022-specs.yaml` | Wave 1 (done) |
| petition012 | Creditor BFF / manual submission | yes | `petition012-specs.yaml` | Wave 4+ (specs ready) |
| petition013 | UI accessibility compliance | yes | `petition013-specs.yaml` | Wave 4+ (specs ready) |
| petition014 | Accessibility statements | yes | `petition014-specs.yaml` | Wave 4+ (specs ready) |
| petition023–026 | Citizen auth / debt UI | yes | not yet | later |
| petition029 | Creditor claims lists / counts | yes | `petition029-specs.yaml` | Wave 2 (done) |
| petition030–038 | Creditor portal (detail, hearing, …) | yes | `petition030`–`petition038-specs.yaml` | Wave 4 (done) |
| petition001 | OCR payment matching | no | skip | skip |
| petition008–010 | Creditor master data / binding | no | skip | skip |
| petition015–018 | Drools validation rules | no | skip | skip |

**Wave 1 (done):** petition022 — GREEN Playwright in `opendebt-e2e/tests/petition022-citizen-landing.spec.ts`; CI `@backlog` grep invert.

**Wave 2 (done):** petition029 — `petition029-creditor-claims.spec.ts` + `creditor-portal-auth.ts`; CI `/etc/hosts` for `keycloak`.

**Wave 3 (done):** Module specs YAML for **petition012–014** and **petition030–038** (twelve files). Petition038 has no Gherkin feature in-repo; YAML references the petition + outcome contract markdown.

**Wave 4 (done):** Creditor portal shells **petition030–038** — `opendebt-e2e/tests/petition030-038-creditor-portal-surfaces.spec.ts` (OAuth gate, HTMX tables, wizard step 1 when enabled, adjustment shell, notifications, reconciliation, reports, dashboard counts, settings).

**Wave 5 (next):** Citizen **023–026** Playwright and/or `petition023-specs.yaml` etc. when those flows are in scope; deeper creditor scenarios (sorting, search, downloads) optional.
