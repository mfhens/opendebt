# Petition 025: MitID/TastSelv OAuth2 browser flow for citizen portal

## Summary

The citizen portal (`opendebt-citizen-portal`) shall authenticate citizens via MitID through the TastSelv OAuth2 provider, using the Authorization Code flow built into Spring Security's OAuth2 Client support. After successful MitID login the portal shall resolve the citizen's CPR number to a `person_id` UUID via the person-registry service (petition 023) and store that identifier in the HTTP session for downstream API calls.

## Context and motivation

MitID is Denmark's national electronic identification system (successor to NemID). Gældsstyrelsen's self-service platform, TastSelv, integrates MitID and acts as the OAuth2/OIDC identity provider for citizen-facing applications. Citizens accessing authenticated pages in the OpenDebt citizen portal — such as "Mit gældsoverblik" (petition 026) — must first prove their identity through MitID.

The citizen portal's `application.yml` already contains placeholder configuration for a TastSelv OAuth2 client registration (`spring.security.oauth2.client.registration.tastselv`) and provider (`spring.security.oauth2.client.provider.tastselv`), including `client-id`, `client-secret`, `issuer-uri`, `authorization-uri`, `token-uri`, `user-info-uri`, and `jwk-set-uri`. However, the `SecurityConfig` is not yet wired to use these for an OAuth2 login flow, and no post-authentication logic exists.

Separately, Keycloak is used for internal service-to-service authentication. The citizen portal must support **both** providers: TastSelv/MitID for citizen browser sessions, and the Keycloak resource server configuration for internal API token validation. For local development and testing, a Keycloak realm can mock the TastSelv provider so that developers do not need access to the real MitID test environment.

## Functional requirements

### OAuth2 login flow

1. `SecurityConfig` shall configure an OAuth2 Authorization Code login flow using the `tastselv` client registration already defined in `application.yml`.
2. Unauthenticated access to public pages (`/`, `/was`, static resources `/css/**`, `/js/**`, `/fonts/**`, `/webjars/**`, and actuator health `/actuator/health`) shall remain permitted without login.
3. Accessing any authenticated page (e.g., `/min-gaeld`) shall trigger a redirect to the TastSelv/MitID authorization endpoint.
4. After successful authentication, Spring Security shall receive an ID token (JWT) from TastSelv containing the citizen's CPR number in a configurable claim (default: `dk:gov:saml:attribute:CprNumberIdentifier`).
5. The CPR claim name shall be configurable via `application.yml` to accommodate different OIDC provider claim mappings.

### Person resolution

6. After successful login, a custom `OAuth2UserService` or authentication success handler shall extract the citizen's CPR from the JWT claims.
7. The CPR shall be resolved to a `person_id` UUID by calling the person-registry service's CPR lookup API (petition 023).
8. If the CPR cannot be resolved (person not found in person-registry), the citizen shall be shown a user-friendly error page explaining that their identity could not be matched and providing contact information.
9. The resolved `person_id` shall be stored in the HTTP session (e.g., as a session attribute `CITIZEN_PERSON_ID`) for use by downstream controllers.
10. The CPR number itself shall **not** be stored in the session, logged, or persisted anywhere in the citizen portal — only the `person_id` UUID is retained (GDPR compliance, ADR-0014).

### Logout

11. A logout endpoint (`/logout`) shall clear the HTTP session and Spring Security context.
12. After logout, the user shall be redirected to the landing page (`/`).
13. If TastSelv supports RP-Initiated Logout (OIDC back-channel or front-channel), the logout flow shall also invalidate the TastSelv session. If not supported, only the local session is cleared.

### Development mock flow

14. For local development (`spring.profiles.active=dev`), the TastSelv provider configuration shall be overridable to point to a local Keycloak realm that mimics TastSelv.
15. The Keycloak mock realm shall issue tokens with a CPR claim so that the full login-to-person-resolution flow can be tested without real MitID credentials.
16. The mock configuration shall be documented in `application-dev.yml` with Keycloak URLs pointing to `localhost`.

### Service client

17. A `PersonRegistryClient` component shall call the person-registry's CPR lookup endpoint using an injected `WebClient.Builder` (ADR-0024 trace propagation).
18. The `PersonRegistryClient` shall use the service URL configured in `application.yml` at `opendebt.services.person-registry.url`.
19. The person-registry service URL shall be added to the citizen portal's `application.yml` configuration alongside the existing `debt-service`, `case-service`, and `payment-service` URLs.

## Technical approach

- **Spring Security OAuth2 Client**: Use `spring-boot-starter-oauth2-client` (already a dependency) with `.oauth2Login()` in `SecurityConfig`.
- **Custom `OidcUserService`**: Extend the default OIDC user service to extract the CPR claim, call person-registry, and store `person_id` in a custom `OidcUser` wrapper or in the authentication principal attributes.
- **Session storage**: Store `person_id` as a session attribute. Spring Session can be added later for distributed sessions.
- **WebClient.Builder**: Inject the Spring-managed `WebClient.Builder` for person-registry calls (Micrometer tracing filters for W3C `traceparent` propagation).
- **Error handling**: Custom error page for person-resolution failures, distinct from generic Spring Security error handling.

## Configuration example

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          tastselv:
            client-id: opendebt-citizen
            client-secret: ${TASTSELV_CLIENT_SECRET}
            scope: openid,profile
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          tastselv:
            issuer-uri: ${TASTSELV_ISSUER_URI}
            authorization-uri: ${TASTSELV_AUTH_URI}
            token-uri: ${TASTSELV_TOKEN_URI}
            user-info-uri: ${TASTSELV_USERINFO_URI}
            jwk-set-uri: ${TASTSELV_JWK_URI}

opendebt:
  citizen:
    auth:
      cpr-claim-name: ${CPR_CLAIM_NAME:dk:gov:saml:attribute:CprNumberIdentifier}
    tastselv-integration:
      enabled: true
      base-url: ${TASTSELV_BASE_URL:https://www.tastselv.skat.dk}
  services:
    person-registry:
      url: ${PERSON_REGISTRY_URL:http://localhost:8084}
    debt-service:
      url: ${DEBT_SERVICE_URL:http://localhost:8082}
    case-service:
      url: ${CASE_SERVICE_URL:http://localhost:8081}
    payment-service:
      url: ${PAYMENT_SERVICE_URL:http://localhost:8083}
```

## Constraints and assumptions

- This petition assumes TastSelv exposes a standard OIDC-compliant authorization server with `authorization_code` grant type support.
- The CPR claim in the ID token is assumed to be present after successful MitID authentication via TastSelv. The exact claim name may vary by environment and is therefore configurable.
- Person-registry must expose a CPR lookup endpoint (petition 023 dependency). If the endpoint is not yet available, the authentication success handler can be stubbed to return a fixed `person_id` for development.
- The citizen portal does not store any PII. The CPR is used transiently during the login flow to resolve the `person_id` and is then discarded.
- Session management is server-side (HTTP session). Distributed session support (e.g., Spring Session with Redis) is out of scope but can be added later for horizontal scaling.

## Out of scope

- Real MitID test environment provisioning and certificate setup.
- TastSelv client registration in production (handled by Gældsstyrelsen IT operations).
- Distributed session management (Spring Session).
- Multi-factor authentication beyond what MitID provides.
- Token refresh for long-lived sessions (standard Spring Security token lifecycle management applies).
- Service-to-service authentication between citizen portal and backend services (existing Keycloak resource server configuration covers this).

## Dependencies

- Petition 023: Person registry CPR lookup API (resolves CPR → `person_id`).
- Petition 022: Citizen portal landing page (provides the public pages and layout infrastructure).
- ADR-0014: GDPR data isolation (CPR must not be stored outside person-registry).
- ADR-0024: Observability and trace propagation (WebClient.Builder for service clients).
