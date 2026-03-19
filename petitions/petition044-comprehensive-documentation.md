# Petition 044: Comprehensive technical and user documentation

## Summary

OpenDebt shall maintain a comprehensive documentation bundle consisting of English-language technical documentation (architecture, API, developer onboarding) and Danish-language user documentation for all three portal audiences: fordringshavere (creditors), skyldnere (citizens), and sagsbehandlere (caseworkers). Documentation shall be authored as Markdown in the repository and buildable as a static documentation site using MkDocs.

## Context and motivation

The project has foundational technical documentation (architecture overview, ADRs, begrebsmodel, AGENTS.md) but lacks:

- **User-facing documentation in Danish** for the three portal audiences
- **Consolidated technical documentation** beyond scattered ADRs and the architecture overview
- **Developer onboarding guide** for new contributors
- **API documentation** beyond auto-generated Swagger/OpenAPI
- **Operations guide** for deployment, monitoring, and incident response
- **A buildable documentation site** that makes all documentation discoverable and navigable

Danish public sector projects are expected to provide clear, accessible user documentation in Danish. Without it, fordringshavere cannot self-serve through the portal, citizens cannot understand their rights and options, and caseworkers lack procedural reference material.

## Functional requirements

### Technical documentation (English)

1. OpenDebt shall maintain an **architecture guide** consolidating the existing architecture overview, service inventory, data flow diagrams (Mermaid), and deployment topology into a navigable structure.
2. OpenDebt shall maintain an **ADR index** with summaries linking to individual ADR files.
3. OpenDebt shall maintain an **API reference** for each service, generated from or linking to OpenAPI specs, with usage examples.
4. OpenDebt shall maintain a **developer onboarding guide** covering: repository structure, build instructions (`mvn verify`), local development setup (Docker Compose, Keycloak, PostgreSQL), coding conventions, test patterns, and contribution workflow.
5. OpenDebt shall maintain a **domain model reference** summarizing the begrebsmodel with the canonical Danish-to-English terminology mapping table and entity relationship diagrams.
6. OpenDebt shall maintain an **operations guide** covering: Kubernetes deployment, configuration management, monitoring (Grafana/Prometheus/Loki), alerting, log analysis, and incident response procedures.

### User documentation -- Fordringshavere (Danish)

7. OpenDebt shall provide a **fordringshaver-guide** in Danish covering:
   - Hvad er OpenDebt og Fordringshaverportalen
   - Oprettelse og indsendelse af fordringer (claim creation and submission)
   - Fordringens livscyklus (claim lifecycle states and transitions)
   - Regulering af fordringer (opskrivning, nedskrivning, tilbagekald)
   - Underretningsmeddelelser (notifications: afregning, udligning, allokering, renter, afskrivning, tilbagesend)
   - Høring og indsigelse (hearing and objection)
   - Afstemning og rapporter (reconciliation and reports)
   - Ofte stillede spørgsmål (FAQ)
8. OpenDebt shall provide a **M2M-integrationsvejledning** in Danish for creditor systems integrating via SOAP or REST, covering: authentication (OCES3/OAuth2), endpoint catalogue, request/response examples, error handling, and test environment setup.

### User documentation -- Skyldnere (Danish)

9. OpenDebt shall provide a **skyldner-guide** in Danish covering:
   - Hvad er OpenDebt og Skyldnerportalen
   - Login med MitID
   - Mit gældsoverblik (my debt overview)
   - Betalingsmuligheder (payment options)
   - Indsigelse og klage (objection and appeal)
   - Renteregler og dækningsrækkefølge (interest rules and payment priority)
   - Kontaktoplysninger og hjælp (contact information and help)
   - Tilgængelighedserklæring (accessibility statement)

### User documentation -- Sagsbehandlere (Danish)

10. OpenDebt shall provide a **sagsbehandler-guide** in Danish covering:
    - Sagsoversigt og sagstildeling (case overview and assignment)
    - Fordringsoversigt og detaljer (claim overview and details)
    - Inddrivelsesskridt (collection measures: modregning, lønindeholdelse, udlæg)
    - Hæftelse og skyldnerforhold (liability and debtor relationships)
    - Indsigelseshåndtering (objection handling)
    - Bogføring og tidslinje (bookkeeping and timeline)
    - Underretninger og kommunikation (notifications and communication)

### Documentation site

11. OpenDebt shall include an MkDocs configuration (`mkdocs.yml`) that builds all documentation into a navigable static site.
12. The site shall support both Danish and English content, organized by audience (technical / fordringshaver / skyldner / sagsbehandler).
13. The site shall use the Material for MkDocs theme for readability and search support.
14. The documentation site shall be buildable with `mkdocs build` and servable locally with `mkdocs serve`.

## Constraints and assumptions

- All user documentation is written in Danish. Technical documentation is written in English.
- Documentation references the begrebsmodel for domain terminology.
- Documentation does not duplicate existing ADR content but links to it.
- Screenshots and UI examples should reference the actual portal templates but are not required in the initial version.
- Documentation is maintained alongside code -- when features change, documentation should be updated.
- MkDocs is chosen for its simplicity, Markdown-native approach, and Material theme ecosystem.

## Out of scope

- Automated documentation generation from source code (Javadoc)
- Video tutorials or interactive walkthroughs
- Printed documentation or PDF generation (can be added to MkDocs later)
- Translation of user documentation into English or other languages
- Documentation CI/CD pipeline (can be added separately)

## Dependencies

- No hard dependencies on other petitions. Documentation can be written for implemented features progressively.
- Content accuracy depends on the implemented state of each feature.
