# ADR 0005: Keycloak for Authentication and Authorization

## Status
Accepted

## Context
OpenDebt has multiple user types with different authentication needs:

| User Type | Authentication Method | Identity Provider |
|-----------|----------------------|-------------------|
| Internal caseworkers | MS Entra ID (via Omada) | Keycloak federation |
| Creditor users | OCES3 certificates | Keycloak client |
| Citizens | MitID via TastSelv | TastSelv integration |
| System-to-system | OCES3 certificates | Direct / Keycloak |

The Horizontale Driftsplatform provides a shared Keycloak instance for JWT token issuance.

## Decision
We use Keycloak as the central identity broker:

### Architecture
```
┌─────────────────┐     ┌─────────────────┐
│   MS Entra ID   │────▶│                 │
│   (via Omada)   │     │                 │
└─────────────────┘     │    Keycloak     │──▶ JWT tokens
┌─────────────────┐     │    (Platform)   │
│  OCES3 Certs    │────▶│                 │
└─────────────────┘     └─────────────────┘
```

### Roles
- `ROLE_CITIZEN` - Borger accessing own data
- `ROLE_CREDITOR` - Fordringshaver submitting debts
- `ROLE_CASEWORKER` - Internal case handler
- `ROLE_SUPERVISOR` - Team lead with approval rights
- `ROLE_ADMIN` - System administrator

### Token Claims
- `sub` - User identifier
- `organization` - CVR for creditors
- `roles` - Application roles
- `cpr` - CPR number (for citizens, encrypted)

### Spring Security Configuration
- OAuth2 Resource Server for JWT validation
- Method-level security with `@PreAuthorize`
- Custom `AuthenticationConverter` for claim mapping

## Consequences

### Positive
- Single sign-on across services
- Standard OAuth2/OIDC protocols
- Integration with existing identity providers
- Platform-managed infrastructure

### Negative
- Dependency on platform Keycloak
- Complex role mapping
- Token refresh handling in clients

### TastSelv Integration
Citizens authenticate via TastSelv, which provides its own OIDC flow. The citizen-portal acts as a relying party to TastSelv, then exchanges tokens for internal Keycloak tokens if needed.

## Amendment: TB-043 — keycloak-oauth2-starter (2026-03-30)

The repeated SecurityConfig boilerplate across all API services has been extracted into a
standalone Spring Boot auto-configuration starter:

**Module:** dk.ufst:keycloak-oauth2-starter:1.0

The starter registers KeycloakResourceServerAutoConfiguration via
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports and provides:

- keycloakSecuredFilterChain — active for !local & !dev & !demo: stateless JWT resource server,
  CSRF disabled, actuator + Swagger UI permitted without authentication.
- keycloakPermissiveFilterChain — active for local | dev | demo: permits all requests for
  local development.

Both beans use @ConditionalOnMissingBean so individual services can override.
Portal modules (creditor-portal, caseworker-portal, citizen-portal) are **not** affected —
they use the OAuth2 client (browser SSO) pattern and retain their own SecurityConfig.
