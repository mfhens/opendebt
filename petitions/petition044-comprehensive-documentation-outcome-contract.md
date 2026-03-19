# Petition 044 Outcome Contract

## Acceptance criteria

1. An MkDocs configuration (`mkdocs.yml`) exists at the repository root and `mkdocs build` produces a static site without errors.
2. The documentation site has four top-level sections: Technical (English), Fordringshaver (Danish), Skyldner (Danish), Sagsbehandler (Danish).
3. The developer onboarding guide enables a new contributor to clone, build, and run tests within 30 minutes.
4. The fordringshaver-guide covers claim lifecycle, notifications, and reconciliation in Danish.
5. The skyldner-guide covers MitID login, debt overview, and payment options in Danish.
6. The sagsbehandler-guide covers case management, collection measures, and objection handling in Danish.
7. The API reference links to or embeds OpenAPI specs for each service.
8. The domain model reference includes the begrebsmodel terminology table and entity relationship diagrams.
9. All Danish documentation uses consistent domain terminology from the begrebsmodel.

## Definition of done

- `mkdocs serve` renders the full documentation site locally.
- Each audience section has at least the pages listed in the functional requirements.
- Danish content is reviewed for correct domain terminology.
- Technical content is consistent with existing architecture docs and ADRs.

## Failure conditions

- `mkdocs build` fails or produces broken links.
- Danish user documentation uses English domain terms instead of begrebsmodel Danish terms.
- Technical documentation contradicts existing ADRs or architecture overview.
- Documentation site is not navigable by audience section.
