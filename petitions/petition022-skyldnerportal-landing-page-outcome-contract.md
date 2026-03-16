# Petition 022 Outcome Contract

## Acceptance criteria

1. The citizen portal serves a landing page at its root path with content modeled after gaeldst.dk/borger/se-din-gaeld.
2. The landing page displays a clear heading explaining the page purpose (debt overview).
3. The landing page explains three ways to view debt: self-service (MitID), letters (Digital Post), and phone.
4. A prominent call-to-action button links to the MitID-authenticated self-service portal (configurable URL).
5. The landing page explains that debt overviews are snapshots and that interest accrues daily.
6. An FAQ section displays at least 7 expandable/collapsible questions and answers.
7. FAQ items are accessible via keyboard and work with assistive technology.
8. A section about possible errors in old debt (2013-2015) is displayed with a link to more information.
9. The page uses the SKAT design tokens and layout structure (header, breadcrumb, content, footer).
10. All user-facing text is externalized to message bundles with Danish (da-DK) and English (en-GB) translations.
11. A language selector is visible in the header and switches the page language.
12. The page includes a skip-link, landmark roles, semantic headings, and an accessibility statement link in the footer.
13. External links (phone number, gaeldst.dk pages, Mit gældsoverblik) are configurable via application.yml.

## Definition of done

- The citizen portal starts and serves the landing page at its configured root path.
- The page renders correctly in Danish and English.
- All FAQ items are expandable/collapsible and keyboard-accessible.
- All external URLs are configurable, not hardcoded in templates.
- The SKAT visual design (dark navy header, breadcrumb, IBM Plex Sans) is applied.
- The page passes basic accessibility checks (heading structure, landmarks, skip-link, focus order).
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- The landing page is not served or returns an error.
- Any user-facing text is hardcoded in the template rather than using message bundles.
- FAQ items are not expandable/collapsible or are not keyboard-accessible.
- External URLs are hardcoded in templates instead of configured.
- The language selector is missing or non-functional.
- The page does not use the SKAT layout and design tokens.
- English translation is missing or incomplete.
