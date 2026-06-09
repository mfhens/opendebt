# User Testing Harness — OpenDebt

## Purpose

This file is the canonical harness specification for `user-testing-flow-validator`.
It defines personas, authentication, navigation conventions, evidence capture standards,
and the flow schema used to validate browser-visible surfaces against VAL-P* assertions.

---

## Services

| Service | Base URL | Notes |
|---|---|---|
| Caseworker Portal | `http://localhost:8087/caseworker-portal` | Thymeleaf + HTMX; primary UI surface |
| Creditor Portal   | `http://localhost:8085/creditor-portal` | Thymeleaf + HTMX |
| Citizen Portal    | `http://localhost:8086/borger` | Thymeleaf + HTMX |
| Debt Service API  | `http://localhost:8082/debt-service` | REST, no UI |
| Case Service API  | `http://localhost:8081/case-service` | REST, no UI |

All services run from `docker-compose.yml` at the repository root.
Start with: `docker compose up -d`

---

## Personas

### CASEWORKER (write)
```yaml
caseworkerId: anna-jensen
role: CASEWORKER
description: Sagsbehandler
```
- Has access to all caseworker-portal pages.
- Can register afbrydelse, objections, and objection decisions.
- Sees all write-action buttons on the limitation panel.
- Can inspect override, deviation, modregning, and decision-snapshot details on petition060 worklists.

### CASEWORKER_READONLY
```yaml
caseworkerId: erik-sorensen
role: SENIOR_CASEWORKER
description: Senior sagsbehandler
```
- Can view all caseworker-portal pages.
- Cannot register any write actions.
- Write-action buttons must be absent from all limitation panel views.
- Can inspect petition060 worklists, but does not gain extra write capability from that surface.

### ADMIN
```yaml
caseworkerId: system-admin
role: ADMIN
description: Systemadministrator
```

---

## Authentication

The production caseworker portal authenticates via Keycloak (OIDC), but the local validation
harness runs against demo mode.
Login URL for local/demo validation: `http://localhost:8087/caseworker-portal/demo-login`.

For Playwright flows, authenticate by navigating to the portal and selecting the demo
caseworker:

```typescript
await page.goto('http://localhost:8087/caseworker-portal/demo-login');
await page.selectOption('#caseworkerId', 'anna-jensen');
await page.click('button[type=submit]');
await page.waitForURL(/caseworker-portal\/cases/);
```

Store auth state per persona using `storageState` in `playwright.config.ts` to avoid
re-authenticating on every test.

---

## Navigation Conventions

### Claim detail page

```
http://localhost:8087/caseworker-portal/cases/{caseId}/debts/{debtId}
```

The limitation panel is rendered as a collapsible section with heading
"Forældelsesstatus" on the claim detail page.

### Petition060 worklist page

```
http://localhost:8087/caseworker-portal/debtors/{debtorId}/retskraft-worklists/{worklistId}
```

The petition060 surface is a direct inspection page. Because the current backend exposes
`GET /api/v1/debtors/{debtorId}/retskraft-worklists/{worklistId}` but no debtor-level list endpoint,
browser flows must generate or otherwise obtain a valid `{worklistId}` for a seeded `{debtorId}`
before navigating there after demo-login.

### Limitation panel anchor

The panel uses id `limitation-panel`. The write-action button has
text "Registrer forældelsesindsigelse". The evaluation form appears
when an objection is `INDSIGELSE_PENDING`.

---

## Evidence Capture Standards

Each flow must capture one or more of the following evidence types:

| Type | Meaning |
|---|---|
| `screenshots` | Full-page PNG taken at the step where the assertion is checked |
| `network` | Captured XHR/fetch response body from the relevant API call |
| `console_errors` | Browser console errors collected at assertion time (empty array = no errors) |

Evidence is written to `petitions/validation/<petition-ID>/user-testing/flows/`.

---

## Flow Schema

`petitions/validation/<petition-ID>/user-testing/flows/main.json` must conform to:

```json
{
  "petition": "P059",
  "generated": "<ISO timestamp>",
  "flows": [
    {
      "id": "flow-<slug>",
      "assertions": ["VAL-P059-NNN", ...],
      "persona": "CASEWORKER | CASEWORKER_READONLY | ADMIN",
      "steps": [
        {
          "action": "navigate | click | fill | assert_visible | assert_text | assert_absent | assert_network | capture",
          "target": "<selector or URL>",
          "value": "<optional value>",
          "evidence": ["screenshots", "network", "console_errors"]
        }
      ],
      "result": "pass | fail | blocked",
      "evidence": {
        "screenshots": ["<relative path>"],
        "network": [{"url": "...", "status": 200, "body": {}}],
        "console_errors": []
      }
    }
  ]
}
```

---

## Assertion-to-Flow Mapping

The following table maps each browser-relevant VAL assertion to its expected flow.
VAL assertions requiring only `network` evidence are validated via BDD/Cucumber
(green in CI) and do not require a browser flow. Those marked `browser` require
a Playwright flow and must appear in `main.json`.

| Assertion | Evidence type | Flow required |
|---|---|---|
| VAL-P059-001 through VAL-P059-039 | network | No — covered by BDD (Cucumber green) |
| VAL-P059-040 | screenshots, console_errors, network | **Yes** — `flow-panel-status` |
| VAL-P059-041 | screenshots, console_errors, network | **Yes** — `flow-panel-afbrydelse-history` |
| VAL-P059-042 | screenshots, console_errors, network | **Yes** — `flow-panel-tillaegsfrist-kompleks` |
| VAL-P059-043 | screenshots, console_errors, network | **Yes** — `flow-panel-write-button-visible` |
| VAL-P059-044 | screenshots, console_errors, network | **Yes** — `flow-panel-indsigelse-pending` |
| VAL-P059-045 | screenshots, console_errors, network | **Yes** — `flow-panel-foraeldet-outcome` |
| VAL-P059-046 | screenshots, console_errors, network | **Yes** — `flow-panel-readonly-no-write-actions` |
| VAL-P059-047 | network | No — covered by BDD |
| VAL-P059-048 | network | No — covered by BDD |
| VAL-P059-049 | network | No — covered by BDD |
| VAL-P060-002 | screenshots, console_errors, network | **Yes** — `flow-retskraft-worklist-inspection` |
| VAL-P060-007 | screenshots, console_errors, network | **Yes** — `flow-retskraft-worklist-inspection` |
| VAL-P060-009 | screenshots, console_errors, network | **Yes** — `flow-retskraft-worklist-inspection` |
| VAL-P060-010 | screenshots, console_errors, network | **Yes** — `flow-retskraft-worklist-inspection` |

---

## Flow Definitions

### flow-panel-status (VAL-P059-040)

Persona: CASEWORKER
Pre-condition: A claim exists with known limitation state (status ACTIVE,
`currentFristExpires`, `udskydelseDato`, `isInUdskydelse = true`).

Steps:
1. Navigate to `http://localhost:8087/caseworker-portal/cases/{caseId}/debts/{debtId}`
2. Assert limitation panel visible (`#limitation-panel`)
3. Assert text matching status label (e.g. "ACTIVE" or localised equivalent)
4. Assert text matching expiry date in ISO format (`yyyy-MM-dd`)
5. Assert text matching postponement date `2021-11-20`
6. Assert postponement-window indicator visible and truthy
7. Capture screenshots + network (GET `/api/v1/foraeldelse/{fordringId}`) + console_errors

Pass: all assertions satisfied, no console errors.

---

### flow-panel-afbrydelse-history (VAL-P059-041)

Persona: CASEWORKER
Pre-condition: A claim exists with two or more afbrydelse entries.

Steps:
1. Navigate to `http://localhost:8087/caseworker-portal/cases/{caseId}/debts/{debtId}`
2. Assert history table visible within `#limitation-panel`
3. Assert expected row count in history table
4. For each visible row: assert columns `type`, `date`, `legalReference`, `newExpiry` are non-empty
5. Assert rows are in ascending chronological order (by visible date column)
6. Capture screenshots + network + console_errors

Pass: all columns present, correct count, chronological order confirmed.

---

### flow-panel-tillaegsfrist-kompleks (VAL-P059-042)

Persona: CASEWORKER
Pre-condition: A claim exists that is a member of a claim complex and has a tillaegsfrist event
with a propagated entry (sourceFordringId, targetFordringId visible).

Steps:
1. Navigate to `http://localhost:8087/caseworker-portal/cases/{caseId}/debts/{debtId}`
2. Assert claim-complex section visible within `#limitation-panel`
3. Assert expected member IDs listed in the complex section
4. Assert tillaegsfrist history section visible with at least one row
5. Assert each row contains `type`, `appliedDate`, `extensionYears`, `newExpiry`
6. Assert propagated event row shows `sourceFordringId` and `targetFordringId`
7. Capture screenshots + network + console_errors

---

### flow-panel-write-button-visible (VAL-P059-043)

Persona: CASEWORKER
Pre-condition: A claim in ACTIVE status exists.

Steps:
1. Navigate to `http://localhost:8087/caseworker-portal/cases/{caseId}/debts/{debtId}`
2. Assert button with text "Registrer forældelsesindsigelse" is visible
3. Capture screenshots + console_errors

Pass: button present and visible.

---

### flow-panel-indsigelse-pending (VAL-P059-044)

Persona: CASEWORKER
Pre-condition: A claim exists in INDSIGELSE_PENDING status.

Steps:
1. Navigate to `http://localhost:8087/caseworker-portal/cases/{caseId}/debts/{debtId}`
2. Assert evaluation form visible (VALID + INVALID options and rationale field)
3. Assert button "Registrer forældelsesindsigelse" is NOT visible
4. Capture screenshots + console_errors

Pass: evaluation controls present, registration button absent.

---

### flow-panel-foraeldet-outcome (VAL-P059-045)

Persona: CASEWORKER
Pre-condition: A claim exists in FORAELDET status with a rationale.

Steps:
1. Navigate to `http://localhost:8087/caseworker-portal/cases/{caseId}/debts/{debtId}`
2. Assert prescribed outcome label visible (e.g. "FORAELDET")
3. Assert rationale text visible and non-empty
4. Assert button "Registrer forældelsesindsigelse" is NOT visible
5. Capture screenshots + console_errors

Pass: outcome and rationale visible, registration button absent.

---

### flow-panel-readonly-no-write-actions (VAL-P059-046)

Persona: CASEWORKER_READONLY
Pre-condition: Any claim in ACTIVE or INDSIGELSE_PENDING status.

Steps:
1. Authenticate as CASEWORKER_READONLY
2. Navigate to `http://localhost:8087/caseworker-portal/cases/{caseId}/debts/{debtId}`
3. Assert limitation panel visible (`#limitation-panel`)
4. Assert button "Registrer forældelsesindsigelse" is NOT present
5. Assert evaluation form (VALID/INVALID) is NOT present
6. Assert no afbrydelse registration controls visible
7. Capture screenshots + console_errors

Pass: panel visible, all write controls absent.

---

### flow-retskraft-worklist-inspection (VAL-P060-002 / 007 / 009 / 010)

Persona: CASEWORKER
Pre-condition: Petition060 candidate items are seeded for a known `{debtorId}`, a worklist is
generated through debt-service, and the resulting page can expose override, deviation,
modregning, and decision-snapshot details.

Steps:
1. Authenticate via `http://localhost:8087/caseworker-portal/demo-login`
2. Navigate to `http://localhost:8087/caseworker-portal/debtors/{debtorId}/retskraft-worklists/{worklistId}`
3. Assert `[data-testid="section50-worklist"]` is visible
4. Assert `[data-testid="section50-override"]` contains the override reason and legal basis
5. Assert `[data-testid="section50-modregning"]` contains the deviation reason and modregning outcome
6. Assert `[data-testid="section50-decision-snapshot"]` contains the rule path, legal reference, origin, timestamp, and selected next item
7. Assert only technical identifiers are shown (no CPR/name test data)
8. Capture screenshots + network + console_errors

## Pre-flight Checklist

Before running flows, verify:

- [ ] `docker compose up -d` completed with all services healthy
- [ ] `http://localhost:8087/caseworker-portal/actuator/health` returns `{"status":"UP"}`
- [ ] `http://localhost:8082/debt-service/actuator/health` returns `{"status":"UP"}` (debt-service)
- [ ] `http://localhost:8081/case-service/actuator/health` returns `{"status":"UP"}` (case-service)
- [ ] Test data seeded: claims in states ACTIVE, INDSIGELSE_PENDING, FORAELDET
- [ ] Petition060 seed (if used): a valid `{debtorId}` / `{worklistId}` pair is created through debt-service or direct database setup before the browser step
- [ ] Demo caseworkers exist on `/caseworker-portal/demo-login` (anna-jensen, erik-sorensen, system-admin)
- [ ] Playwright installed: `npm ci` in `opendebt-e2e/`

---

## Running E2E flows

```bash
cd opendebt-e2e
npm ci
npx playwright test tests/caseworker-portal/petition059-limitation-panel.spec.ts
```

The `petition059-limitation-panel.spec.ts` tests are tagged `@petition059` and
`@backlog`. Remove `@backlog` tag from individual tests to include them in CI.
