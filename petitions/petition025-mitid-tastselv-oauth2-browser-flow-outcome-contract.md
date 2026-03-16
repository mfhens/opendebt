# Petition 025 Outcome Contract

## Acceptance criteria

1. `SecurityConfig` configures an OAuth2 Authorization Code login flow using the `tastselv` client registration from `application.yml`.
2. Public pages (`/`, `/was`, static resources, actuator health) remain accessible without authentication.
3. Accessing an authenticated page (e.g., `/min-gaeld`) redirects the user to the TastSelv/MitID authorization endpoint.
4. After successful MitID login, the citizen's CPR is extracted from a configurable JWT claim.
5. The CPR is resolved to a `person_id` UUID by calling the person-registry CPR lookup API.
6. The `person_id` is stored in the HTTP session for use by downstream controllers.
7. The CPR number is never stored in the session, logged, or persisted in the citizen portal (GDPR compliance).
8. If the CPR cannot be resolved in person-registry, a user-friendly error page is displayed with contact information.
9. A logout endpoint (`/logout`) clears the session and redirects to the landing page (`/`).
10. For local development, the TastSelv provider can be replaced by a Keycloak mock realm that issues tokens with a CPR claim.
11. A `PersonRegistryClient` calls person-registry using an injected `WebClient.Builder` (trace propagation per ADR-0024).
12. The person-registry service URL is configurable via `application.yml` under `opendebt.services.person-registry.url`.
13. The CPR claim name is configurable via `application.yml` under `opendebt.citizen.auth.cpr-claim-name`.

## Definition of done

- The citizen portal starts and serves public pages without requiring authentication.
- Navigating to an authenticated page triggers the OAuth2 Authorization Code redirect.
- After mock MitID login (Keycloak dev realm), the portal extracts the CPR, resolves `person_id`, and stores it in the session.
- The `person_id` is available as a session attribute (`CITIZEN_PERSON_ID`) for subsequent controller requests.
- No CPR numbers appear in application logs, session stores, or persisted data.
- The logout flow clears the session and returns the user to the landing page.
- Person-registry client uses injected `WebClient.Builder` (not `WebClient.create()`), verified by ArchUnit test.
- Every acceptance criterion is covered by at least one Gherkin scenario or unit test.

## Failure conditions

- Public pages require authentication or are blocked by the security filter chain.
- The OAuth2 login flow is not triggered when accessing authenticated pages.
- CPR numbers are logged, stored in the session, or persisted anywhere in the citizen portal.
- Person-resolution failure results in a stack trace or generic error page instead of a user-friendly message.
- The `PersonRegistryClient` uses `WebClient.create()` instead of the injected `WebClient.Builder`.
- Logout does not clear the session or does not redirect to the landing page.
- The TastSelv provider configuration cannot be overridden for local development with Keycloak.
- The CPR claim name is hardcoded instead of configurable.
