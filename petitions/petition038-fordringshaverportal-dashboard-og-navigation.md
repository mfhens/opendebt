# Petition 038: Fordringshaverportal -- Dashboard, navigation og indstillinger

## Summary

The Fordringshaverportal shall provide a dashboard homepage with summary counts, a portal-wide navigation structure compliant with the SKAT standardlayout, default landing logic based on roles, claimant selection for umbrella-users, and a settings area for managing claimant contact information.

## Context and motivation

The portal needs a clear navigation structure and an entry point that gives creditors an overview of their claims status. The legacy portal used a React sidebar with Material-UI. In OpenDebt, the portal uses the SKAT standardlayout (ADR-0023) with Thymeleaf + HTMX. Navigation must accommodate 10+ sections while remaining compliant with the SKAT visual identity and accessibility requirements.

**Supersedes:** Fordringshaverportal petitions 012 (claims count -- dashboard integration), 013 (claimant agreement and contact info -- settings), 015 (frontend architecture -- navigation), 017 (homepage and landing).
**References:** Petition 008 (creditor data model), Petition 009 (creditor master data), Petition 010 (channel binding), Petition 012 (BFF), Petition 013 (accessibility), Petition 021 (i18n).

## Functional requirements

### Dashboard homepage

1. The portal homepage shall display summary counts for the acting creditor:
   - Fordringer i inddrivelse (claims in recovery) -- count
   - Fordringer i hoering (claims in hearing) -- count
   - Afviste fordringer (rejected claims) -- count
   - Nulfordringer (zero-balance claims) -- count
2. Each count shall link to the corresponding list page.
3. The homepage shall display the creditor profile card (existing implementation in `index.html`).
4. Summary counts shall be loaded via HTMX after the page shell renders.

### Portal navigation

5. The portal shall provide a navigation component within the SKAT standardlayout that links to all portal sections.
6. The navigation component shall use a secondary navigation pattern within the content area (left-side nav or horizontal sub-nav below the breadcrumb), consistent with SKAT design patterns.
7. Navigation items (visibility controlled by roles and permissions):

   | Menu Item | Route | Required Role/Permission |
   |-----------|-------|--------------------------|
   | Forside | / | All authenticated |
   | Til inddrivelse | /fordringer/inddrivelse | CREDITOR_VIEWER or CREDITOR_EDITOR |
   | Nulfordringer | /fordringer/nulfordringer | CREDITOR_VIEWER or CREDITOR_EDITOR |
   | I hoering | /fordringer/hoering | CREDITOR_VIEWER or CREDITOR_EDITOR |
   | Afviste | /fordringer/afviste | CREDITOR_VIEWER or CREDITOR_EDITOR |
   | Opret fordring | /fordring/ny | CREDITOR_EDITOR + allow_portal_actions + allow_create_recovery_claims |
   | Underretninger | /underretninger | CREDITOR_VIEWER or CREDITOR_EDITOR |
   | Rapporter | /rapporter | All authenticated |
   | Afstemning | /afstemning | CREDITOR_RECONCILIATION or CREDITOR_SUPPORT |
   | Indstillinger | /indstillinger | All authenticated |

8. External links (open in new tab):
   - Aftalemateriale -> configurable URL (default: https://gaeldst.dk/fordringshaver/individuelle-aftaler)
   - Kontakt -> configurable URL (default: https://www.gaeldst.dk/fordringshaver/fordringshaversupport/)
   - Guides -> configurable URL (default: https://www.gaeldst.dk/fordringshaver/find-vejledninger/guides-til-fordringshaverportalen/)
9. External link URLs shall be configurable via application properties, NOT hardcoded in templates.

### Default landing logic

10. After login, the portal shall redirect to the appropriate page based on the user's roles:
    - Standard claim roles (CREDITOR_VIEWER, CREDITOR_EDITOR) -> `/` (dashboard)
    - CREDITOR_RECONCILIATION only -> `/afstemning`
    - Umbrella-users -> `/vaelg-fordringshaver` (claimant selection)

### Umbrella-user claimant selection

11. If the authenticated user is associated with a parent creditor that has acting-on-behalf-of rights (petition 008, petition 010), the user shall be presented with a claimant selection page.
12. The selection page shall list all creditors the user may act on behalf of.
13. Selecting a creditor shall set the acting creditor context for the session (via `?actAs=` parameter or session attribute).
14. The selection shall be changeable at any time via the dashboard or header.

### Claimant settings and contact information

15. The portal shall provide a settings page showing the creditor's agreement configuration:
    - Whether portal actions are allowed
    - Allowed claim types
    - Allowed debtor types
    - Notification preferences
16. The settings page shall allow editors to manage the creditor's contact email:
    - Add, update, or remove the notification email address.
    - Email changes shall be submitted to `creditor-service` through the BFF.
    - All contact info changes shall be logged to the audit log.
17. The creditor agreement shall be cacheable with a manual refresh button.

### System status indicator

18. The portal navigation area shall include a system status indicator showing whether backend services are operational.

### Access control

19. Dashboard and navigation: all authenticated portal users.
20. Claimant selection: only umbrella-users.
21. Contact info update: `CREDITOR_EDITOR` role.

## Layout and accessibility -- SKAT standardlayout compliance

22. The portal shall use the SKAT standardlayout implemented in `templates/layout/default.html`:
    - Skip link to main content
    - Language selector ribbon (petition 021)
    - SKAT header with logo and primary navigation (Borger, Erhverv, Soeg, Log ind)
    - Breadcrumb navigation
    - Main content area with portal-specific secondary navigation
    - Footer with accessibility statement link (/was) (petition 014)
23. The portal secondary navigation shall be implemented as a `<nav>` element with `aria-label` for screen readers, placed within the main content area.
24. The active navigation item shall be visually indicated and announced to assistive technology.
25. Navigation items shall be filtered server-side based on the user's roles -- items the user cannot access shall not be rendered in the DOM.
26. The portal shall use `skat-tokens.css` for all styling (color palette, spacing, typography from SKAT design language).
27. The portal font shall be IBM Plex Sans (as configured in existing skat-tokens.css), NOT Open Sans (legacy).
28. The portal language shall default to Danish (`lang="da"`) with English available via language selector.
29. All user-facing text shall use message bundles (petition 021).

## Data source mapping

| Old source | OpenDebt source |
|---|---|
| PSRM DKGetClaimsCountForClaimant | `debt-service` GET /api/v1/debts/count?creditorId={id}&status=... |
| PSRM ClaimantAgreement | `creditor-service` GET /api/v1/creditors/{id}/agreement |
| PSRM DKUpdateClaimantContactInfo | `creditor-service` PUT /api/v1/creditors/{id}/contact |
| DCS SSO / Login / Logout | Keycloak / MitID Erhverv (existing OAuth2 flow) |
| DCS GetEntityInformation | Keycloak user info endpoint |

## Constraints and assumptions

- The portal uses Keycloak for authentication (replacing DCS SSO). Role mappings are configured in Keycloak realm roles.
- The SKAT standardlayout is already implemented and operational in the creditor portal.
- External links are configurable to support different environments.
- This petition defines the portal navigation and dashboard, not backend API contracts.

## Out of scope

- OAuth2/MitID Erhverv authentication implementation details
- Backend creditor-service API design
- Detailed role provisioning in Keycloak
- Internal caseworker dashboard
