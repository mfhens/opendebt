# Petition 028: Digital Post integration for citizen letter retrieval

## Summary

OpenDebt shall integrate with Digital Post (Denmark's public-sector digital mailbox operated by Digitaliseringsstyrelsen) to allow citizens to view letters sent by Gældsstyrelsen through the citizen portal. The citizen portal shall display a "Mine breve" (My letters) page listing debt collection letters, payment reminders, demand notices, and other correspondence, with the ability to view or download letter content directly.

## Context and motivation

Gældsstyrelsen sends formal correspondence to citizens via Digital Post, including debt overviews (gældsoversigt), payment reminders (rykkere), demand notices (påkrav), instalment plan letters, and objection responses. Petition 004 and ADR-0008 cover the sending side of letter management.

Citizens currently need to log into Digital Post separately to find their Gældsstyrelsen letters. Integrating letter retrieval into the citizen portal ("Mit gældsoverblik") gives citizens a single place to see both their debt situation (petition 026) and the correspondence related to it.

Digital Post is the system of record for delivered letters. The citizen portal shall retrieve letters on demand from the Digital Post API rather than storing copies locally, avoiding data duplication and ensuring citizens always see the authoritative delivery state.

Some citizens are exempt from Digital Post and receive physical mail instead. The portal shall handle this gracefully by displaying an informational message.

## Functional requirements

1. A Digital Post client component (in `letter-service` or as a shared client) shall retrieve letters sent to a citizen's Digital Post inbox by Gældsstyrelsen.
2. The citizen portal shall display a "Mine breve" (en-GB: "My letters") page listing letters sent by Gældsstyrelsen.
3. Each letter entry shall show:
   - Date sent (afsendelsesdato)
   - Subject or letter type (e.g., "Påkrav", "Rykker", "Gældsoversigt")
   - Read/unread status
4. Citizens shall be able to view letter content (PDF or HTML) directly in the portal.
5. The letter list shall be filtered to the authenticated citizen's CPR number, resolved via `person_id` through the person registry (ADR-0014 GDPR isolation).
6. Letters shall be retrieved from the Digital Post API on each request; letter content shall not be stored locally in OpenDebt (Digital Post is the system of record).
7. If the citizen is exempt from Digital Post, the page shall display an informational message explaining that their correspondence is sent by physical mail.
8. The page shall be internationalized (petition 021) with Danish as primary and English (en-GB) as additional language.
9. The page shall be accessible (petition 013, petition 014) with proper heading structure, keyboard navigation, and screen reader support.
10. A "Mine breve" link shall be added to the citizen portal navigation.

## Technical approach

- The Digital Post integration uses the Digitaliseringsstyrelsen REST API for retrieving messages sent by the registered Gældsstyrelsen sender.
- The `DigitalPostClient` follows the service client pattern: inject `WebClient.Builder` for trace propagation (ADR-0024), authenticate with OCES3 system certificate.
- The citizen's CPR is resolved from `person_id` via the person registry client; the CPR is never logged or exposed in responses (ADR-0014).
- Letter list and content retrieval are separate API calls to Digital Post, allowing lazy loading of content.
- The "Mine breve" page uses Thymeleaf + HTMX (ADR-0023), matching the citizen portal design (petition 022).
- Pagination is handled server-side if the Digital Post API supports it; otherwise the most recent letters are fetched with a configurable limit.
- PDF letter content is served via a proxy endpoint in the citizen portal backend to avoid exposing the Digital Post API directly to the browser.

## Configuration example

```yaml
opendebt:
  digital-post:
    enabled: true
    api-url: ${DIGITAL_POST_API_URL:https://api.digitalpost.dk/v1}
    sender-id: ${DIGITAL_POST_SENDER_ID:}
    certificate:
      keystore: ${DIGITAL_POST_KEYSTORE_PATH:classpath:certs/oces3.p12}
      keystore-password: ${DIGITAL_POST_KEYSTORE_PASSWORD:}
    letter-fetch-limit: ${DIGITAL_POST_FETCH_LIMIT:50}
  citizen:
    navigation:
      mine-breve:
        enabled: true
        path: /borger/mine-breve
```

## Constraints and assumptions

- The citizen is authenticated via MitID (petition 025) and their `person_id` is available from the session context.
- The citizen's CPR is resolved from `person_id` via the person registry; no PII is stored or logged outside person registry (ADR-0014).
- Digital Post API access requires OCES3 system certificate and sender registration with Digitaliseringsstyrelsen.
- Digital Post is the system of record; OpenDebt does not cache or persist letter content.
- Citizens exempt from Digital Post have a known exemption status that can be queried via the Digital Post API.
- The portal reuses the SKAT visual design language (ADR-0023) and citizen portal layout established in petition 022.

## Out of scope

- Sending letters via Digital Post (covered by petition 004 and ADR-0008).
- Physical mail fallback tracking and status.
- Digital Post onboarding for citizens.
- Letter template authoring and management.
- Batch retrieval or background synchronization of letters.

## Dependencies

- Petition 004: Underretning, påkrav, rykker (the sending side that produces the letters citizens retrieve).
- Petition 013: UI webtilgængelighed compliance (accessibility).
- Petition 014: Accessibility statements and feedback.
- Petition 021: Internationalization infrastructure.
- Petition 022: Citizen portal landing page (portal foundation and navigation).
- Petition 025: MitID authentication (citizen identity and session).
- Petition 026: Citizen debt overview page (provides navigation context).
- ADR-0008: Letter management.
- ADR-0014: GDPR data isolation (CPR resolution via person registry).
- ADR-0023: Thymeleaf + HTMX frontend.
- ADR-0024: Observability and trace propagation.
