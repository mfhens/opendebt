# ADR 0003: Java and Spring Boot Technology Stack

## Status
Accepted

## Context
UFST (Udviklings- og Forenklingsstyrelsen) has established technology preferences for systems running on the Horizontale Driftsplatform. The organization has extensive experience with Java and Spring Boot, and existing systems use this stack.

Key requirements:
- Kubernetes-native deployment
- OAuth2/OIDC integration with Keycloak
- OpenAPI-first API development
- Integration with DUPLA API gateway
- Long-term maintainability (10+ year system lifecycle)

## Decision
We use the following technology stack:

### Runtime
- **Java 21 LTS** - Latest long-term support version
- **Spring Boot 3.3** - Latest stable Spring Boot
- **Spring Security** - OAuth2 Resource Server for Keycloak integration

### Build & Quality
- **Maven** - Standard build tool at UFST
- **Spotless** - Code formatting (Google Java Style)
- **JaCoCo** - Code coverage (80% line, 70% branch minimum)
- **ArchUnit** - Architecture testing
- **OWASP Dependency Check** - Security vulnerability scanning
- **SonarQube** - Static code analysis

### API
- **SpringDoc OpenAPI** - OpenAPI 3.1 specification generation
- **MapStruct** - DTO mapping
- **Jakarta Validation** - Input validation

### Observability
- **Micrometer** - Metrics
- **Prometheus** - Metrics collection
- **Spring Boot Actuator** - Health checks and management

## Consequences

### Positive
- Aligned with UFST technology standards
- Large talent pool for Java developers in Denmark
- Mature ecosystem with extensive documentation
- Strong security features out of the box
- Well-supported by the Horizontale Driftsplatform

### Negative
- Larger container images compared to Go/Rust
- Slower startup time (mitigated by keeping services running)
- Memory overhead of JVM

### Alternatives Considered
- **Kotlin**: Excellent but smaller talent pool in public sector
- **Go**: Fast but less ecosystem support at UFST
- **Node.js**: Good for portals but not for backend services at UFST
