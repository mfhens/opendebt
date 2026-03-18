# Petition 042: Case data model based on the fællesoffentlige sagsdefinition (OIO Sag v2.0)

## Summary

The `opendebt-case-service` data model shall be redesigned to align with the fællesoffentlige sagsdefinition as defined in "OIO Specifikation af Model for Sag Version 2.0" (arkitektur.digst.dk), specialized for the debt collection domain (inddrivelse). The current CaseEntity is a flat placeholder lacking the structural richness required for compliant public-sector case management: sagsparter (case parties with roles), sagsjournal (journal with JournalPost and JournalNotat), sagshjemmel (legal basis), sagsklassifikation (KLE and domain-specific classification), sagshændelser (case events/audit trail), sagsrelationer (inter-case references), and the proper tilstandsmodel (state machine). This petition brings case-service into compliance with both OIO Sag and the OpenDebt begrebsmodel v3 (section 2, "Sag" as process carrier for inddrivelsessag).

## Context and motivation

### Current state

`CaseEntity` today is a minimal flat table:

| Field | Type | Purpose |
|-------|------|---------|
| id | UUID | Technical PK |
| caseNumber | String | SAG-YYYY-NNNNN |
| debtorPersonId | UUID | Single debtor reference |
| status | Enum (10 values) | Flat status without transition rules |
| totalDebt/totalPaid/totalRemaining | BigDecimal | Denormalized balance (stale on read) |
| debtIds | List\<UUID\> | Simple join table to debt-service |
| activeStrategy | Enum (5 values) | Single collection strategy |
| assignedCaseworkerId | String | Single caseworker |
| notes | Text | Unstructured free text |
| lastActivityAt | Timestamp | Last update (no audit trail) |

This model has critical gaps:

1. **No sagsparter (case parties)** — Only a single `debtorPersonId`. Cannot represent multiple debtors, co-liable parties (solidarisk hæftelse), creditors, or secondary parties like legal representatives. OIO Sag requires Sagspart with primary/secondary roles.

2. **No sagsjournal** — No JournalPost or JournalNotat. No structured way to attach documents, decisions, hearing responses, letters, or caseworker notes to the case. OIO Sag requires JournalPost as the document-to-case binding with its own metadata.

3. **No sagshjemmel (legal basis)** — No reference to the legal basis for the collection case (GIL, Inddrivelsesloven, specific paragraphs). OIO Sag requires Sagshjemmel with ParagrafHenvisning.

4. **No sagsklassifikation** — No KLE classification, no domain-specific classification (hearing, appeal, voluntary payment). OIO Sag requires Emneklasse, Handlingsklasse, and domain facets.

5. **No sagshændelser (case events)** — No structured event log. `lastActivityAt` is a single timestamp. Cannot reconstruct case history, which is required for audit, Statsrevisorerne, and Ombudsmanden compliance.

6. **No proper tilstandsmodel** — The status enum mixes lifecycle states (OPEN, IN_PROGRESS, CLOSED_*) with operational states (AWAITING_PAYMENT, WAGE_GARNISHMENT_ACTIVE). OIO Sag defines SagsTilstand as a proper state machine: Opstået → Oplyst → Afgjort → Afsluttet.

7. **No sagsrelationer** — No way to link related cases (oversag, anden sag, præcedens). Debt collection often involves related cases for the same debtor or claim complex.

8. **No CaseServiceImpl** — The service interface exists but had no implementation (just created a stub in the previous session to make it start).

9. **Denormalized balances** — `totalDebt/totalPaid/totalRemaining` are stored on the case but never updated. They should be computed from debt-service and payment-service on demand, not stored.

### The fællesoffentlige sagsdefinition (OIO Sag v2.0)

The OIO specification defines a case as "en samling af sammenhørende dokumenter og øvrige sammenhørende oplysninger, der i sit hele anvendes til at dokumentere en arbejdsproces, typisk til administrative formål, herunder til at træffe afgørelser."

Core model elements:

| OIO Concept | OpenDebt Mapping | Description |
|-------------|-----------------|-------------|
| Sag | CaseEntity | The central case object |
| Sagspart | CasePartyEntity | Party-to-case relationship with role |
| Sagsnummer | caseNumber | Unique human-readable identifier |
| Titel | title | Public case title |
| SagsTilstand | caseState | State machine (CREATED → ASSESSED → DECIDED → CLOSED) |
| TilstandsDato | stateChangedAt | Date of last state transition |
| JournalPost | CaseJournalEntryEntity | Document-to-case binding with metadata |
| JournalNotat | CaseJournalNoteEntity | Internal caseworker notes (lovpligtigt notat) |
| Sagshjemmel | CaseLegalBasisEntity | Legal basis reference with paragraph |
| Emneklasse | subjectClassification | KLE or domain classification |
| Handlingsklasse | actionClassification | Action facet |
| Oversag | parentCaseId | Parent case reference |
| AndenSag | relatedCases | Related case references |
| Præcedens | precedentCaseId | Precedent case reference |
| Ejer | ownerOrganisationId | Owning organisation |
| Ansvarlig | responsibleUnitId | Responsible org unit |
| PrimærBehandler | primaryCaseworkerId | Primary caseworker |
| AndreBehandlere | additionalCaseworkerIds | Additional caseworkers |
| OprettetAf | createdBy | Creator identity |
| Principielindikator | precedentIndicator | Whether this is a precedent case |
| Kassationsindikator | retentionOverride | Archive retention override |
| OffentlighedUndtagetAlternativTitel | confidentialTitle | Confidential alternative title |

### Specialization for debt collection (inddrivelse)

Beyond the generic OIO model, the case-service data model must support debt-collection-specific concepts from the begrebsmodel v3:

| Domain concept | Entity/field | Description |
|---------------|-------------|-------------|
| Inddrivelsessag | CaseEntity (specialized) | A case carrying one or more overdue claims (restancer) for inddrivelse |
| Inddrivelsesskridt | CollectionMeasureEntity | Active collection measures (modregning, lønindeholdelse, udlæg, afdragsordning) linked to case |
| Indsigelse | ObjectionEntity | Debtor objections linked to case and specific debts |
| Overdragelse | TransferRecord (event) | Record of when debts were transferred for collection |
| Dækningsrækkefølge audit | Via payment-service posteringslog | Linked from case journal |
| Workflow state | Flowable BPMN process variables | Current BPMN activity maps to case state |

## Functional requirements

### FR-1: CaseEntity redesign

Replace the current flat CaseEntity with a model aligned to OIO Sag v2.0:

```
CaseEntity
├── id: UUID (PK)
├── caseNumber: String (unique, format: SAG-YYYY-NNNNN)
├── title: String (obligatorisk per OIO Sag)
├── description: String (optional)
├── confidentialTitle: String (optional, for offentlighedsundtaget)
├── caseState: CaseState enum (CREATED, ASSESSED, DECIDED, CLOSED)
├── stateChangedAt: LocalDateTime
├── caseType: CaseType enum (DEBT_COLLECTION, HEARING, APPEAL, OBJECTION)
├── subjectClassification: String (KLE code, e.g. 15.20.04)
├── actionClassification: String (action facet)
├── precedentIndicator: boolean (default false)
├── retentionOverride: Boolean (nullable, kassationsindikator)
├── ownerOrganisationId: String (ejer — Gældsstyrelsen org ID)
├── responsibleUnitId: String (ansvarlig OrgEnhed)
├── primaryCaseworkerId: String (primær behandler)
├── parentCaseId: UUID (nullable, oversag reference)
├── workflowProcessInstanceId: String (Flowable reference)
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
├── version: Long (@Version)
└── sysperiod: tstzrange (temporal history)
```

Remove denormalized balance fields (`totalDebt`, `totalPaid`, `totalRemaining`). These shall be computed on demand by aggregating from debt-service and payment-service via API calls.

### FR-2: CasePartyEntity (Sagspart)

A new entity representing the relationship between a case and a party (person or organisation):

```
CasePartyEntity
├── id: UUID (PK)
├── caseId: UUID (FK → cases)
├── personId: UUID (reference to person-registry)
├── partyRole: PartyRole enum
│     PRIMARY_DEBTOR      — skyldner (hovedskyldner)
│     CO_DEBTOR           — medhæfter (solidarisk hæftelse)
│     CREDITOR            — fordringshaver
│     LEGAL_REPRESENTATIVE — fuldmagtshaver
│     CONTACT_PERSON      — kontaktperson
│     GUARANTOR           — kautionist
├── partyType: PartyType enum (PERSON, ORGANISATION)
├── activeFrom: LocalDate
├── activeTo: LocalDate (nullable — null means still active)
├── addedBy: String
├── createdAt: LocalDateTime
```

This replaces the single `debtorPersonId` field and enables:
- Multiple debtors (solidarisk hæftelse, begrebsmodel v3 §4.38)
- Creditor references (fordringshaver linked to case)
- Legal representatives and contact persons

### FR-3: CaseDebtEntity (Sag-Fordring relation)

Replace the simple `@ElementCollection` join table with a proper entity that tracks when and why debts were added to the case:

```
CaseDebtEntity
├── id: UUID (PK)
├── caseId: UUID (FK → cases)
├── debtId: UUID (reference to debt-service)
├── addedAt: LocalDateTime
├── addedBy: String
├── removedAt: LocalDateTime (nullable)
├── removedBy: String (nullable)
├── transferReference: String (overdragelsesreference)
├── notes: String
```

### FR-4: CaseJournalEntryEntity (JournalPost)

OIO Sag's JournalPost represents the binding between a case and a document:

```
CaseJournalEntryEntity
├── id: UUID (PK)
├── caseId: UUID (FK → cases)
├── journalEntryTitle: String (JournalpostTitel)
├── journalEntryTime: LocalDateTime (JournalpostTid)
├── documentId: UUID (reference to letter-service or external document)
├── documentDirection: DocumentDirection enum
│     INCOMING   — modtaget (from debtor, creditor, court)
│     OUTGOING   — afsendt (to debtor, creditor)
│     INTERNAL   — internt notat
├── documentType: String (letter type, hearing response, court order, etc.)
├── confidentialTitle: String (nullable, offentlighedsundtaget)
├── registeredBy: String
├── createdAt: LocalDateTime
```

### FR-5: CaseJournalNoteEntity (JournalNotat)

Internal caseworker notes that are part of the case but not standalone documents:

```
CaseJournalNoteEntity
├── id: UUID (PK)
├── caseId: UUID (FK → cases)
├── noteTitle: String
├── noteText: String (TEXT)
├── authorId: String (caseworker who wrote the note)
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
```

This replaces the single `notes` TEXT field on CaseEntity.

### FR-6: CaseLegalBasisEntity (Sagshjemmel)

Reference to the legal basis for the collection case:

```
CaseLegalBasisEntity
├── id: UUID (PK)
├── caseId: UUID (FK → cases)
├── legalSourceUri: String (ELI URI, e.g. https://www.retsinformation.dk/eli/lta/...)
├── legalSourceTitle: String (e.g. "Lov om inddrivelse af gæld til det offentlige")
├── paragraphReference: String (e.g. "§ 1, stk. 1")
├── description: String (free text explanation)
├── createdAt: LocalDateTime
```

### FR-7: CaseEventEntity (Sagshændelse)

An immutable event log for the case, replacing the single `lastActivityAt` timestamp:

```
CaseEventEntity
├── id: UUID (PK)
├── caseId: UUID (FK → cases)
├── eventType: CaseEventType enum
│     CASE_CREATED
│     STATE_CHANGED
│     CASEWORKER_ASSIGNED
│     PARTY_ADDED
│     PARTY_REMOVED
│     DEBT_ADDED
│     DEBT_REMOVED
│     JOURNAL_ENTRY_ADDED
│     NOTE_ADDED
│     STRATEGY_CHANGED
│     COLLECTION_MEASURE_INITIATED
│     OBJECTION_RECEIVED
│     HEARING_STARTED
│     HEARING_RESOLVED
│     WORKFLOW_TASK_COMPLETED
│     CASE_CLOSED
├── description: String
├── metadata: String (JSON, e.g. {"fromState": "CREATED", "toState": "ASSESSED"})
├── performedBy: String
├── performedAt: LocalDateTime
```

### FR-8: CaseRelationEntity (Sagsrelation)

References between cases per OIO Sag:

```
CaseRelationEntity
├── id: UUID (PK)
├── sourceCaseId: UUID (FK → cases)
├── targetCaseId: UUID (FK → cases)
├── relationType: CaseRelationType enum
│     PARENT        — oversag
│     RELATED       — anden sag
│     PRECEDENT     — præcedens
│     SPLIT_FROM    — opdelingskilde
│     MERGED_INTO   — sammenlægning
├── description: String
├── createdBy: String
├── createdAt: LocalDateTime
```

### FR-9: CollectionMeasureEntity (Inddrivelsesskridt)

Track active collection measures linked to the case:

```
CollectionMeasureEntity
├── id: UUID (PK)
├── caseId: UUID (FK → cases)
├── debtId: UUID (the specific debt this measure targets)
├── measureType: MeasureType enum
│     VOLUNTARY_PAYMENT
│     PAYMENT_PLAN
│     WAGE_GARNISHMENT
│     OFFSETTING
│     ATTACHMENT
├── status: MeasureStatus enum (PLANNED, ACTIVE, COMPLETED, CANCELLED)
├── startDate: LocalDate
├── endDate: LocalDate (nullable)
├── amount: BigDecimal (nullable, e.g. payment plan amount)
├── reference: String (external reference, e.g. fogedret journal number)
├── notes: String
├── createdBy: String
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
```

This replaces the single `activeStrategy` enum field and supports multiple concurrent collection measures per case.

### FR-10: ObjectionEntity (Indsigelse)

Track debtor objections linked to case and debts:

```
ObjectionEntity
├── id: UUID (PK)
├── caseId: UUID (FK → cases)
├── debtId: UUID (nullable — may apply to whole case)
├── objectionType: ObjectionType enum
│     EXISTENCE     — fordringens eksistens
│     AMOUNT        — fordringens størrelse
│     BASIS         — fordringens grundlag
│     TREATMENT     — fordringens behandling
│     LIMITATION    — forældelse
├── status: ObjectionStatus enum (RECEIVED, UNDER_REVIEW, ACCEPTED, REJECTED)
├── description: String
├── debtorStatement: String (TEXT)
├── caseworkerAssessment: String (TEXT, nullable)
├── receivedAt: LocalDateTime
├── resolvedAt: LocalDateTime (nullable)
├── resolvedBy: String (nullable)
├── createdAt: LocalDateTime
```

### FR-11: Proper state machine (SagsTilstand)

Implement a state machine aligned with OIO Sag, specialized for debt collection:

```
CaseState:
  CREATED       — Opstået: case created, debts transferred, awaiting assessment
  ASSESSED      — Oplyst: case fully assessed, collection strategy determined
  DECIDED       — Afgjort: collection measures decided and active
  SUSPENDED     — Suspenderet: case suspended (appeal, objection with opsættende virkning)
  CLOSED_PAID   — Afsluttet (betalt): all debts fully paid
  CLOSED_WRITTEN_OFF — Afsluttet (afskrevet): debts written off
  CLOSED_WITHDRAWN   — Afsluttet (tilbagekaldt): debts withdrawn by creditor
  CLOSED_CANCELLED   — Afsluttet (annulleret): case cancelled
```

Valid transitions:
```
CREATED → ASSESSED
ASSESSED → DECIDED
ASSESSED → SUSPENDED
DECIDED → SUSPENDED
DECIDED → CLOSED_PAID
DECIDED → CLOSED_WRITTEN_OFF
DECIDED → CLOSED_WITHDRAWN
DECIDED → CLOSED_CANCELLED
SUSPENDED → ASSESSED (after objection resolved)
SUSPENDED → CLOSED_WITHDRAWN
```

State transitions shall be enforced in the service layer and logged as CaseEventEntity entries.

### FR-12: CaseServiceImpl with full CRUD and domain operations

A proper service implementation with:
- `createCase()` — Creates case, adds initial parties and debts, records CASE_CREATED event
- `getCaseById()` / `listCases()` — Read with eager-loaded parties, debts
- `transitionState()` — Enforces valid state transitions, records STATE_CHANGED event
- `addParty()` / `removeParty()` — Manages case parties with events
- `addDebt()` / `removeDebt()` — Manages case debts with events
- `addJournalEntry()` / `addJournalNote()` — Journal management
- `addLegalBasis()` — Legal basis references
- `initiateCollectionMeasure()` — Creates collection measure records
- `receiveObjection()` / `resolveObjection()` — Objection management
- `assignCaseworker()` — Caseworker assignment with event
- `getCaseBalance()` — Computed balance by calling debt-service and payment-service APIs
- `getCaseEvents()` — Returns case event history
- `getCaseTimeline()` — Returns combined timeline (events + journal + measures)

### FR-13: Flyway migration

A new migration (V3) shall:
1. Add new columns to `cases` table (title, caseState, caseType, subjectClassification, etc.)
2. Create new tables: `case_parties`, `case_journal_entries`, `case_journal_notes`, `case_legal_bases`, `case_events`, `case_relations`, `collection_measures`, `objections`
3. Migrate existing data: convert old `status` values to new `caseState`, migrate `debtorPersonId` to a PRIMARY_DEBTOR entry in `case_parties`, migrate `notes` to a journal note, convert `activeStrategy` to a collection measure record
4. Create history tables for temporal audit (same pattern as existing `cases_history`)
5. Drop deprecated columns after migration

### FR-14: Updated REST API

Extend CaseController with new endpoints:

```
GET    /api/v1/cases/{id}/parties          — List case parties
POST   /api/v1/cases/{id}/parties          — Add party
DELETE /api/v1/cases/{id}/parties/{partyId} — Remove party

GET    /api/v1/cases/{id}/journal          — List journal entries and notes
POST   /api/v1/cases/{id}/journal          — Add journal entry
POST   /api/v1/cases/{id}/journal/notes    — Add journal note

GET    /api/v1/cases/{id}/events           — Get case event history
GET    /api/v1/cases/{id}/timeline         — Get combined timeline

POST   /api/v1/cases/{id}/state            — Transition case state
POST   /api/v1/cases/{id}/assign           — Assign caseworker (update existing)

GET    /api/v1/cases/{id}/measures          — List collection measures
POST   /api/v1/cases/{id}/measures          — Initiate collection measure

GET    /api/v1/cases/{id}/objections        — List objections
POST   /api/v1/cases/{id}/objections        — Receive objection
PUT    /api/v1/cases/{id}/objections/{id}   — Resolve objection

GET    /api/v1/cases/{id}/balance           — Computed balance (calls debt-service + payment-service)

GET    /api/v1/cases/{id}/legal-bases       — List legal bases
POST   /api/v1/cases/{id}/legal-bases       — Add legal basis

GET    /api/v1/cases/{id}/relations         — List related cases
POST   /api/v1/cases/{id}/relations         — Add case relation
```

All endpoints enforce `CASEWORKER` or `ADMIN` role via `@PreAuthorize`.

### FR-15: Updated CaseDto and sub-DTOs

Replace the flat CaseDto in opendebt-common with a richer structure:

```java
CaseDto
├── id, caseNumber, title, description
├── caseState, stateChangedAt
├── caseType, subjectClassification
├── primaryCaseworkerId
├── parties: List<CasePartyDto>
├── debtIds: List<UUID>  (kept for backward compatibility)
├── activeMeasures: List<CollectionMeasureDto>
├── openObjections: int (count)
├── createdAt, updatedAt
```

New DTOs: CasePartyDto, CaseJournalEntryDto, CaseJournalNoteDto, CaseEventDto, CaseLegalBasisDto, CollectionMeasureDto, ObjectionDto, CaseRelationDto, CaseBalanceDto, CaseTimelineEntryDto.

## Out of scope

- Flowable BPMN process definition updates (separate petition — the workflow delegates already exist as stubs).
- Full KLE classification integration (requires reference data service).
- Sagsarkiv / afleveringsversioner (archival delivery — deferred until production).
- Digital Post integration for case documents (handled by letter-service).
- Full caseworker portal integration (petition 040 will consume the new APIs).
- Sagsoverblik / overbliksløsning (cross-case dashboard — separate petition).

## Non-functional requirements

| Requirement | Target |
|------------|--------|
| Backward compatibility | Existing API consumers (creditor-portal, caseworker-portal) must continue to work during migration via the flat CaseDto with debtIds |
| Migration safety | V3 migration must be non-destructive — old data preserved in history tables |
| Performance | Case event log queries must be indexed by caseId and performedAt |
| Temporal history | All mutable entities must have corresponding _history tables with sysperiod |
| Audit | Every state-changing operation must produce a CaseEventEntity |

## Dependencies

| Petition | Dependency |
|----------|-----------|
| 006 | Indsigelse workflow — ObjectionEntity provides the data model |
| 040 | Sagsbehandlerportal — consumes the new case APIs |
| 041 | Demo access — demo data must be updated for new schema |

## References

- OIO Specifikation af Model for Sag Version 2.0 — https://arkitektur.digst.dk/specifikationer/sag/oio-specifikation-af-model-sag
- OpenDebt begrebsmodel v3 — `docs/begrebsmodel/Inddrivelse-begrebsmodel-UFST-v3.md`
- Begrebsmodel implementeringsmapping — `docs/begrebsmodel/Inddrivelse-begrebsmodel-implementeringsmapping.md`
- ADR-0007: No cross-service database connections
- ADR-0013: Enterprise PostgreSQL with audit and history
- ADR-0014: GDPR data isolation — person-registry
- ADR-0016: Flowable workflow engine
- De fællesoffentlige regler for begrebs- og datamodellering — https://arkitektur.digst.dk/metoder/begrebs-og-datametoder/regler-begrebs-og-datamodellering

## Terminology mapping (begrebsmodel v3)

| Danish (OIO Sag) | English (code) | Entity |
|------------------|---------------|--------|
| Sag | Case | CaseEntity |
| Sagspart | Case Party | CasePartyEntity |
| Sagsnummer | Case Number | caseNumber |
| SagsTilstand | Case State | CaseState enum |
| JournalPost | Journal Entry | CaseJournalEntryEntity |
| JournalNotat | Journal Note | CaseJournalNoteEntity |
| Sagshjemmel | Legal Basis | CaseLegalBasisEntity |
| Emneklasse | Subject Classification | subjectClassification |
| Handlingsklasse | Action Classification | actionClassification |
| Oversag | Parent Case | parentCaseId |
| AndenSag | Related Case | CaseRelationEntity |
| Præcedens | Precedent | precedentCaseId |
| PrimærBehandler | Primary Caseworker | primaryCaseworkerId |
| Inddrivelsesskridt | Collection Measure | CollectionMeasureEntity |
| Indsigelse | Objection | ObjectionEntity |
| Principielindikator | Precedent Indicator | precedentIndicator |
| Kassationsindikator | Retention Override | retentionOverride |
