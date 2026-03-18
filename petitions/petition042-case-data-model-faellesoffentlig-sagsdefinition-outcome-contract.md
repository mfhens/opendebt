# Petition 042 — Outcome contract

## Petition

Case data model based on the fællesoffentlige sagsdefinition (OIO Sag v2.0)

## Acceptance criteria

### Data model

1. **AC-1: CaseEntity has OIO Sag core attributes** — CaseEntity contains: id, caseNumber, title, description, confidentialTitle, caseState (enum), stateChangedAt, caseType, subjectClassification, actionClassification, precedentIndicator, retentionOverride, ownerOrganisationId, responsibleUnitId, primaryCaseworkerId, parentCaseId, workflowProcessInstanceId, createdBy, createdAt, updatedAt, version, sysperiod.

2. **AC-2: CasePartyEntity exists** — A `case_parties` table exists with: id, caseId (FK), personId, partyRole (PRIMARY_DEBTOR, CO_DEBTOR, CREDITOR, LEGAL_REPRESENTATIVE, CONTACT_PERSON, GUARANTOR), partyType (PERSON, ORGANISATION), activeFrom, activeTo, addedBy, createdAt.

3. **AC-3: Multiple parties per case** — A case can have multiple parties with different roles. At least one PRIMARY_DEBTOR is required. Multiple CO_DEBTOR entries are supported for solidarisk hæftelse.

4. **AC-4: CaseDebtEntity exists** — A `case_debts` table replaces the simple join table, with: id, caseId (FK), debtId, addedAt, addedBy, removedAt, removedBy, transferReference, notes.

5. **AC-5: CaseJournalEntryEntity exists** — A `case_journal_entries` table exists with: id, caseId (FK), journalEntryTitle, journalEntryTime, documentId, documentDirection (INCOMING, OUTGOING, INTERNAL), documentType, confidentialTitle, registeredBy, createdAt.

6. **AC-6: CaseJournalNoteEntity exists** — A `case_journal_notes` table exists with: id, caseId (FK), noteTitle, noteText, authorId, createdAt, updatedAt.

7. **AC-7: CaseLegalBasisEntity exists** — A `case_legal_bases` table exists with: id, caseId (FK), legalSourceUri, legalSourceTitle, paragraphReference, description, createdAt.

8. **AC-8: CaseEventEntity exists** — A `case_events` table exists with: id, caseId (FK), eventType (enum with 15+ event types), description, metadata (JSON), performedBy, performedAt. Indexed by (caseId, performedAt).

9. **AC-9: CaseRelationEntity exists** — A `case_relations` table exists with: id, sourceCaseId (FK), targetCaseId (FK), relationType (PARENT, RELATED, PRECEDENT, SPLIT_FROM, MERGED_INTO), description, createdBy, createdAt.

10. **AC-10: CollectionMeasureEntity exists** — A `collection_measures` table exists with: id, caseId (FK), debtId, measureType (VOLUNTARY_PAYMENT, PAYMENT_PLAN, WAGE_GARNISHMENT, OFFSETTING, ATTACHMENT), status (PLANNED, ACTIVE, COMPLETED, CANCELLED), startDate, endDate, amount, reference, notes, createdBy, createdAt, updatedAt.

11. **AC-11: ObjectionEntity exists** — An `objections` table exists with: id, caseId (FK), debtId, objectionType (EXISTENCE, AMOUNT, BASIS, TREATMENT, LIMITATION), status (RECEIVED, UNDER_REVIEW, ACCEPTED, REJECTED), description, debtorStatement, caseworkerAssessment, receivedAt, resolvedAt, resolvedBy, createdAt.

### State machine

12. **AC-12: State transitions are enforced** — Invalid state transitions (e.g., CREATED → CLOSED_PAID) are rejected with an error. Only valid transitions as defined in the petition are allowed.

13. **AC-13: State transitions are logged** — Every state transition creates a CaseEventEntity with eventType=STATE_CHANGED and metadata containing fromState and toState.

### Service layer

14. **AC-14: CaseServiceImpl is fully implemented** — All methods from the CaseService interface work correctly: createCase, getCaseById, listCases, transitionState, addParty, removeParty, addDebt, removeDebt, addJournalEntry, addJournalNote, addLegalBasis, initiateCollectionMeasure, receiveObjection, resolveObjection, assignCaseworker, getCaseEvents.

15. **AC-15: All state-changing operations produce events** — Creating a case, changing state, adding/removing parties, adding/removing debts, adding journal entries, initiating collection measures, and receiving/resolving objections all produce corresponding CaseEventEntity entries.

16. **AC-16: Computed balance** — `getCaseBalance(caseId)` calls debt-service and payment-service APIs to compute current balances (no stored denormalized totals).

### API

17. **AC-17: New REST endpoints exist** — All endpoints from FR-14 respond correctly: parties CRUD, journal CRUD, events list, state transition, measures CRUD, objections CRUD, balance, legal bases, relations.

18. **AC-18: Authorization enforced** — All new endpoints require `CASEWORKER` or `ADMIN` role via `@PreAuthorize`.

### Migration

19. **AC-19: V3 migration runs cleanly** — The Flyway V3 migration creates all new tables, adds new columns, migrates existing data, and does not lose existing case data.

20. **AC-20: Backward compatibility** — The flat CaseDto (with debtIds list) continues to work for existing consumers. New detail DTOs are additive.

21. **AC-21: History tables** — All new mutable tables have corresponding `_history` tables with `sysperiod` for temporal audit.

### Tests

22. **AC-22: Unit tests for state machine** — Tests cover all valid transitions and reject all invalid transitions.

23. **AC-23: Unit tests for CaseServiceImpl** — Tests cover all CRUD operations, party management, journal management, event logging, and balance computation (with mocked service clients).

24. **AC-24: Architecture tests pass** — SharedArchRules (PII, cross-DB) pass for case-service with the new entities.

## Definition of done

- All 8 new entity classes exist with JPA annotations and builder pattern.
- All 8 new database tables exist via Flyway V3 migration with proper indexes and constraints.
- CaseServiceImpl implements all interface methods with event logging.
- State machine enforces valid transitions.
- All REST endpoints from FR-14 are implemented and secured.
- Unit tests achieve 80% line coverage, 70% branch coverage.
- `mvn verify` passes for case-service.
- Demo seed data (V2) updated for new schema or replaced by V4.

## Failure conditions

- CaseEntity remains a flat table without proper party, journal, or event support.
- No enforcement of state transitions (any status change is allowed).
- State changes not logged as events (no audit trail).
- Balance values stored on the case instead of computed from APIs.
- Single debtorPersonId field instead of proper CasePartyEntity.
- No journal support (JournalPost/JournalNotat missing).
- No legal basis references.
- Migration destroys existing case data.
- Existing API consumers break (creditor-portal, caseworker-portal).
