# Petition 038 — Outcome Contract

## Summary

Implement the creditor portal dashboard homepage with summary claim counts, a portal-wide secondary navigation component with role-based visibility, default landing logic per role, umbrella-user claimant selection, a settings page for creditor agreement configuration and contact email management, and a system status indicator.

## Acceptance Criteria

### Dashboard homepage

- AC-1: The portal homepage (`/`) displays four summary counts (claims in recovery, claims in hearing, rejected claims, zero-balance claims) for the acting creditor.
- AC-2: Each count is a link to the corresponding filtered list page.
- AC-3: Summary counts are loaded asynchronously via HTMX after the page shell renders.
- AC-4: The existing creditor profile card and shortcuts remain on the dashboard.

### Portal navigation

- AC-5: A secondary navigation component is rendered within the main content area on all portal pages.
- AC-6: The navigation component is a `<nav>` element with `aria-label` for accessibility.
- AC-7: Navigation items are filtered server-side based on the user's roles — inaccessible items are not rendered in the DOM.
- AC-8: The active navigation item is visually indicated and announced to assistive technology via `aria-current="page"`.
- AC-9: External links (Aftalemateriale, Kontakt, Guides) open in a new tab and their URLs are configurable via `application.yml` properties (not hardcoded).
- AC-10: All navigation labels use message bundles (i18n).

### Default landing logic

- AC-11: Standard claim users (CREDITOR_VIEWER, CREDITOR_EDITOR) land on `/` (dashboard).
- AC-12: Users with only CREDITOR_RECONCILIATION role land on `/afstemning`.
- AC-13: Umbrella-users land on `/vaelg-fordringshaver` (claimant selection).

### Umbrella-user claimant selection

- AC-14: A claimant selection page (`/vaelg-fordringshaver`) lists all creditors the user may act on behalf of.
- AC-15: Selecting a creditor sets the acting creditor context for the session.
- AC-16: The selection is changeable at any time via the dashboard header.

### Settings page

- AC-17: A settings page (`/indstillinger`) displays the creditor's agreement configuration (portal actions allowed, allowed claim types, allowed debtor types, notification preferences).
- AC-18: Editors (CREDITOR_EDITOR) can manage the creditor's contact email on the settings page.
- AC-19: Email changes are submitted to creditor-service via the BFF.

### System status indicator

- AC-20: The navigation area includes a system status indicator showing backend service operational status.

### Cross-cutting

- AC-21: All new text uses message bundles with Danish (da) and English (en-GB) translations.
- AC-22: All templates use `skat-tokens.css` for styling and follow SKAT standardlayout.
- AC-23: All source code identifiers are in English.

## Affected Components

- `opendebt-creditor-portal` — controllers, templates, fragments, i18n bundles, configuration
- No backend API changes (consumes existing creditor-service and debt-service endpoints)

## Out of Scope

- Backend creditor-service or debt-service API implementation
- Keycloak role provisioning
- OAuth2/MitID authentication details
