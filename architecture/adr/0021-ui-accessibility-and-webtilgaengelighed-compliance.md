# ADR 0021: UI Accessibility and Webtilgængelighed Compliance

## Status
Accepted

## Context

OpenDebt is developed for Danish public authorities and exposes user interfaces for citizens, creditors, and potentially other public-sector roles. These web sites and any future mobile applications must comply with Danish web accessibility requirements.

Digitaliseringsstyrelsen states that public-sector web sites and mobile applications are covered by the Danish web accessibility law and are considered compliant when they satisfy the harmonized European standard **EN 301 549 v3.2.1**. For web sites and software, this maps primarily to **WCAG 2.1**, with additional requirements in EN 301 549 chapters 5, 6, 7, and 12 when relevant.

Digitaliseringsstyrelsen also requires a separate accessibility statement for each web site and mobile application, published through **WAS-Tool**, kept up to date when material changes occur, and updated at least annually.

OpenDebt therefore needs an explicit architecture and delivery rule for accessibility so that UI compliance is built into design, implementation, testing, release, and operations rather than treated as a later review activity.

## Decision

All current and future OpenDebt user interfaces shall be treated as accessibility-regulated public-sector digital services.

### Compliance baseline

1. **All public-facing web UIs shall conform to EN 301 549 v3.2.1 as applicable to web sites.**
2. **WCAG 2.1 AA shall be the practical baseline for UI design and implementation**, supplemented by relevant EN 301 549 requirements for:
   - documents
   - software behavior
   - support/documentation
   - audio/video or two-way communication, if such features are introduced
3. **Each web site and mobile application shall have its own accessibility statement** created in WAS-Tool.
4. **Each accessibility statement shall be updated on relevant change and at least annually.**
5. **Every UI release shall include accessibility verification** using both automated and manual checks.

### Scope

This decision applies to:

- `creditor-portal`
- `citizen-portal`
- any future admin or caseworker web UI
- any future mobile applications
- user-facing documents and help/support flows exposed through those UIs

### Engineering rules

1. UI components must support keyboard-only operation.
2. Focus order and visible focus indication must be preserved.
3. Semantic structure, headings, labels, names, roles, and values must be exposed to assistive technology.
4. Forms must provide accessible instructions, validation, and error messaging.
5. Color may not be the only carrier of meaning.
6. Contrast, zoom, and reflow must be supported within the compliance baseline.
7. Linked or embedded documents must be accessible, or an accessible alternative must be provided.
8. Support and feedback contact paths required by accessibility statements must not depend on inaccessible verification mechanisms.

### Delivery and operations rules

1. Accessibility requirements must be captured in petitions and acceptance criteria for UI work.
2. Automated accessibility checks should run in CI for UI projects when those projects are implemented.
3. Manual accessibility review must be performed for keyboard navigation, screen-reader critical flows, and form/error handling before production release.
4. Each UI must expose a discoverable link to its accessibility statement, ideally in the footer and, where practical, at `/was`.

## Consequences

### Positive

- Accessibility becomes a first-class quality requirement for all UIs
- Compliance work is shifted left into implementation and testing
- OpenDebt aligns with Danish public-sector obligations
- Citizens and creditors with disabilities gain more reliable access to the solution

### Negative

- UI delivery requires additional design, testing, and review effort
- Accessibility defects may block release readiness
- Accessibility statements require recurring governance work

### Mitigations

- Use reusable accessible UI patterns and components
- Include accessibility acceptance criteria in petitions up front
- Automate a subset of checks and reserve manual review for high-risk flows

## Alternatives considered

| Option | Reason not chosen |
|--------|-------------------|
| Treat accessibility as a later QA activity | Too late in the lifecycle and risky for legal compliance |
| Limit compliance to citizen-facing UIs only | Creditors and other users of public-sector UIs are also in scope |
| Rely on WCAG alone without EN 301 549 context | DIGST compliance is framed through EN 301 549 and accessibility statements |
