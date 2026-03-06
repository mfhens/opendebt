# Petition 002: Creditor creation of a new fordring

## Summary

OpenDebt shall allow a fordringshaver to create a new fordring primarily via API and, for small fordringshavere, manually via the fordringshaverportal. API creation requires a valid OCES3 certificate, while portal creation requires MitID Erhverv login and is limited to the fordringshaver linked to the logged-in identity. Every submitted fordring shall be checked against the rules for inddrivelsesparathed; if the fordring is not inddrivelsesparat, the fordringshaver shall receive an error message stating the reason. If the fordring is inddrivelsesparat, OpenDebt shall create a new debt post for the person or company and keep bookkeeping updated.

## Context and motivation

OpenDebt needs a standard creditor submission flow for new fordringer. The default flow is system-to-system creation via API for fordringshavere that integrate directly with OpenDebt. In addition, small fordringshavere need a manual submission path through the fordringshaverportal.

Both channels must enforce the correct authentication and access model:

- API access is certificate-based using OCES3
- Portal access is user-based using MitID Erhverv
- Portal users may act only for the fordringshaver their MitID Erhverv identity is linked to

Before a submitted fordring can become an active debt in OpenDebt, it must pass the rules for inddrivelsesparathed. Submissions that fail this readiness check must be rejected with a reason. Submissions that pass must create the debt post and keep bookkeeping aligned with the creation.

## Functional requirements

1. A fordringshaver shall be able to create a new fordring via the API.
2. API creation shall be the default way for a fordringshaver to create a new fordring.
3. Small fordringshavere shall also be able to create a new fordring manually via the fordringshaverportal.
4. API creation shall require a valid OCES3 certificate.
5. Portal creation shall require MitID Erhverv login.
6. In the fordringshaverportal, a user shall be allowed to create fordringer only for the fordringshaver linked to that user’s MitID Erhverv identity.
7. Every submitted fordring shall be evaluated against the rules to determine whether it is inddrivelsesparat.
8. If a submitted fordring is not inddrivelsesparat, OpenDebt shall reject the creation and return an error message that states the reason.
9. If a submitted fordring is inddrivelsesparat, OpenDebt shall create a new debt post for the relevant person or company.
10. When a new debt post is created from an inddrivelsesparat fordring, bookkeeping shall be updated accordingly.

## Constraints and assumptions

- The API is the primary creditor submission channel; the portal is a supplementary manual channel.
- This petition does not define how a fordringshaver is classified as “small”.
- This petition does not define the detailed payload fields for creating a fordring.
- This petition does not define the technical format of the returned error message beyond requiring that it states the reason.
- “Keep bookkeeping updated” is intentionally kept at a high level and does not define accounts, postings, or booking sequence.
- If authentication, access control, or inddrivelsesparathed validation fails, no debt post is created.

## Out of scope

- Detailed OCES3 certificate onboarding, issuance, and lifecycle management
- Detailed MitID Erhverv federation, session, and identity resolution behavior
- The detailed business rules that determine inddrivelsesparathed
- Portal UI design and form layout
- Detailed bookkeeping implementation, account mapping, and reconciliation behavior
