# Petition 013: UI webtilgængelighed compliance

## Summary

All OpenDebt user interfaces shall comply with the Danish web accessibility requirements applicable to public-sector web sites and mobile applications. For web UIs, EN 301 549 v3.2.1 with WCAG 2.1 AA as the practical implementation baseline shall be treated as a mandatory non-functional requirement.

## Context and motivation

OpenDebt serves citizens, creditors, and public-sector users. These users must be able to use OpenDebt regardless of disability or functional impairment. Accessibility is therefore not optional UX quality; it is a delivery requirement for public-sector digital services.

OpenDebt must incorporate accessibility into UI design, implementation, testing, and release management. If accessibility is added late, defects become expensive to fix and may block legal compliance.

## Functional requirements

1. All OpenDebt web UIs shall support keyboard-only operation for core user journeys.
2. All OpenDebt web UIs shall expose visible focus indication and logical focus order.
3. All OpenDebt web UIs shall use semantic structure so that headings, landmarks, controls, labels, and status messages are available to assistive technology.
4. Forms shall provide accessible labels, instructions, validation feedback, and error identification.
5. Color shall not be the only means of conveying information.
6. Text and UI components shall meet the applicable contrast requirements in the compliance baseline.
7. UI layouts shall support zoom and reflow within the applicable compliance baseline.
8. Linked or embedded documents shall be accessible, or an accessible alternative shall be provided.
9. If a UI introduces video, audio, or two-way communication features, the relevant EN 301 549 requirements for those media types shall also be met.
10. Accessibility verification shall be part of release readiness for every UI.

## Constraints and assumptions

- This petition defines the compliance baseline and required qualities, not the final component-library choice.
- WCAG 2.1 AA is used as the practical baseline for web implementation, but the governing compliance reference remains EN 301 549 v3.2.1.
- This petition applies to existing portals and any future web/mobile UIs.

## Out of scope

- Exact UI design system selection
- The full content of accessibility statements
- Detailed testing-tool selection
