# Petition 012: `Fordringshaverportal` as BFF and manual submission channel

## Summary

OpenDebt shall treat `creditor-portal` as a human interaction channel and BFF-style aggregator, not as a master-data system of record and not as the primary M2M entry point. The portal shall support manual creditor operations by calling the owning backend services.

## Context and motivation

OpenDebt aims for straight-through processing as the default creditor path. The portal exists for manual interaction, visibility, and support for smaller creditors that cannot use the M2M path for all operations. This means the portal should orchestrate user interaction, but it should not own creditor master data or debt-domain persistence.

The portal therefore needs a clear responsibility split:

- read creditor profile and permissions from the creditor master-data service
- submit manual `fordringer` to `debt-service`
- show related case data from `case-service`

## Functional requirements

1. `creditor-portal` shall act as a user-facing interaction layer and not as the system of record for creditor master data.
2. `creditor-portal` shall authenticate portal users through the approved user-authentication flow.
3. `creditor-portal` shall resolve the acting `fordringshaver` through the creditor master-data backend service.
4. `creditor-portal` shall read creditor profile/configuration from the creditor master-data backend service.
5. `creditor-portal` shall submit manual `fordringer` to `debt-service` rather than persisting debt-domain state locally.
6. `creditor-portal` shall use the same acting-on-behalf-of rules as the M2M channel.
7. `creditor-portal` shall allow a user to act only for the bound `fordringshaver` or for an allowed represented `fordringshaver`.
8. `creditor-portal` shall not be used as the primary system-to-system integration endpoint for creditor systems.

## Constraints and assumptions

- The portal may cache or compose view models for user interaction, but it does not become the business system of record.
- This petition does not define the portal UI layout.
- This petition assumes backend service APIs exist for creditor lookup and debt submission.

## Out of scope

- Detailed portal screen design
- MitID Erhverv implementation details
- External M2M API design
- Full case-management UX
