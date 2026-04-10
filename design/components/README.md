# Component Definitions

Each YAML file in this directory defines one OpenDebt component.
The `component-assigner` agent reads these files when mapping petition scenarios
to responsible components. The `pipeline-conductor` uses the `tags` field at
Phase 2.7 (NFR Alignment) to determine which NFRs from `compliance/nfr-register.yaml`
apply to a given petition.

## Schema

```yaml
id: <kebab-case-id>           # REQUIRED — stable machine identifier
name: "<Human Readable Name>" # REQUIRED
description: >                # REQUIRED — one paragraph, what it does
  ...
type: service|portal|library|gateway|infrastructure
module: <maven-module-name>   # Maven artifact ID, or ~ for non-Maven components
tags:                         # REQUIRED — must align with applies_to.tags in nfr-register.yaml
  - service                   # backend microservice with REST API
  - portal                    # web UI (Thymeleaf/HTMX)
  - citizen                   # citizen-facing portal
  - financial                 # handles money, bookkeeping, or payments
  - legal                     # implements statutory calculations or decisions
  - gateway                   # legacy integration / SOAP adapter
  - library                   # shared library, no standalone deployment
  - rules-engine              # contains or loads Drools .drl rules
api_spec: <path-to-openapi>   # relative path from repo root, or ~
owner_team: ~                 # team name or ~
```

## Tags used in `compliance/nfr-register.yaml`

| Tag | NFRs triggered |
|-----|---------------|
| `service` | GDPR-002, GDPR-005, RES-001, RES-002, RES-003, OBS-002, OBS-003, ARCH-001, ARCH-003 |
| `portal` | ACC-001, ACC-002 |
| `citizen` | ACC-001 |
| `financial` | AUDIT-002, AUDIT-003 |
| `legal` | ARCH-004 |
