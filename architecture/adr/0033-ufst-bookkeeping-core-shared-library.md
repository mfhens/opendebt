# ADR 0033: Extract Bookkeeping Core as Shared UFST Library

## Status
Accepted

## Context

OpenDebt (opendebt-payment-service) contains a fully implemented, tested double-entry bookkeeping module (see ADR-0018). It provides:

- Generic double-entry posting engine (debit/credit pairs, bi-temporal dates)
- Immutable ledger entry model with storno (reversal) pattern for corrections
- Period-based interest accrual and retroactive recalculation
- Timeline replay over an event log
- Chart of accounts abstraction (`AccountCode`)

OSM2 (VAT modernisation) is in active development and has the same structural accounting requirements:

- VAT receivables, SKB/NemKonto bank, revenue accounts, write-offs, refunds
- Interest on late payment (morarenter) using the same daily-rate formula
- Retroactive corrections (VAT returns amended up to 3 years back)
- Storno pattern for audit-safe corrections
- Statsligt regnskab audit trail

Allowing both systems to independently implement these mechanics would produce two diverging codebases with shared invariants and shared failure modes. The decision has been made at UFST level to converge on a single shared library.

## Decision

We extract the double-entry bookkeeping core into a shared Maven library:

```
groupId:    dk.ufst
artifactId: ufst-bookkeeping-core
```

### Initial home

The module lives inside the OpenDebt repository (`ufst-bookkeeping-core/`) as a sibling Maven module until a dedicated UFST platform repository is established. At that point it is extracted with full Git history via `git subtree` or `git filter-repo`.

### What goes into the library

| Component | Included | Rationale |
|---|---|---|
| Double-entry posting engine | ✅ | Generic — no business-domain assumptions |
| `InterestAccrualService` | ✅ | Pure calculation logic, no JPA |
| `TimelineReplayService` | ✅ | Generic event-log replay |
| `RetroactiveCorrectionService` | ✅ | Generic storno + recalculation orchestration |
| `AccountCode` / `AccountType` | ✅ as interface/SPI | Each system provides its own kontoplan implementation |
| `InterestPeriod`, `CorrectionResult` models | ✅ | Value objects, no persistence |
| `LedgerEntryEntity`, `DebtEventEntity` | ❌ | JPA entities are persistence-layer — stay in each system |
| `LedgerEntryRepository`, `DebtEventRepository` | ❌ | Spring Data — stay in each system |
| `ImmuLedgerAppender` wiring | ❌ | Deployment concern — each system decides tamper-evidence strategy |
| `AllocationNotificationService` | ❌ | OpenDebt-specific payment allocation domain |
| `CoveragePriorityService` | ❌ | OpenDebt-specific dækningsrækkefølge logic |
| `CrossingTransactionDetector` | ❌ | OpenDebt-specific crossing detection |

### Account code SPI

Each consuming system provides its own kontoplan by implementing a simple interface:

```java
public interface AccountCodeProvider<T extends Enum<T>> {
    String getCode(T account);
    String getName(T account);
    AccountType getType(T account);
}
```

OpenDebt passes its `AccountCode` enum; OSM2 passes its own VAT-specific enum. The core engine is agnostic.

### Persistence port

The library defines repository interfaces (ports) that each system implements with its own JPA adapters:

```java
// In ufst-bookkeeping-core (port — no JPA dependency)
public interface LedgerEntryStore {
    void save(LedgerEntry debitEntry, LedgerEntry creditEntry);
}

public interface DebtEventStore {
    void save(FinancialEvent event);
    List<FinancialEvent> findPrincipalAffectingEvents(UUID subjectId);
}
```

### Maven coordinates

```xml
<dependency>
    <groupId>dk.ufst</groupId>
    <artifactId>ufst-bookkeeping-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

The library targets Java 21 with no Spring dependency. Spring `@Service` / `@Transactional` wiring stays in each consuming system's adapter layer.

## Consequences

### Positive
- Single implementation of double-entry invariants, storno, and interest math shared across OpenDebt and OSM2
- OSM2 inherits OpenDebt's tested implementation rather than reimplementing from scratch
- No third-party bookkeeping library dependency — fully owned by UFST
- Extractable to a standalone repo when UFST platform infrastructure is ready (no code change required, only build relocation)
- Java 21, no Spring coupling — usable in any JVM-based UFST system

### Negative
- OpenDebt repo temporarily owns a cross-programme artifact — requires discipline not to introduce OpenDebt-specific logic into the shared module
- Release versioning must be coordinated between OpenDebt and OSM2 teams
- Requires OSM2 to refactor its persistence layer to implement the port interfaces

### Mitigations
- Module boundary enforced by Maven: `ufst-bookkeeping-core` has zero dependency on `opendebt-*` modules
- A clear `CODEOWNERS` or ownership annotation documents that this module is UFST-level, not OpenDebt-level
- Semantic versioning with a stable `1.x` API; breaking changes require major version bump with migration notes

## Alternatives Considered

| Option | Reason Not Chosen |
|---|---|
| Each system implements its own bookkeeping | Divergence guaranteed; same bugs fixed twice; storno/interest logic is subtle |
| Use Apache Fineract accounting module | Full banking platform; far exceeds scope; heavy dependency |
| Wait for UFST platform repo before extracting | OSM2 development is underway now; delay means OSM2 builds its own before the library exists |
| `com.yanimetaxas:bookkeeping` | Only version 0.1.0 exists; unmaintained for 9 years (see ADR-0018 amendment) |

## Related ADRs

- ADR-0018: Double-Entry Bookkeeping — original decision; implementation in `opendebt-payment-service` is the seed for this library
- ADR-0029: immudb for Financial Ledger Integrity — tamper-evidence layer; remains a per-system deployment concern, not part of the shared library
