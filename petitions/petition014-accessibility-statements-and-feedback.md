# Petition 014: Accessibility statements and feedback for each UI

## Summary

Each OpenDebt web site and future mobile application shall publish and maintain its own accessibility statement in accordance with Digitaliseringsstyrelsen requirements. The statement shall be discoverable, updated on relevant change and at least annually, and provide accessible written and telephone contact options.

## Context and motivation

Compliance is not limited to technical UI behavior. Public-sector web accessibility also requires a maintained accessibility statement per web site or app, plus a contact path for users who encounter inaccessible content.

OpenDebt therefore needs an explicit implementation requirement for accessibility statements and feedback channels, so that operational compliance is built into every UI rollout.

## Functional requirements

1. Each OpenDebt web site shall have its own accessibility statement.
2. Each future OpenDebt mobile application shall have its own accessibility statement.
3. Accessibility statements shall be created using WAS-Tool where applicable to public-sector obligations.
4. Each accessibility statement shall be updated when changes occur that affect the information in the statement.
5. Each accessibility statement shall be updated at least once annually.
6. Each accessibility statement shall provide both written and telephone contact information.
7. The written contact path shall not require MitID login or depend on inaccessible verification mechanisms such as inaccessible CAPTCHA.
8. Each web UI shall make the accessibility statement easy to find, preferably through a footer link labeled `Tilgængelighedserklæring` and, where practical, through `/was`.
9. For future mobile applications, the accessibility statement shall be reachable from the app store listing or the authority web site, and preferably also from inside the application.

## Constraints and assumptions

- This petition defines the compliance obligations and discoverability requirements, not the exact organization responsible for maintaining the statements.
- Existing OpenDebt portals are each treated as separate web sites for statement purposes unless later consolidated.
- This petition assumes OpenDebt is operated as a public-sector solution subject to DIGST requirements.

## Out of scope

- Detailed text of each accessibility statement
- Detailed WAS-Tool operating instructions
- Complaint handling workflow beyond making contact paths available
