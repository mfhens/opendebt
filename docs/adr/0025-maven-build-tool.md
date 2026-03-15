# ADR 0025: Maven Build Tool

## Status
Accepted

## Context
OpenDebt is a multi-module Java/Spring Boot system (ADR-0003) with 13 microservices built on a shared parent POM. The project needs a build tool that supports:

- Multi-module builds with dependency management across services
- Integration with UFST CI/CD pipelines on the Horizontale Driftsplatform
- Reproducible builds for long-lived public-sector software (10+ year lifecycle)
- Plugin ecosystem for quality tooling: Spotless, JaCoCo, OWASP Dependency Check, SonarQube
- Familiarity for the existing Java developer talent pool at UFST

UFST has standardized on Maven for all Java projects running on the Horizontale Driftsplatform. Existing CI templates, security scanning pipelines, and operational tooling assume Maven conventions (pom.xml, `mvn verify`, standard lifecycle phases).

## Decision
We use **Apache Maven** as the build tool for all OpenDebt modules.

### Build structure
- A root `pom.xml` manages shared dependency versions, plugin configuration, and module declarations.
- Each microservice has its own `pom.xml` inheriting from the root.
- `opendebt-common` is a shared module providing base classes, DTOs, and test utilities consumed by all services.

### Key plugins
| Plugin | Purpose |
|--------|---------|
| `spring-boot-maven-plugin` | Executable JAR packaging |
| `spotless-maven-plugin` | Code formatting (Google Java Style) |
| `jacoco-maven-plugin` | Coverage enforcement (80% line, 70% branch) |
| `maven-surefire-plugin` | Unit tests |
| `maven-failsafe-plugin` | Integration tests |
| `dependency-check-maven-plugin` | OWASP vulnerability scanning |
| `sonar-maven-plugin` | Static analysis |

### Standard workflow
```bash
mvn spotless:apply    # Fix formatting
mvn verify            # Compile + unit tests + integration tests + coverage
```

## Consequences

### Positive
- **Organizational alignment.** All UFST Java projects use Maven; CI templates, artifact repositories, and operational playbooks work without adaptation.
- **Reproducibility.** Maven's declarative POM model and strict lifecycle phases produce deterministic builds. The Maven Wrapper (`mvnw`) pins the Maven version per repository.
- **Plugin maturity.** Every quality gate plugin (Spotless, JaCoCo, OWASP, SonarQube) has first-class Maven support with well-documented configuration.
- **Talent pool.** Maven is the dominant build tool in Danish public-sector Java shops; onboarding is immediate.

### Negative
- **Verbosity.** POM XML is more verbose than Gradle's Kotlin/Groovy DSL, particularly for multi-module dependency management.
- **Build speed.** Maven's sequential default lifecycle is slower than Gradle's incremental/cached builds. Mitigated by `-T 1C` (parallel threads) and CI caching of `~/.m2/repository`.
- **Custom build logic.** Extending the build beyond standard plugins requires writing a Maven plugin (Java) rather than inline scripting (Gradle). This has not been needed so far.

### Alternatives Considered

| Alternative | Reason for rejection |
|-------------|---------------------|
| **Gradle (Kotlin DSL)** | Faster incremental builds and more concise configuration. However, UFST CI templates and operational tooling assume Maven. Gradle's flexibility can lead to non-standard build scripts that are harder to audit over a 10+ year lifecycle. Smaller talent pool for Gradle in Danish public sector. |
| **Gradle (Groovy DSL)** | Same organizational misalignment as Kotlin DSL, with additional concern that Groovy DSL is being phased out in favor of Kotlin DSL by the Gradle project. |
| **Bazel** | Excellent for monorepos with mixed languages, but massive overhead for a pure Java/Spring Boot project. No UFST operational support. |

## References
- ADR-0003: Java and Spring Boot Technology Stack
