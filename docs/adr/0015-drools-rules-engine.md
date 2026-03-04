# ADR 0015: Drools for Business Rules Engine

## Status
Accepted

## Context
Danish debt collection involves complex business rules including:

- **Readiness validation** (indrivelsesparathed) - 20+ criteria to determine if debt can be collected
- **Interest calculation** - Different rates based on debt type, creditor, legal basis
- **Collection priority** - Legal priority order for offsetting (boernebidrag > skat > boeder)
- **Thresholds and limits** - Protected income amounts, garnishment limits

Requirements:
- Rules must be maintainable by business analysts (not just developers)
- Rules change frequently based on legislation changes
- Audit trail of which rules were applied
- Support for decision tables (Excel-based rules)

## Decision
We adopt **Drools 9.x** as the business rules engine.

### Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    RULES ENGINE SERVICE                      │
│                       (Port 8091)                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  Readiness  │  │  Interest   │  │  Priority   │         │
│  │   Rules     │  │   Rules     │  │   Rules     │         │
│  │  (.drl)     │  │  (.xlsx)    │  │  (.drl)     │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│                         │                                   │
│                    Drools Engine                            │
│                    (KieContainer)                           │
└─────────────────────────────────────────────────────────────┘
                          │
            ┌─────────────┴─────────────┐
            │                           │
     debt-service              case-service
   (calls rules API)         (calls rules API)
```

### Rule Types

| Rule Category | Format | Maintainer |
|---------------|--------|------------|
| Debt readiness | DRL | Developers |
| Interest rates | Excel decision table | Business analysts |
| Collection priority | DRL | Developers |
| Thresholds | Excel decision table | Business analysts |

### Example Rule (DRL)
```drools
rule "Child Support Priority"
    when
        $request : CollectionPriorityRequest(isChildSupport == true)
    then
        result.setPriorityRank(1);
        result.setLegalBasis("Inddrivelsesloven § 10");
end
```

### Example Decision Table (Excel)
| Debt Type | Days Past Due | Interest Rate | Legal Basis |
|-----------|---------------|---------------|-------------|
| TAX | 0-30 | 0% | Grace period |
| TAX | 31+ | 10% | Renteloven § 5 |
| FINE | 0+ | 8% | Standard rate |

### API
```http
POST /rules-engine/api/v1/rules/readiness/evaluate
POST /rules-engine/api/v1/rules/interest/calculate
POST /rules-engine/api/v1/rules/priority/evaluate
```

## Consequences

### Positive
- **Business-friendly**: Excel decision tables for non-technical users
- **Auditable**: Log which rules fired for each evaluation
- **Flexible**: Rules can be changed without code deployment
- **Mature**: Drools is battle-tested in financial services
- **DMN support**: Decision Model and Notation for complex decisions

### Negative
- **Learning curve**: DRL syntax requires training
- **Performance**: Rule evaluation has overhead vs. hardcoded logic
- **Complexity**: KIE infrastructure adds operational complexity

### Mitigations
- Provide templates for common rule patterns
- Cache KieContainer for performance
- Unit test all rules
- Version control rule files

## Alternatives Considered

| Option | Reason Not Chosen |
|--------|-------------------|
| Easy Rules | Too simple, no decision tables |
| OpenL Tablets | Less Spring Boot integration |
| Custom rules in DB | No standard tooling, harder to maintain |
