# Petition 059 Outcome Contract

## Petition reference

**Petition 059:** Forældelse — prescription rules (G.A.2.4)
**Legal basis:** G.A.2.4 (v3.16, 2026-03-28), GIL §§ 18, 18a, Forældelsesl. §§ 3, 5, 18–19,
Gæld.bekendtg., SKM2015.718.ØLR

---

## Observable outcomes by functional requirement

### FR-1: Forældelsesfrist tracking

**Preconditions**
- A fordring has been accepted to inddrivelse and a `ForaeldelseRecord` exists for it.
- The fordring has a `retsgrundlag` type set at registration.

**Trigger**
- A GET request is made to `GET /foraeldelse/{fordringId}`.

**Expected backend behaviour**
- The endpoint returns a `ForaeldelseStatusDto` containing:
  - `fordringId` (UUID)
  - `currentFristExpires` (LocalDate — the computed next expiry date)
  - `udskydelseDato` (LocalDate — the applicable GIL § 18a start-date)
  - `isInUdskydelse` (boolean — true if current date is before `udskydelseDato`)
  - `retsgrundlag` (ORDINARY or SAERLIGT_RETSGRUNDLAG)
  - `afbrydelseHistory` (ordered list of all registered afbrydelse events with type,
    eventDate, legalReference, newFristExpires)
  - `tillaegsfristHistory` (ordered list of all tillægsfrister with type, appliedDate,
    extensionYears, newFristExpires)
  - `status` (ACTIVE, FORAELDET, or INDSIGELSE_PENDING)
- A fordring with no afbrydelse events returns `currentFristExpires = udskydelseDato + 3 years`
  or the ordinary 3-year frist from registration if udskydelse does not apply.
- A fordring with afbrydelse events returns `currentFristExpires` computed from the most
  recent afbrydelse event date plus the applicable frist (3 or 10 years).
- Requesting a fordringId that does not exist returns HTTP 404.

**Expected portal behaviour**
- The sagsbehandlerportal displays the forældelsesstatus panel on the fordring detail page
  showing all fields from `ForaeldelseStatusDto` in a human-readable format.

---

### FR-2: Udskydelse — system-specific start-date rules (GIL § 18a, stk. 1 / G.A.2.4.1)

**Preconditions**
- A fordring is being registered in the system with a known source system and registration date.

**Trigger**
- Fordring registration event (accepted to inddrivelse).

**Expected backend behaviour**
- For a PSRM fordring with registration date ≥ 2015-11-19:
  - `udskydelseDato` is set to `2021-11-20`.
  - `currentFristExpires` cannot be earlier than `2024-11-21` regardless of
    the registration date.
  - `isInUdskydelse` is `true` if the current date is before `2021-11-20`.
- For a DMI/SAP38 fordring with registration date ≥ 2024-01-01:
  - `udskydelseDato` is set to `2027-11-20`.
  - `currentFristExpires` cannot be earlier than `2030-11-21`.
  - `isInUdskydelse` is `true` if the current date is before `2027-11-20`.
- Udskydelse is stored as a non-mutable field. A subsequent afbrydelse event does not
  change `udskydelseDato`.
- A fordring outside the udskydelse ranges has `udskydelseDato = null` and
  `isInUdskydelse = false`.

**Expected portal behaviour**
- The forældelsesstatus panel shows the udskydelseDato and whether the fordring is currently
  in an udskydelse window.

---

### FR-3: Afbrydelse event registration

**Preconditions**
- A `ForaeldelseRecord` exists for the target fordring.
- The caller holds the `CASEWORKER` or `SYSTEM_INTEGRATION` role.

**Trigger**
- `POST /foraeldelse/{fordringId}/afbrydelse` with a valid `AfbrydelseEventDto`.

**Expected backend behaviour — BEROSTILLELSE (GIL § 18a, stk. 8):**
- Service accepts type `BEROSTILLELSE` with an `eventDate`.
- New `currentFristExpires = eventDate + 3 years`.
- Event logged with legal reference `GIL § 18a, stk. 8`.
- Response: HTTP 201 with the updated `ForaeldelseStatusDto`.

**Expected backend behaviour — LOENINDEHOLDELSE (GIL § 18, stk. 4):**
- Service accepts type `LOENINDEHOLDELSE` only when `afgoerelseRegistreret = true`.
- If `afgoerelseRegistreret` is `false` or absent: service returns HTTP 422 with a
  problem-detail body indicating that varsel alone does not constitute afbrydelse.
- Afbrydelse date: the `eventDate` (date underretning reaches the debtor).
- New `currentFristExpires = eventDate + 3 years`.
- Event logged with legal reference `GIL § 18, stk. 4 + Forældelsesl. § 18, stk. 4`.
- If lønindeholdelse is subsequently inactive for ≥ 1 year: a new 3-year frist begins.
- Propagation to fordringskompleks members (see FR-4).
- Response: HTTP 201.

**Expected backend behaviour — UDLAEG (Forældelsesl. § 18, stk. 1):**
- Service accepts type `UDLAEG` regardless of whether the udlæg was successful or forgæves.
- For a fordring with `retsgrundlag = ORDINARY`: new `currentFristExpires = eventDate + 3 years`.
- For a fordring with `retsgrundlag = SAERLIGT_RETSGRUNDLAG`: new
  `currentFristExpires = eventDate + 10 years`.
- Event logged with legal reference `Forældelsesl. § 18, stk. 1`.
- Response: HTTP 201.

**Expected backend behaviour — MODREGNING (Forældelsesl. § 18, stk. 4):**
- Service accepts type `MODREGNING` with an `eventDate`.
- New `currentFristExpires = eventDate + 3 years`.
- Event logged with legal reference `Forældelsesl. § 18, stk. 4`.
- Response: HTTP 201.

**Failure conditions for FR-3:**
- Type `LOENINDEHOLDELSE` with `afgoerelseRegistreret = false` → HTTP 422.
- Unknown event type → HTTP 422.
- Missing `eventDate` → HTTP 422.
- `fordringId` does not exist → HTTP 404.

---

### FR-4: Fordringskompleks membership and afbrydelse propagation (GIL § 18a, stk. 2 / G.A.2.4.2)

**Preconditions**
- Two or more fordringer are registered as members of the same fordringskompleks via
  `POST /fordringskompleks`.
- An afbrydelse event is registered for one member fordring.

**Trigger**
- `POST /foraeldelse/{fordringId}/afbrydelse` where `fordringId` is a member of a
  fordringskompleks.

**Expected backend behaviour**
- Within the same database transaction, the afbrydelse event is propagated to all other
  members of the same kompleks.
- Each propagated event is logged with:
  - `sourceFordringId` (the fordring that triggered the afbrydelse)
  - `targetFordringId` (each other member)
  - `propagationReason = FORDRINGSKOMPLEKS_PROPAGATION`
  - `legalReference = GIL § 18a, stk. 2`
- All members' `currentFristExpires` are updated to the same new value.
- If the transaction fails for any member, the entire transaction rolls back and no
  `ForaeldelseRecord` is updated (atomicity guarantee).
- `GET /foraeldelse/{anyMemberId}` for any member reflects the propagated event.

**Expected portal behaviour**
- The forældelsesstatus panel shows fordringskompleks membership and the source fordringId
  for any propagated afbrydelse event.

---

### FR-5: Tillægsfrister (G.A.2.4.4.2)

**Preconditions**
- A `ForaeldelseRecord` exists for the target fordring.
- An internal opskrivning has been applied to the fordring.

**Trigger**
- `POST /foraeldelse/{fordringId}/tillaegsfrist` with type `INTERN_OPSKRIVNING`.

**Expected backend behaviour**
- Service computes `newFristExpires = max(currentFristExpires, eventDate) + 2 years`.
- `ForaeldelseRecord.currentFristExpires` is updated atomically.
- A `TillaegsfristEvent` is logged with:
  - `type = INTERN_OPSKRIVNING`
  - `appliedDate` (the date of the internal opskrivning)
  - `extensionYears = 2`
  - `newFristExpires`
  - `legalReference = G.A.2.4.4.2`
- Response: HTTP 201 with updated `ForaeldelseStatusDto`.
- `tillaegsfristHistory` in the response contains the new entry.

---

### FR-6: Forældelsesindsigelse workflow (G.A.2.4.6)

**Preconditions**
- A `ForaeldelseRecord` exists for the target fordring with `status = ACTIVE`.
- The caseworker holds a role that permits indsigelse registration.

**Trigger — registration:**
- Caseworker registers the debtor's indsigelse via
  `POST /foraeldelse/{fordringId}/indsigelse`.

**Expected backend behaviour — registration:**
- Fordring status transitions from `ACTIVE` to `INDSIGELSE_PENDING`.
- A `ForaeldelseIndsigelse` record is created with a unique `indsigelsesId`, timestamp,
  and caseworker identity.
- An audit log (CLS) entry is created.
- Response: HTTP 201 with `{ indsigelsesId, status: INDSIGELSE_PENDING }`.

**Trigger — evaluation:**
- `PUT /foraeldelse/{fordringId}/indsigelse/{indsigelsesId}` with
  `{ outcome: VALID | INVALID, rationale: string }`.

**Expected backend behaviour — evaluation (VALID):**
- Fordring status transitions to `FORAELDET`.
- Fordring lifecycle transitions to a terminal inddrivelse state (removed from active
  inddrivelse).
- An audit log entry is created with outcome `VALID`, caseworker identity, timestamp, and
  rationale.
- Response: HTTP 200 with updated `ForaeldelseStatusDto` showing `status = FORAELDET`.

**Expected backend behaviour — evaluation (INVALID):**
- Fordring status returns to `ACTIVE`.
- The rejection rationale is stored in the `ForaeldelseIndsigelse` record.
- An audit log entry is created with outcome `INVALID`, caseworker identity, timestamp, and
  rationale.
- Response: HTTP 200 with updated `ForaeldelseStatusDto` showing `status = ACTIVE`.

**Expected portal behaviour:**
- The sagsbehandlerportal displays a "Registrer forældelsesindsigelse" button when
  `status = ACTIVE`.
- When `status = INDSIGELSE_PENDING`, the portal displays the evaluation form (VALID/INVALID
  with rationale text field).
- When `status = FORAELDET`, the portal displays the outcome and rationale; the button is
  no longer available.

---

### FR-7: Caseworker portal visibility

**Preconditions**
- A caseworker is authenticated and navigates to a fordring detail view.

**Trigger**
- The fordring detail page is rendered.

**Expected portal behaviour**
- A forældelsesstatus panel is rendered containing:
  - Current status (Aktiv / Forældet / Indsigelse under behandling) prominently displayed.
  - Next expiry date (`currentFristExpires`) in ISO date format.
  - Udskydelse information (applicable date, whether currently in udskydelse window).
  - Afbrydelse history table (type, date, legal reference, resulting new frist) in
    chronological order.
  - Fordringskompleks membership indicator (name/ID of kompleks, list of member fordringIds).
  - Tillægsfrist history (type, date, extension, new frist) if any tillægsfrister have
    been applied.
  - Indsigelse workflow controls per FR-6.
- Caseworkers with read-only access see the panel but cannot trigger write operations.
- The panel is accessible from the fordring detail view without requiring a separate
  navigation step.

---

## Acceptance criteria

1. `GET /foraeldelse/{fordringId}` returns a `ForaeldelseStatusDto` with `currentFristExpires`,
   `udskydelseDato`, `isInUdskydelse`, `retsgrundlag`, `afbrydelseHistory`,
   `tillaegsfristHistory`, and `status` (FR-1).
2. A fordring with no afbrydelse events and no udskydelse returns `currentFristExpires` equal
   to the registration date plus 3 years (FR-1).
3. A PSRM fordring registered from 19-11-2015 has `udskydelseDato = 2021-11-20`; its
   `currentFristExpires` is never earlier than `2024-11-21` (FR-2).
4. A DMI/SAP38 fordring registered from 2024-01-01 has `udskydelseDato = 2027-11-20`; its
   `currentFristExpires` is never earlier than `2030-11-21` (FR-2).
5. `udskydelseDato` is immutable; a subsequent afbrydelse event does not change its value (FR-2).
6. Registering a BEROSTILLELSE event sets `currentFristExpires = eventDate + 3 years` and
   logs the event with legal reference `GIL § 18a, stk. 8` (FR-3).
7. Registering a LOENINDEHOLDELSE event with `afgoerelseRegistreret = true` sets
   `currentFristExpires = eventDate + 3 years` and logs the event with legal reference
   `GIL § 18, stk. 4` (FR-3).
8. Registering a LOENINDEHOLDELSE event with `afgoerelseRegistreret = false` (varsel alone)
   returns HTTP 422; no afbrydelse event is logged and `currentFristExpires` is unchanged (FR-3).
9. Registering a UDLAEG event on a fordring with `retsgrundlag = ORDINARY` sets
   `currentFristExpires = eventDate + 3 years` (FR-3).
10. Registering a UDLAEG event on a fordring with `retsgrundlag = SAERLIGT_RETSGRUNDLAG`
    sets `currentFristExpires = eventDate + 10 years` (FR-3).
11. Registering a forgæves udlæg (insolvenserklæring) has the same afbrydelse effect as a
    successful udlæg for both ORDINARY and SAERLIGT_RETSGRUNDLAG fordringer (FR-3).
12. Registering a MODREGNING event sets `currentFristExpires = eventDate + 3 years` and logs
    the event with legal reference `Forældelsesl. § 18, stk. 4` (FR-3).
13. When an afbrydelse event is registered for a fordring that is a member of a
    fordringskompleks, all other members receive the propagated event in the same
    transaction; each member's `currentFristExpires` is updated; propagation is logged with
    `GIL § 18a, stk. 2` (FR-4).
14. If the fordringskompleks propagation transaction fails for any member, no
    `ForaeldelseRecord` is updated (atomicity) (FR-4).
15. Registering `INTERN_OPSKRIVNING` tillægsfrist adds 2 years to `currentFristExpires` and
    logs the extension with legal reference `G.A.2.4.4.2` (FR-5).
16. Registering a forældelsesindsigelse transitions the fordring status to
    `INDSIGELSE_PENDING` and creates a `ForaeldelseIndsigelse` record with a unique ID (FR-6).
17. Evaluating a forældelsesindsigelse as `VALID` transitions the fordring to status
    `FORAELDET` and removes it from active inddrivelse; the outcome is audit-logged (FR-6).
18. Evaluating a forældelsesindsigelse as `INVALID` returns the fordring to status `ACTIVE`
    and stores the rejection rationale; the outcome is audit-logged (FR-6).
19. The sagsbehandlerportal forældelsesstatus panel displays all required fields (status,
    expiry date, udskydelse, afbrydelse history, kompleks membership, tillægsfrist history,
    indsigelse controls) on the fordring detail page (FR-7).
20. All afbrydelse event registrations, kompleks propagations, tillægsfrister, indsigelse
    registrations, and indsigelse evaluations are logged to the audit log (CLS) with
    caseworker or system identity, timestamp, fordringId, event type, and legal reference
    (NFR-2).
21. No CPR, name, address, or other PII appears in any `ForaeldelseRecord`,
    `AfbrydelseEvent`, `TillaegsfristEvent`, `FordringskompleksLink`,
    `ForaeldelseIndsigelse` entity or in any API response body — debtors are referenced
    only by `person_id` UUID (NFR-3).
22. Every acceptance criterion is covered by at least one Gherkin scenario.

---

## Definition of done

- `ForaeldelseService` calculates `currentFristExpires` correctly for all cases (PSRM,
  DMI/SAP38, særligt retsgrundlag, with/without afbrydelse events).
- Udskydelse dates from GIL § 18a, stk. 1 applied as non-mutable fields at registration.
- All four afbrydelse types implemented with correct date logic, frist calculation, and
  legal reference logging.
- HTTP 422 returned when LOENINDEHOLDELSE is submitted without confirmed afgørelse.
- Forgæves udlæg treated identically to successful udlæg.
- Fordringskompleks propagation is atomic and logged with GIL § 18a, stk. 2 reference.
- Tillægsfrister (2-year extension) applied and logged for INTERN_OPSKRIVNING events.
- Forældelsesindsigelse workflow: register → evaluate (VALID → FORAELDET removes from
  inddrivelse; INVALID → ACTIVE with stored rationale).
- `GET /foraeldelse/{fordringId}` returns correct `ForaeldelseStatusDto` at p99 < 200 ms.
- Sagsbehandlerportal panel displays all required forældelsesstatus fields.
- No PII in forældelses-service entities or API responses.
- All 22 AC covered by Gherkin scenarios.
- `behave --dry-run` passes on `petitions/petition059-foraeldelse.feature`.

---

## Success metrics

| Metric | Target |
|--------|--------|
| Fordringer with correct `currentFristExpires` (no afbrydelse) | 100% |
| PSRM fordringer (≥ 19-11-2015) with `udskydelseDato = 2021-11-20` | 100% |
| DMI/SAP38 fordringer (≥ 2024-01-01) with `udskydelseDato = 2027-11-20` | 100% |
| LOENINDEHOLDELSE without afgørelse rejected (HTTP 422) | 100% |
| Forgæves udlæg afbryder equally to successful udlæg | 100% |
| Fordringskompleks propagation atomic (all or nothing) | 100% |
| VALID forældelsesindsigelse removes fordring from inddrivelse | 100% |
| INVALID forældelsesindsigelse stores rationale and returns fordring to ACTIVE | 100% |
| Audit log entries for all afbrydelse events and indsigelse outcomes | 100% |
| No PII in forældelses entities or API responses | 100% |
| `GET /foraeldelse/{fordringId}` p99 response time | < 200 ms |
| AC covered by at least one Gherkin scenario | 22/22 |

---

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| `ForaeldelseRecord` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/ForaeldelseRecord.java` |
| `AfbrydelseEvent` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/AfbrydelseEvent.java` |
| `TillaegsfristEvent` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/TillaegsfristEvent.java` |
| `FordringskompleksLink` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/FordringskompleksLink.java` |
| `ForaeldelseIndsigelse` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/ForaeldelseIndsigelse.java` |
| `ForaeldelseService` | `opendebt-debt-service/src/main/java/.../service/ForaeldelseService.java` |
| `FordringskompleksService` | `opendebt-debt-service/src/main/java/.../service/FordringskompleksService.java` |
| `ForaeldelseApiController` | `opendebt-debt-service/src/main/java/.../controller/ForaeldelseApiController.java` |
| `ForaeldelseStatusDto` | `opendebt-debt-service/src/main/java/.../dto/ForaeldelseStatusDto.java` |
| `AfbrydelseEventDto` | `opendebt-debt-service/src/main/java/.../dto/AfbrydelseEventDto.java` |
| `TillaegsfristDto` | `opendebt-debt-service/src/main/java/.../dto/TillaegsfristDto.java` |
| `ForaeldelseIndsigelsesDto` | `opendebt-debt-service/src/main/java/.../dto/ForaeldelseIndsigelsesDto.java` |
| Sagsbehandlerportal forældelsesstatus panel | `opendebt-caseworker-portal/.../templates/fordring/foraeldelse-status.html` |
| Sagsbehandlerportal indsigelse form | `opendebt-caseworker-portal/.../templates/fordring/foraeldelse-indsigelse.html` |
| Danish message bundle additions | `opendebt-caseworker-portal/src/main/resources/messages_da.properties` |
| English message bundle additions | `opendebt-caseworker-portal/src/main/resources/messages_en_GB.properties` |
| Liquibase migration | `opendebt-debt-service/src/main/resources/db/changelog/` |
| Gherkin feature file | `petitions/petition059-foraeldelse.feature` |

---

## Failure conditions

- `GET /foraeldelse/{fordringId}` does not return `currentFristExpires`, `udskydelseDato`,
  `afbrydelseHistory`, or `status` in the response body.
- A PSRM fordring (≥ 19-11-2015) has `currentFristExpires` earlier than 2024-11-21.
- A DMI/SAP38 fordring (≥ 2024-01-01) has `currentFristExpires` earlier than 2030-11-21.
- `udskydelseDato` changes after a subsequent afbrydelse event.
- LOENINDEHOLDELSE with `afgoerelseRegistreret = false` is accepted (varsel alone afbryder).
- Varsel-only afbrydelse registration succeeds and updates `currentFristExpires`.
- A UDLAEG event on a særligt retsgrundlag fordring produces a 3-year (not 10-year) frist.
- Forgæves udlæg does not constitute afbrydelse.
- Fordringskompleks propagation is partial (some members updated, others not).
- A valid forældelsesindsigelse does not remove the fordring from active inddrivelse.
- An invalid forældelsesindsigelse does not store the rationale or return the fordring to ACTIVE.
- Any afbrydelse event, tillægsfrist, or indsigelse outcome is not logged to the audit log.
- CPR, name, address, or other PII appears in any entity or API response.
- `behave --dry-run` fails on `petitions/petition059-foraeldelse.feature`.
