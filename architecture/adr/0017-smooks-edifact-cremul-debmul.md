# ADR 0017: Smooks for CREMUL/DEBMUL EDIFACT Integration

## Status
Accepted

## Context
OpenDebt must integrate with Statens Koncernbetalinger (SKB) for payment processing. SKB uses UN/EDIFACT message types:

- **CREMUL** (Multiple Credit Advice) - notifications of incoming payments (credits)
- **DEBMUL** (Multiple Debit Advice) - notifications of outgoing payments (debits)

These are standard EDIFACT messages used by Danish public sector institutions for bank reconciliation. The integration requires:

- Parsing incoming CREMUL files to register received payments
- Generating outgoing DEBMUL files to initiate payments (e.g., refunds)
- Mapping EDIFACT segments to Java domain objects
- Supporting multiple EDIFACT directory versions (e.g., D93A, D03B)
- Audit trail of all financial messages processed
- Reliable error handling for malformed messages

There is no Danish open source component specifically for SKB integration. We need a general-purpose EDIFACT library to build the adapter layer.

## Decision
We adopt **Smooks EDI Cartridge** for parsing and generating CREMUL/DEBMUL EDIFACT messages.

### Architecture
```
Statens Koncernbetalinger (SKB)
            │
     CREMUL / DEBMUL files
            │
            ▼
┌──────────────────────────────────────────────┐
│         INTEGRATION GATEWAY                   │
│              (Port 8090)                      │
├──────────────────────────────────────────────┤
│  ┌────────────────────────────────────────┐  │
│  │        Smooks EDI Cartridge            │  │
│  │                                        │  │
│  │  CREMUL → Java Beans (CreditAdvice)    │  │
│  │  Java Beans (DebitAdvice) → DEBMUL     │  │
│  │                                        │  │
│  │  UN/EDIFACT Mapping Models             │  │
│  │  (d93a, d03b, etc.)                    │  │
│  └────────────────────────────────────────┘  │
│                     │                         │
│         SKB Adapter Service                   │
│    (file polling, validation, routing)        │
└──────────────────────────────────────────────┘
            │                    │
            ▼                    ▼
    payment-service        debt-service
  (register payments)    (update debt status)
```

### Dependencies
```xml
<!-- Smooks Core -->
<dependency>
    <groupId>org.smooks</groupId>
    <artifactId>smooks-core</artifactId>
</dependency>

<!-- Smooks EDI Cartridge -->
<dependency>
    <groupId>org.smooks.cartridges.edi</groupId>
    <artifactId>smooks-edi-cartridge</artifactId>
</dependency>

<!-- UN/EDIFACT mapping models (version as needed) -->
<dependency>
    <groupId>org.smooks.cartridges.edi</groupId>
    <artifactId>edifact-schemas</artifactId>
</dependency>
```

### Message Flow

| Direction | Message | Flow |
|-----------|---------|------|
| Inbound | CREMUL | SKB → file drop → integration-gateway parses → payment-service registers payment → debt-service updates balance |
| Outbound | DEBMUL | payment-service requests refund → integration-gateway generates DEBMUL → file drop → SKB |

### Processing Pattern
```java
@Service
public class CremulProcessor {

    private final Smooks smooks;

    public List<CreditAdvice> parseCremul(InputStream cremulStream) {
        // Smooks maps EDIFACT segments directly to Java beans
        JavaResult result = new JavaResult();
        smooks.filterSource(new StreamSource(cremulStream), result);
        return result.getBean(List.class);
    }
}
```

## Consequences

### Positive
- **Mature**: Smooks is battle-tested, used in Red Hat Fuse / JBoss enterprise integration
- **EDIFACT-native**: Built-in UN/EDIFACT mapping models for direct segment-to-Java mapping
- **No intermediate XML**: Can map EDIFACT directly to Java beans without XML transformation step
- **Extensible**: Custom mapping configurations for SKB-specific message variants
- **Apache Camel integration**: Can combine with Camel for file polling, routing, and error handling
- **Open source**: Apache 2.0 license, no vendor lock-in

### Negative
- **Learning curve**: Smooks configuration and EDIFACT mapping models require training
- **EDIFACT complexity**: CREMUL/DEBMUL segments are inherently complex
- **No SKB-specific adapter**: Must build the SKB file exchange layer ourselves (file polling, naming conventions, acknowledgements)
- **Version management**: Must maintain correct EDIFACT directory version matching SKB requirements

### Mitigations
- Create reusable SKB adapter component within integration-gateway
- Comprehensive unit tests with sample CREMUL/DEBMUL files from SKB documentation
- Validate all parsed messages against expected schema before processing
- Log all inbound/outbound EDIFACT messages for audit (without PII - use person_id references)

## Alternatives Considered

| Option | Reason Not Chosen |
|--------|-------------------|
| StAEDI | Good streaming parser but lacks built-in EDIFACT mapping models. Would require more manual mapping code. Viable as lightweight alternative. |
| java-edilib | Less actively maintained, smaller community |
| mescedia-core | Less focused, broader EDI platform with more complexity than needed |
| Custom parser | High development cost, error-prone for complex EDIFACT grammar |
| Proprietary EDI gateway | Against open source principle, vendor lock-in |
