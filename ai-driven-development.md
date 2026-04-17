---

## marp: true
theme: ey
paginate: true

---

# AI-Driven Software Development

### From Code Generation to Governed Delivery

**OpenDebt showcase · April 2026**



---

## The Real Bottleneck

- AI already reduces time-to-first-code
- Complex delivery still fails on ambiguity, drift, and rework
- National debt collection needs control, not just speed
- The question is how to accelerate without losing governance



---

## What Enterprise Delivery Must Guarantee


| Capability            | Why it matters in OpenDebt                           |
| --------------------- | ---------------------------------------------------- |
| Traceability          | Public decisions must link back to business intent   |
| Security and privacy  | PII, OAuth2, and role-based access must hold         |
| Auditability          | Financial and legal actions must be reviewable       |
| Architecture control  | A 12-service platform cannot drift silently          |
| Current documentation | Operators, developers, and auditors need one picture |




---

## The SDLC In One View


| Step                       | Main output               | Why it exists                |
| -------------------------- | ------------------------- | ---------------------------- |
| Petition                   | Business intent           | Bound the scope              |
| Outcome contract + Gherkin | Observable behaviour      | Remove ambiguity early       |
| Architecture + policies    | Governed design           | Keep change platform-aligned |
| Specs + tests              | Executable expectations   | Drive implementation         |
| Implementation + review    | Code with evidence        | Enforce quality              |
| Doc sync + status          | Current operating picture | Reduce drift afterwards      |


**Human role:** gate intent, accountability, and merge readiness.



---

## Guardrails Exist Before Coding Starts

- `project-bootstrap` creates the delivery substrate
- `.factory/project.yaml` captures stack, commands, and test routing
- `architecture/workspace.dsl` and `architecture/policies.yaml` define the model and rules
- `compliance/nfr-register.yaml` makes cross-cutting NFRs explicit
- `AGENTS.md` and `petitions/program-status.yaml` keep conventions and status visible



---

## Example 1: Petition 050, Unified Case Timeline


| Requirement                                      | Why it matters                 |
| ------------------------------------------------ | ------------------------------ |
| Replace fragmented event views with one timeline | Better situational awareness   |
| Reuse across three portals                       | One component, three audiences |
| Role-based visibility                            | GDPR data minimisation         |
| HTMX filtering and pagination                    | Usable at real scale           |


**Simple request. Still touches UX, privacy, reuse, and architecture.**



---

## What Traceability Actually Means

- A petition anchors the business intent
- Outcome contracts and scenarios define observable behaviour
- Specs and architecture explain how the change will be delivered
- Tests, reviews, docs, and status link back to the same petition
- This is artifact-level traceability, not a claim that every conversation is captured



---

## NFRs Are Explicit And Reusable


| Concern       | Example in OpenDebt                | Checked by       |
| ------------- | ---------------------------------- | ---------------- |
| Security      | Auth on non-public endpoints       | Review and tests |
| Audit         | Structured audit for state changes | Review and tests |
| Observability | Trace propagation and JSON logs    | Review           |
| Architecture  | No cross-service DB access         | C4 governance    |


**Applicable NFRs are injected into architecture and specs before implementation.**



---

## Architecture Is A Checked Artifact

- `architecture/workspace.dsl` is a living model, not slideware
- `c4-model-validator` checks syntax and structural completeness
- `c4-architecture-governor` checks code and IaC against the declared model
- Drift is surfaced before it becomes expensive
- This matters more in a platform than in a single application



---

## Example 2: Petition 058, Statutory And Financial Complexity


| Domain concern        | Why it is hard                          |
| --------------------- | --------------------------------------- |
| Three-tier offsetting | Legal priority ordering                 |
| Korrektionspulje      | Reversal and settlement lifecycle       |
| Rentegodtgoerelse     | Date rules and statutory exceptions     |
| Appeal deadlines      | Legally significant dates               |
| Ledger + immudb       | Financial integrity and tamper evidence |


**Same SDLC. Much higher stakes.**



---

## Documentation And Portfolio Visibility Are Outputs

- `implementation-doc-sync` checks documentation impact with every change
- `doc-writer` regenerates docs from petitions, specs, ADRs, and compliance data
- `petitions/program-status.yaml` is the portfolio source of truth
- Current snapshot: `52` implemented, `6` validated, `3` in progress, `1` blocked
- The goal is not magical perfection; it is to make drift hard to hide



---

## What Acceleration Really Means

- Less ambiguity before work starts
- Less architecture drift during implementation
- Less rework caused by late NFR discovery
- Less manual chasing of docs and delivery status
- More human attention on the decisions that actually matter



---



# Takeaway

### OpenDebt shows that the same governed pipeline can support:

### 1. Simple product changes

### 2. Cross-cutting platform concerns

### 3. Statutory and financially sensitive workflows

**AI accelerates complex delivery when it reduces ambiguity, drift, and rework.**

**Questions?**

