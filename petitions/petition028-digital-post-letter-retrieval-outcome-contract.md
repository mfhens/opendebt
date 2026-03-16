# Petition 028 Outcome Contract

## Acceptance criteria

1. A Digital Post client component retrieves letters sent by Gældsstyrelsen to a citizen's Digital Post inbox.
2. The citizen portal displays a "Mine breve" page listing letters with date sent, subject/type, and read/unread status.
3. Citizens can view letter content (PDF or HTML) directly in the portal.
4. The letter list is filtered to the authenticated citizen based on their `person_id` resolved to CPR via person registry.
5. Letter content is retrieved from the Digital Post API on demand and is not stored locally in OpenDebt.
6. If the citizen is exempt from Digital Post, the page displays an informational message about physical mail.
7. All user-facing text is externalized to message bundles with Danish (da-DK) and English (en-GB) translations.
8. The page is accessible: proper heading structure, keyboard navigation, screen reader support, and ARIA attributes.
9. A "Mine breve" link is present in the citizen portal navigation.
10. The Digital Post client uses injected `WebClient.Builder` for trace propagation (ADR-0024) and authenticates with OCES3 certificate.

## Definition of done

- The Digital Post client retrieves letter metadata and content from the Digital Post API.
- The "Mine breve" page renders a list of letters for the authenticated citizen.
- Letter content (PDF or HTML) is viewable or downloadable from the portal.
- The CPR lookup goes through person registry; no PII is logged or stored outside person registry.
- Digital Post exempt citizens see an appropriate informational message instead of an empty list.
- The page renders correctly in Danish and English.
- The page passes basic accessibility checks (heading structure, landmarks, keyboard navigation, focus order).
- The "Mine breve" link appears in the citizen portal navigation.
- The Digital Post API URL, sender ID, and certificate are configurable via `application.yml`.
- Every acceptance criterion is covered by at least one Gherkin scenario.

## Failure conditions

- The Digital Post client is missing or cannot connect to the Digital Post API.
- Letters from other senders appear in the citizen's letter list.
- Letter content is stored locally instead of being retrieved on demand.
- The citizen's CPR is logged, stored in OpenDebt outside person registry, or exposed in API responses.
- Digital Post exempt citizens see an error instead of an informational message.
- The "Mine breve" page is not accessible or not internationalized.
- The "Mine breve" navigation link is missing.
- The Digital Post client uses `WebClient.create()` instead of injected `WebClient.Builder`.
