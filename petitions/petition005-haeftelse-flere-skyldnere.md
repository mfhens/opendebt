# Petition 005: Explicit hæftelse and support for multiple skyldnere

## Summary

OpenDebt shall model the relation between `fordring` and `skyldner` through an explicit `hæftelse` concept. A `fordring` may be linked to one or more `skyldnere`, and each relation shall state the relevant liability type. OpenDebt shall support at least `enehæftelse`, `solidarisk hæftelse`, and `delt hæftelse`.

## Context and motivation

The current codebase assumes a single debtor reference on a debt-like object. That is too crude for the begrebsmodel. The business needs an explicit liability model because:

- one claim may have more than one liable party
- different liable parties may have different liability forms
- communication, case handling, and collection logic depend on who is liable and how

Without `hæftelse`, OpenDebt cannot represent several central public-sector claim situations correctly.

## Functional requirements

1. OpenDebt shall represent the relationship between a `fordring` and a `skyldner` through an explicit `hæftelse` object.
2. A `fordring` shall be allowed to have one or more `hæftelser`.
3. Each `hæftelse` shall relate exactly one `fordring` to exactly one `skyldner`.
4. OpenDebt shall support at least these liability types:
   - `enehæftelse`
   - `solidarisk hæftelse`
   - `delt hæftelse`
5. A `fordring` with one liable party shall be expressible as `enehæftelse`.
6. A `fordring` with multiple jointly liable parties shall be expressible as `solidarisk hæftelse`.
7. A `fordring` with multiple parties responsible for defined portions shall be expressible as `delt hæftelse`.
8. When OpenDebt models `delt hæftelse`, the system shall preserve the relevant share or amount for each liable relation.
9. Communication and collection handling shall be able to determine which `skyldnere` are relevant from the registered `hæftelser`.

## Constraints and assumptions

- This petition establishes the liability structure but does not define every legal variant beyond the three listed forms.
- This petition does not define final UI behaviour for presenting multiple liable parties.
- This petition does not define the detailed enforcement allocation rules for payments across solidarity relationships.
- Person and organization identity remain stored through technical UUID references only.

## Out of scope

- Detailed payment allocation rules between multiple liable parties
- Full legal edge-case catalogue for liability changes
- Detailed debtor self-service UX
