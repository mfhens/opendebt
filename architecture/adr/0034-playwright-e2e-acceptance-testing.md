# ADR 0034: Playwright TypeScript as the E2E Acceptance Testing Layer for Portal Features

## Status
Accepted

## Context

### The existing test pyramid and its gap

OpenDebt's portals (creditor, caseworker, citizen) are Thymeleaf + HTMX BFFs. They do
not own data; their only logic is request routing, rendering, and session management.
The existing test layers cover:

| Layer | Technology | Scope | Infrastructure |
|-------|-----------|-------|----------------|
| Unit tests | JUnit 5 + Mockito | Individual classes, controller method return values, template assertions | None — in-process |
| BDD acceptance tests | Cucumber-JVM 7.18 | Spring-layer behaviour, service integration, business rules | H2 or embedded PostgreSQL — in-process Spring context |
| Integration tests | JUnit 5 + Testcontainers | Flyway migrations, immudb connectivity | Docker containers for specific dependencies |

None of these layers exercise the portals **through a browser**. A portal feature can
pass all Cucumber scenarios and still fail in production because:

- Thymeleaf fragments render malformed HTML that a browser's error recovery silently masks
- HTMX swap targets are missing, misnamed, or have the wrong `hx-*` attributes
- Keycloak redirect flows behave differently with a real browser than with a mocked security
  context
- WCAG violations (missing focus management, broken skip links) are invisible to JUnit
- JavaScript errors block form submission without surfacing in any server-side test

The `user-testing-flow-validator` agent in the delivery pipeline (Phase 6.5) requires a
validation contract and a live application URL. It can run Playwright assertions to confirm
user-facing acceptance criteria. However, the agent has no systematic source of test files
to maintain between pipeline runs, and the pipeline has no layer that generates or
accumulates those tests from Gherkin feature files. This is the gap.

### Technology options evaluated

Two credible options exist for adding browser-level testing:

**Option A — `playwright-java` Maven module**

Microsoft publishes `com.microsoft.playwright:playwright`, a Java binding for the Playwright
browser automation API. It runs under JUnit 5 and integrates into Maven via the
`maven-failsafe-plugin`.

| Aspect | Assessment |
|--------|-----------|
| Language consistency | Stays within the Java/Maven ecosystem |
| Playwright Java maturity | Generally available but thinner documentation and community than TypeScript. Codegen, trace viewer, and UI Mode are CLI tools independent of language. |
| Framework fit | `maven-failsafe-plugin` lifecycle (`verify`) requires a running application — typically started via `spring-boot:start`. Starting the full portal stack (three portals + Keycloak + PostgreSQL) via Maven plugins is complex and fragile compared to `docker compose up`. |
| Toolchain change | No Node.js required — but adding a new Maven module adds Maven parent POM changes, dependency management, and Spotless/JaCoCo configuration for a module that tests behaviour, not code quality. |
| What is tested | Browser-rendered HTML. The fact that the test is in Java does not mean it has access to the JVM — Playwright talks to the browser over CDP, not to the application. |

**Option B — Standalone `opendebt-e2e/` Node.js project (TypeScript)**

A standalone `opendebt-e2e/` directory at the repository root, not a Maven module,
containing a Node.js project with `@playwright/test` and TypeScript.

| Aspect | Assessment |
|--------|-----------|
| Language | TypeScript — Playwright's primary and most thoroughly documented language |
| Maturity | `@playwright/test` is Playwright's native test runner. Auto-waiting, trace recording, screenshot diffing, codegen, and the UI mode are all first-class in TypeScript. |
| Framework fit | `docker compose up` + `npx playwright test`. Mirrors how the application runs in production. No Maven lifecycle involvement — the right boundary, because browser tests are a system concern, not a build concern. |
| Toolchain change | Adds Node.js to the developer toolchain and CI. Node.js is already present in many teams via the Playwright CLI; it is available on all standard CI runners. |
| Language independence | The tests exercise rendered HTML and browser behaviour. The backend is irrelevant inside a Playwright test. Using TypeScript for browser tests against a Java backend is the established industry pattern — Spring Boot, Jenkins, the Kubernetes dashboard, and Elasticsearch all do this. |
| Agent ecosystem fit | The `playwright-test-generator` agent generates native TypeScript `test()` blocks from Gherkin feature files. It targets the `opendebt-e2e/` structure and reads `project.yaml` for configuration. |

### Existing precedent

| Project | Backend | E2E test language |
|---------|---------|------------------|
| Spring PetClinic | Java | Selenium (Java) — same language coherence argument; less tooling maturity |
| Kubernetes dashboard | Go | TypeScript (Cypress / Playwright) |
| Jenkins | Java | Selenium (Java) — historical; moving to Playwright TypeScript |
| elastic/kibana | Node.js | TypeScript Playwright |
| skat.dk | Next.js | Not public; TypeScript Playwright is the de-facto standard for React + Next.js apps |

The common pattern for complex polyglot systems is: backend language for server tests,
TypeScript for browser tests. The browser test layer is a seam that belongs to neither
the client nor the server.

## Decision

We adopt **Playwright TypeScript** in a standalone **`opendebt-e2e/` Node.js project**
as the E2E acceptance testing layer for all portal features.

### Positioning in the test pyramid

```
                    ┌─────────────────────────────────┐
                    │  E2E (Playwright TypeScript)     │  opendebt-e2e/
                    │  Browser → Portal → Services     │  docker compose stack
                    └─────────────────────────────────┘
              ┌───────────────────────────────────────────────┐
              │  BDD Acceptance (Cucumber-JVM)                │  in-process Spring
              │  Spring context → H2 / embedded PG            │  per Maven module
              └───────────────────────────────────────────────┘
    ┌───────────────────────────────────────────────────────────────────┐
    │  Unit (JUnit 5 + Mockito) + Integration (Testcontainers)          │
    └───────────────────────────────────────────────────────────────────┘
```

The layers are complementary, not alternatives:

| Layer | Answers | Speed | Scope |
|-------|---------|-------|-------|
| Playwright E2E | "Does the portal work for a user with a real browser?" | Slow (needs running stack) | Cross-cutting: auth, rendering, HTMX, accessibility |
| Cucumber acceptance | "Does the Spring application logic satisfy the Gherkin scenarios?" | Fast (in-process) | Business rules, service integration |
| Unit | "Does this class/method behave correctly?" | Very fast | Isolated components |

### Project structure

`opendebt-e2e/` is a sibling directory to the Maven modules. It is **not** listed in the
parent `pom.xml`. Maven ignores it entirely.

```
opendebt-e2e/
├── package.json               # @playwright/test, typescript
├── playwright.config.ts       # baseURL per portal, projects (chromium)
├── tsconfig.json
└── tests/
    ├── creditor-portal/
    │   └── *.spec.ts
    ├── caseworker-portal/
    │   └── *.spec.ts
    └── citizen-portal/
        └── *.spec.ts
```

Test files are generated by the `playwright-test-generator` agent from Gherkin feature
files. The agent reads `project.yaml` for the `e2e` section to find `project_root`,
`test_dir`, and `base_urls`.

### CI positioning

The `e2e-tests` CI job depends on `build` (JARs must exist). It:

1. Runs `docker compose up -d` to bring up the full stack
2. Waits for `/actuator/health` on all three portals
3. Installs Playwright dependencies (`npm ci && npx playwright install --with-deps chromium`)
4. Runs `npx playwright test`
5. Uploads the HTML report to GitHub Artifacts (30-day retention)
6. Runs `docker compose down -v` in `always()` to clean volumes

Chromium only in CI. Firefox and WebKit are opt-in for local developer runs.

### Test generation

Portal Gherkin scenarios that reach Phase 5 of the delivery pipeline are routed to
`playwright-test-generator` (not `bdd-test-generator`) when `playwright.config.ts` is
detected in `project.yaml`. The generator produces RED-phase `test()` blocks — failing
with `throw new Error('Not implemented: <petition-ID> — "<scenario title>"')` — until
a developer implements the Playwright actions.

This keeps the RED-GREEN discipline consistent with the TDD-enforcer's approach for
backend code.

### What is NOT covered by this layer

| Concern | Where it is covered |
|---------|-------------------|
| Business rule correctness | Cucumber-JVM (in-process) |
| Service integration and API contracts | Cucumber-JVM + integration tests |
| Formal legal rule encoding | Catala (ADR-0032) |
| Portal controller unit behaviour | JUnit 5 controller unit tests |
| Testcontainers scenarios (Flyway, immudb) | Integration tests (ADR as-is) |
| Performance / load testing | Load test module (`load-testing/`) |

## Consequences

### Improved

- Portal features now have a browser-level verification step that tests rendering,
  HTMX behaviour, Keycloak redirect flows, and cross-cutting concerns (focus management,
  error states) that no JVM-level test can reach.
- The `user-testing-flow-validator` agent (Phase 6.5) has a systematic source of
  Playwright tests generated from Gherkin scenarios — traceable from petition to browser
  assertion.
- Playwright's trace viewer and screenshot capture provide evidence artefacts for
  compliance and audit, aligned with the petition pipeline's `validation-contract.md`
  requirement.
- Codegen (`npx playwright codegen`) accelerates test authoring for complex flows.
- Chromium-only default in CI is fast (single browser, modern Chromium = supports all
  relevant Danish public-sector user agents).

### Accepted costs

- **Node.js in the toolchain.** Developers need Node.js locally to run E2E tests.
  It is not required for the Java build. `opendebt-e2e/` is clearly separated and
  ignored by Maven.
- **E2E tests require a running stack.** They cannot run in the pre-commit or unit-test
  phase. CI time increases because `docker compose up` and portal health checks add
  latency before any test runs.
- **Two test authoring languages.** Cucumber steps are Java; Playwright tests are
  TypeScript. Developers working on portal features must be comfortable with both.
  The TypeScript subset required is minimal: `test()`, `expect()`, `page` object.
- **Test brittleness risk.** Browser tests are inherently more brittle than unit tests
  (timing, selector stability, environmental variance). Playwright's auto-waiting and
  strict selectors mitigate this, but E2E suite maintenance is an ongoing commitment.

### Unchanged

- The Maven build and all existing JUnit/Cucumber tests are unaffected.
- `pom.xml` (parent and modules) has no knowledge of `opendebt-e2e/`.
- Cucumber-JVM remains the primary BDD layer for business rule acceptance.
- ADR-0023 (Thymeleaf + HTMX) is unaffected; Playwright tests against whatever
  the portal renders, regardless of the server-side templating technology.
- ADR-0021 (WCAG 2.1 AA) is not replaced by this ADR; axe-core assertions can be
  added to Playwright tests as a complementary accessibility check.

## Alternatives considered

| Option | Reason not chosen |
|--------|------------------|
| **`playwright-java` Maven module** | Maven lifecycle (surefire/failsafe) is the wrong place for browser tests against a live stack. Starting three portals + Keycloak + PostgreSQL via `spring-boot:start` Maven plugin is fragile. The Java API has thinner tooling than TypeScript. Adds a Maven module that would need Spotless, JaCoCo, and parent POM wiring — overhead not justified for a test-only module. |
| **Selenium WebDriver (Java)** | Older API, inferior auto-waiting model, no built-in trace viewer. Playwright is the successor and the current industry standard for browser automation. |
| **Cypress (TypeScript)** | JavaScript-only API (no first-class support for multiple browser projects), iFrame and multi-tab limitations, commercial product (Cypress Cloud). Playwright is open-source, supports all browsers natively, and has a more complete API for portal scenarios involving auth redirects (Keycloak OIDC flows are multi-tab/multi-domain). |
| **Thymeleaf template unit tests only** | Tests that assert `html.contains("layout:decorate=...")` verify file content, not rendered behaviour in a browser. They cannot catch HTMX wiring errors, auth redirect failures, or accessibility issues. They remain useful as unit tests (already in use via `portal-tdd-enforcer`) but do not substitute for browser-level testing. |

## References

- ADR-0023: Creditor Portal Frontend Technology (Thymeleaf + HTMX)
- ADR-0021: UI Accessibility and Webtilgængelighed Compliance (WCAG 2.1 AA)
- ADR-0032: Catala as the Formal Compliance Verification Layer
- `~/.claude/agents/playwright-test-generator.agent.md` — generates TypeScript `test()` blocks from Gherkin feature files
- `.factory/project.yaml` — `e2e` section defines `project_root`, `test_dir`, `base_urls`
- `.github/workflows/ci.yml` — `e2e-tests` job definition
- Microsoft Playwright documentation: https://playwright.dev/docs/intro
