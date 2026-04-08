# ADR 0004: API-First Design with OpenAPI

## Status
Accepted

## Context
OpenDebt must integrate with multiple systems:
- DUPLA (UFST's data exchange platform)
- TastSelv (citizen self-service)
- Digital Post / e-Boks
- CPR/CVR registers
- Various creditor systems (1,200+ institutions)

The Fællesoffentlige Arkitekturprincipper mandate:
- Clear API contracts
- Interoperability
- Documentation
- Versioning strategy

DUPLA specifically requires:
- OpenAPI specifications
- Standard error formats
- Audit logging via CLS (Central Logging Service)

## Decision
We adopt an API-first design approach:

1. **OpenAPI 3.1 Specifications**: All APIs are defined in OpenAPI specs before implementation
2. **Contract Location**: Specs stored in `/api-specs/` directory
3. **Code Generation**: Consider generating server stubs and client SDKs
4. **Versioning**: URL-based versioning (`/api/v1/`, `/api/v2/`)
5. **Error Format**: Standardized error response schema
6. **HATEOAS**: Consider for discoverability (optional)

### API Design Principles
- RESTful resource-oriented design
- Consistent naming (kebab-case for URLs, camelCase for JSON)
- Pagination for list endpoints
- Idempotency keys for mutations
- Correlation IDs for tracing

### Security
- JWT Bearer tokens from Keycloak
- OCES3 certificates for system-to-system
- Scopes for fine-grained authorization

## Consequences

### Positive
- Clear contracts enable parallel development
- Easy integration for external systems
- Automatic documentation generation
- Contract testing possible
- DUPLA compliance

### Negative
- Upfront design effort required
- Spec/implementation drift risk
- Additional tooling needed

### Mitigations
- CI/CD validation of spec vs implementation
- SpringDoc generates spec from code as backup
- Regular spec reviews in pull requests
