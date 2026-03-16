# Petition 022: Skyldnerportal (citizen portal) landing page

## Summary

The citizen portal (`opendebt-citizen-portal`) shall provide a public-facing landing page modeled after the Gældsstyrelsen "Se din gæld" page (gaeldst.dk/borger/se-din-gaeld). This page is the entry point for citizens (skyldnere/borgere) to understand their debt situation, access self-service, and find answers to common questions. The page shall be internationalized from the start (petition 021) with Danish as the primary language and English (en-GB) as the first additional language.

## Context and motivation

Gældsstyrelsen's current public website (gaeldst.dk) provides citizens with a "Se din gæld" landing page that explains how to view debt, links to the MitID-authenticated self-service portal ("Mit gældsoverblik"), lists contact information, and answers frequently asked questions about debt collection, interest, payment options, and debt limitation (forældelse).

OpenDebt needs to recreate this citizen-facing entry point as a Thymeleaf-based portal (ADR-0023) that can serve as the starting point for the citizen debt overview journey. The page must be accessible (petition 013), internationalized (petition 021), and follow the SKAT/Gældsstyrelsen visual design language already established in the Fordringshaverportal.

## Functional requirements

### Landing page content

1. The landing page shall display a heading: "Overblik over din gæld" (en-GB: "Overview of your debt").
2. The landing page shall explain the multiple ways a citizen can view their debt:
   a. Via the digital self-service solution (Mit gældsoverblik / MitID login).
   b. Via letters sent to Digital Post / mailbox.
   c. Via phone contact (display phone number).
3. The landing page shall include a prominent call-to-action linking to the MitID-authenticated self-service (configured URL, not hardcoded).
4. The landing page shall explain that a debt overview is a snapshot and that interest accrues daily.
5. The landing page shall include a Frequently Asked Questions (FAQ) section with expandable/collapsible items covering at minimum:
   a. "Hvor kan jeg se min samlede gæld?" / "Where can I see my total debt?"
   b. "Hvordan betaler jeg min gæld?" / "How do I pay my debt?"
   c. "Hvor meget skal jeg betale i rente?" / "How much interest do I need to pay?"
   d. "Hvad gør jeg, hvis jeg har svært ved at betale?" / "What do I do if I have payment difficulties?"
   e. "Kan jeg få rådgivning omkring min gæld?" / "Can I get advice about my debt?"
   f. "Skal jeg betale, selv om min gæld er gammel?" / "Do I have to pay my debt even if it is old?"
   g. "Hvem inddriver I gæld for?" / "For whom do you collect debt?"
6. FAQ answers shall contain relevant links to other pages (payment information, interest rates, payment difficulties, debt counselling, creditor list).
7. The landing page shall include a section about possible errors in old debt from the 2013-2015 period with a link to more information.

### Portal infrastructure

8. The citizen portal shall use the same SKAT design tokens and layout structure as the Fordringshaverportal (shared `skat-tokens.css`, header/breadcrumb/content/footer layout).
9. The citizen portal shall use Thymeleaf with the layout dialect for page composition.
10. The citizen portal shall use HTMX for any interactive elements (e.g., FAQ expand/collapse).
11. The citizen portal shall include a language selector in the header (petition 021).
12. The citizen portal shall include a skip-link, landmark roles, and accessible heading structure (petition 013).
13. The citizen portal shall include an accessibility statement page linked from the footer (petition 014).
14. All user-facing text shall be externalized to message bundles (`messages_da.properties`, `messages_en_GB.properties`) from the start.

### Navigation and routes

15. The landing page shall be served at the portal root path (`/borger` or `/`).
16. The MitID login link shall navigate to the authenticated self-service area (separate page, out of scope for this petition).
17. Links to payment information, interest rates, and other sub-pages shall use configurable URLs (initially pointing to gaeldst.dk equivalents until internal pages are built).

## Technical approach

- Thymeleaf SSR with layout dialect (same pattern as creditor-portal).
- Shared CSS (`skat-tokens.css`) copied or linked from common resources.
- FAQ section using `<details>` / `<summary>` HTML5 elements for native accessible expand/collapse, enhanced with HTMX if needed.
- Spring Boot controller returning the landing page view.
- All text via `#{...}` Thymeleaf message expressions.
- External URLs configurable via `application.yml`.

## Configuration example

```yaml
opendebt:
  citizen:
    external-links:
      mit-gaeldsoverblik: ${MIT_GAELDSOVERBLIK_URL:https://mitgaeldsoverblik.gaeldst.dk/}
      payment-info: ${PAYMENT_INFO_URL:https://gaeldst.dk/borger/saadan-betaler-du-din-gaeld}
      interest-rates: ${INTEREST_RATES_URL:https://gaeldst.dk/borger/betal-min-gaeld/renter-og-gebyrer}
      payment-difficulties: ${PAYMENT_DIFFICULTIES_URL:https://gaeldst.dk/borger/hvis-du-ikke-kan-betale-din-gaeld}
      debt-counselling: ${DEBT_COUNSELLING_URL:https://gaeldst.dk/borger/hvis-du-ikke-kan-betale-din-gaeld/brug-for-raadgivning-om-din-gaeld}
      creditor-list: ${CREDITOR_LIST_URL:https://gaeldst.dk/borger/om-gaeld-til-inddrivelse/se-hvem-vi-inddriver-gaeld-for}
      debt-errors: ${DEBT_ERRORS_URL:https://gaeldst.dk/borger/om-gaeld-til-inddrivelse/hvis-der-er-fejl-i-din-gaeld}
      phone-number: "70 15 73 04"
      phone-international: "+45 70 15 73 04"
  i18n:
    default-locale: da-DK
    supported-locales:
      - da-DK
      - en-GB
```

## Constraints and assumptions

- This petition covers the landing page only, not the authenticated self-service area (which requires MitID/TastSelv integration).
- FAQ content is based on gaeldst.dk as of March 2026 and may need periodic updates.
- External links default to gaeldst.dk pages until OpenDebt builds its own equivalents.
- The portal reuses the SKAT visual design language from ADR-0023 and the creditor portal implementation.

## Out of scope

- MitID/TastSelv authenticated self-service (debt overview, payment).
- Payment processing pages.
- Interest rate calculation pages.
- Debt counselling referral system.
- Admin or caseworker-facing pages.

## Dependencies

- Petition 013: UI webtilgængelighed compliance (accessibility baseline).
- Petition 014: Accessibility statements and feedback.
- Petition 021: Internationalization infrastructure.
