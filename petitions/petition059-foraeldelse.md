# Petition 059: Forældelse — prescription rules (G.A.2.4)

## Summary

Prescription (forældelse) is a substantive debtor right under Danish law: a fordring whose
forældelsesfrist has expired is legally extinguished and may no longer be the subject of
inddrivelse. Continuation of inddrivelse on a forældet fordring constitutes unlawful
collection activity and exposes Gældsstyrelsen to administrative fines, debtor compensation
claims, and reputational damage toward the Danish public-sector creditor community.

PSRM must therefore maintain a real-time, auditable record of the forældelsesfrist for every
fordring under management, register all events that interrupt (afbryde) or extend the frist,
and present the current forældelsesstatus — including the next expected expiry date and full
afbrydelse history — to caseworkers and downstream system processes.

The forældelses-ruleset for PSRM fordringer is defined in G.A.2.4 (v3.16, 2026-03-28) and
deviates substantially from the ordinary forældelsesfrist regime: all fordringer under PSRM
management carry a **3-year inddrivelse-forældelsesfrist** regardless of the original frist
or retsgrundlag, and GIL § 18a introduces system-specific start-date rules (udskydelse) that
postpone the commencement of the frist well beyond the registration date. Four categories of
PSRM-side events afbryder (reset) the clock; related fordringer sharing a fordringskompleks
must have afbrydelse events propagated across all members; and an internal opskrivning can
add a 2-year tillægsfrist.

Without this petition, PSRM has no systematic mechanism for tracking forældelsesfrist.
Caseworkers rely on manual awareness to identify approaching expiry dates — a material
compliance risk given the volume and complexity of the G.A.2.4 ruleset. This petition is
rated **Tier A** and is a direct prerequisite for petition 061 (afdragsordninger), petition
062 (lønindeholdelse full spec), and petition 065 (bortfald og afskrivning). A Catala formal
encoding of the forældelses rules is tracked separately in petition 070.

**References:** Petition 003 (fordring lifecycle), Petition 007 (lønindeholdelse trigger),
Petition 053 (opskrivning — tillægsfrist linkage), Petition 057 (dækningsrækkefølge —
prerequisite), Petition 066 (udlæg), Petition 070 (Catala companion spike, separate petition).

---

## Context and motivation

The Gæld til Det Offentlige Inddrivelse Act (GIL) mandates that PSRM tracks the
forældelsesfrist for every fordring from the moment of acceptance. The G.A.2.4 rules (v3.16)
document the complete ruleset applicable to PSRM:

- **Base frist (G.A.2.4.3):** 3 years for all PSRM-managed fordringer, regardless of the
  original claim's retsgrundlag or the forældelsesfrist that applied before inddrivelse.
  Even a fordring based on a dom (dom normally giving 10 years) carries only a 3-year frist
  once under PSRM management — except after an udlæg event on a fordring with særligt
  retsgrundlag, which then gives a new 10-year frist.
- **Udskydelse (G.A.2.4.1 / GIL § 18a, stk. 1):** The start date of the forældelsesfrist is
  postponed by statute for both legacy PSRM (from 19-11-2015: earliest frist 20-11-2021) and
  DMI/SAP38 fordringer (registered from 1-1-2024: earliest frist 20-11-2027). Udskydelse is
  not an afbrydelse — it is a one-time statutory postponement of the starting point.
- **Afbrydelse (G.A.2.4.4.1):** Four categories of inddrivelse events reset the forældelses-
  frist clock and start a new period from the event date: (1) afgørelse om berostillelse,
  (2) lønindeholdelse (confirmed afgørelse, not mere varsel), (3) udlæg (including forgæves
  udlæg), and (4) modregning.
- **Fordringskomplekser (G.A.2.4.2):** Certain fordringer are grouped; an afbrydelse event
  for one member propagates to all other members of the kompleks.
- **Tillægsfrister (G.A.2.4.4.2):** An internal opskrivning (interne opskrivning, G.A.2.3.4.4)
  extends the running forældelsesfrist by 2 years.
- **Indsigelse (G.A.2.4.6):** A debtor may formally assert forældelse. PSRM must support the
  caseworker workflow to evaluate and register the indsigelse outcome. A valid indsigelse
  removes the fordring from inddrivelse.

The absence of systematic forældelses-tracking is a material compliance risk. Missed expiry
dates lead to unlawful collection; the lack of an auditable record makes it impossible to
demonstrate compliance in court or in response to ombudsman or Datatilsyn enquiries.

---

## Legal basis / G.A. references

| Reference | Content |
|-----------|---------|
| G.A.2.4 (v3.16, 2026-03-28) | Complete forældelses-ruleset for PSRM/DMI fordringer |
| G.A.2.4.1 | Udskydelse — GIL § 18a, stk. 1 start-date rules for PSRM and DMI/SAP38 fordringer |
| G.A.2.4.2 | Fordringskomplekser — definition, membership rules, afbrydelse propagation |
| G.A.2.4.3 | 3-year base forældelsesfrist for all PSRM-managed fordringer; 10-year exception for udlæg + særligt retsgrundlag |
| G.A.2.4.4.1 | Egentlig afbrydelse — four event types: berostillelse, lønindeholdelse, udlæg, modregning |
| G.A.2.4.4.2 | Tillægsfrister — 2-year extension triggered by interne opskrivninger |
| G.A.2.4.5 | Strafbare forhold — special rules for bøder and konfiskationsbeløb (out of scope) |
| G.A.2.4.6 | Indsigelse over forældelse — debtor challenge workflow and removal from inddrivelse |
| GIL § 18, stk. 4 | Lønindeholdelse afbryder forældelsesfrist when underretning om afgørelse reaches debtor |
| GIL § 18a, stk. 1 | Udskydelse — statutory postponement of forældelsesfrist start date for PSRM and DMI |
| GIL § 18a, stk. 2 | Fordringskomplekser — grouping rule and cross-member afbrydelse |
| GIL § 18a, stk. 8 | Afgørelse om berostillelse afbryder forældelsesfrist (PSRM only) |
| Forældelsesl. § 3, stk. 1 | 3-year base forældelsesfrist for ordinary claims |
| Forældelsesl. § 5, stk. 1 | 10-year frist for claims based on dom or forlig (særligt retsgrundlag) |
| Forældelsesl. § 18, stk. 1 | Udlæg afbryder forældelsesfrist — also applies to forgæves udlæg |
| Forældelsesl. § 18, stk. 4 | Modregning afbryder forældelsesfrist |
| Forældelsesl. § 19 | Suspension af forældelsesfrist under certain periods |
| Gæld.bekendtg. | Inddrivelsesregler for fordringer under PSRM |
| SKM2015.718.ØLR | Landsretsdom: afbrydelse by lønindeholdelse requires actual afgørelse, not mere varsel |

---

## Domain terms

| Term | Danish | Definition |
|------|--------|------------|
| Forældelse | Prescription | Legal extinction of a debt claim due to the passage of the forældelsesfrist without interruption |
| Forældelsesfrist | Prescription period | The time window after which a claim lapses if no afbrydelse event occurs |
| Afbrydelse | Interruption | An inddrivelse event that resets the forældelsesfrist clock and starts a new period |
| Egentlig afbrydelse | True interruption | A permanent reset of the clock (distinguished from suspension) |
| Tillægsfrist | Additional period | A 2-year extension of the forældelsesfrist triggered by an internal opskrivning |
| Fordringskompleks | Claim complex | A group of related fordringer for which an afbrydelse event on one member propagates to all members |
| Berostillelse | Suspension of collection | An administrative decision by PSRM to suspend inddrivelse of a fordring; afbryder forældelsesfrist |
| Udskydelse | Postponement | A statutory rule that delays the commencement of the forældelsesfrist start date |
| Indsigelse | Objection | A formal assertion by the debtor that the fordring is forældet and may no longer be collected |
| Særligt retsgrundlag | Special legal basis | A dom or forlig that gives a 10-year new frist after an udlæg event |
| Forgæves udlæg | Unsuccessful attachment | An udlæg attempt resulting in an insolvenserklæring; still constitutes valid afbrydelse |
| Underretning | Notification | The formal act of informing the debtor of an inddrivelse decision (e.g. lønindeholdelse afgørelse) |

---

## PSRM Reference Context

In PSRM, the forældelsesfrist is tracked at the fordring level. Each fordring carries:

- A **source system** (PSRM or DMI/SAP38) and **registration date** that determine which
  udskydelse rule applies under GIL § 18a, stk. 1.
- A **retsgrundlag** flag indicating whether the fordring has særligt retsgrundlag (dom/forlig),
  which governs whether an udlæg event sets a 10-year rather than 3-year new frist.
- An **afbrydelse event log** recording each event type, date, and the legal reference that
  triggers it, enabling deterministic reconstruction of the current forældelsesfrist.
- Optional **fordringskompleks membership** — fordringer linked under GIL § 18a, stk. 2 share
  their afbrydelse events across all members.

The forældelsesfrist calculation resolves to:

```
current_frist_expires = max(
    udskydelse_date,
    last_afbrydelse_date + base_frist_years,
    last_tillægsfrist_date + 2 years   (if applicable)
)
```

where `base_frist_years` is 3 for all event types except udlæg on særligt retsgrundlag (10).

**System-specific udskydelse rules (GIL § 18a, stk. 1 / G.A.2.4.1):**

| Source system | Registration condition | Forældelsesfrist earliest from | Forældet earliest |
|---|---|---|---|
| PSRM | From 19-11-2015 onward | 20-11-2021 | 21-11-2024 |
| DMI/SAP38 | Registered on 1-1-2024 or later | 20-11-2027 | 21-11-2030 |

Note: Udskydelse prevents the clock from starting. It is not an afbrydelse and does not
reset a clock that is already running from an earlier afbrydelse event.

**Afbrydelse event types (G.A.2.4.4.1):**

| Type | Afbrydelse occurs when | New frist after afbrydelse |
|---|---|---|
| BEROSTILLELSE (GIL § 18a, stk. 8) | Afgørelse om berostillelse is made | 3 years from afgørelse date |
| LOENINDEHOLDELSE (GIL § 18, stk. 4) | Underretning om afgørelse om lønindeholdelse reaches debtor | 3 years; new frist if inactive for ≥ 1 year |
| UDLAEG (forældelsesl. § 18, stk. 1) | Anmodning om udlæg is lodged (or udgående forretning begins) | 3 years; 10 years if særligt retsgrundlag |
| MODREGNING (forældelsesl. § 18, stk. 4) | Modregning is executed | 3 years from modregning date |

**Lønindeholdelse nuances (SKM2015.718.ØLR / GIL § 18, stk. 4):**
- Varsel alone does NOT constitute afbrydelse — only confirmed afgørelse afbryder.
- Afbrydelse operates at afgørelsesniveau: covers all fordringer listed in the afgørelse.
- If lønindeholdelse has been inactive for 1 year, a new forældelsesfrist begins running.
- Afbrydelse of one fordring in a fordringskompleks propagates to all members of the kompleks.

---

## Functional requirements

### FR-1: Forældelsesfrist tracking

PSRM shall maintain a persistent `ForaeldelseRecord` for each fordring under inddrivelse,
storing:
- The applicable udskydelse date (per GIL § 18a, stk. 1 — computed at fordring registration)
- The retsgrundlag type (`ORDINARY` or `SAERLIGT_RETSGRUNDLAG`)
- A complete, ordered log of afbrydelse events (type, date, legal reference, new frist date)
- A complete log of tillægsfrister (type, date, extension applied)
- The computed `currentFristExpires` date (updated on each event)
- The fordring's current forældelsesstatus: `ACTIVE`, `FORAELDET`, or `INDSIGELSE_PENDING`

The `ForaeldelseRecord` shall be created automatically when a fordring is accepted to
inddrivelse, initialised with the udskydelse date and the current forældelsesstatus `ACTIVE`.

The service shall expose a read endpoint:

```
GET /foraeldelse/{fordringId}
Response: ForaeldelseStatusDto {
    fordringId: UUID,
    currentFristExpires: LocalDate,
    udskydelseDato: LocalDate,
    isInUdskydelse: boolean,
    retsgrundlag: ORDINARY | SAERLIGT_RETSGRUNDLAG,
    afbrydelseHistory: [
        { type, eventDate, legalReference, newFristExpires }
    ],
    tillaegsfristHistory: [
        { type, appliedDate, extensionYears, newFristExpires }
    ],
    status: ACTIVE | FORAELDET | INDSIGELSE_PENDING
}
```

### FR-2: Udskydelse — system-specific start-date rules (GIL § 18a, stk. 1 / G.A.2.4.1)

At fordring registration, `ForaeldelseService` shall compute and store the udskydelse date
based on source system and registration date:

- **PSRM fordringer** registered from 19-11-2015 onward: `udskydelseDato = 2021-11-20`.
  No forældelsesfrist can expire before 2024-11-21.
- **DMI/SAP38 fordringer** registered on 2024-01-01 or later: `udskydelseDato = 2027-11-20`.
  No forældelsesfrist can expire before 2030-11-21.
- Fordringer outside these ranges follow the ordinary forældelsesfrist calculation.

Udskydelse is a one-time, non-mutable start-date rule. Once set at registration, it cannot
be changed by subsequent afbrydelse events. It prevents the clock from starting; it does
not reset an already-running clock.

### FR-3: Afbrydelse event registration

The service shall accept POST requests to register afbrydelse events of all four types
defined in G.A.2.4.4.1:

**Endpoint:** `POST /foraeldelse/{fordringId}/afbrydelse`

**Request body (`AfbrydelseEventDto`):**
```
{
    type: BEROSTILLELSE | LOENINDEHOLDELSE | UDLAEG | MODREGNING,
    eventDate: LocalDate,
    legalReference: string,
    afgoerelseRegistreret: boolean   (required and must be true for LOENINDEHOLDELSE)
}
```

**Per-type behaviour:**

1. **BEROSTILLELSE** (GIL § 18a, stk. 8):
   - Afbrydelse date: the date the afgørelse om berostillelse is made.
   - New frist: `eventDate + 3 years`.
   - Applies to PSRM-managed fordringer only.

2. **LOENINDEHOLDELSE** (GIL § 18, stk. 4 + forældelsesl. § 18, stk. 4):
   - Afbrydelse date: the date underretning om afgørelse om lønindeholdelse reaches the debtor.
   - **Varsel alone is NOT valid**: service shall return HTTP 422 if `afgoerelseRegistreret` is
     `false` or absent.
   - Operates at afgørelsesniveau: all fordringer covered by the afgørelse are afbrudt.
   - If lønindeholdelse is subsequently inactive for ≥ 1 year, a new frist starts from the
     first day of the inactivity anniversary.
   - Propagates to all fordringer in the same fordringskompleks (see FR-4).

3. **UDLAEG** (forældelsesl. § 18, stk. 1):
   - Afbrydelse date: the date the anmodning om udlæg is lodged.
   - Forgæves udlæg (insolvenserklæring) has the same afbrydelse effect as successful udlæg.
   - New frist: `eventDate + 3 years`; or `eventDate + 10 years` if the fordring has
     `retsgrundlag = SAERLIGT_RETSGRUNDLAG`.

4. **MODREGNING** (forældelsesl. § 18, stk. 4):
   - Afbrydelse date: the date modregning is executed.
   - New frist: `eventDate + 3 years`.

Each registration shall atomically update `ForaeldelseRecord.currentFristExpires` and log
the event with the calling identity, timestamp, and applicable legal reference.

### FR-4: Fordringskompleks membership and afbrydelse propagation (GIL § 18a, stk. 2 / G.A.2.4.2)

`FordringskompleksService` shall:
- Maintain fordringskompleks membership via a `FordringskompleksLink` table (fordringId,
  kompleksId, linkedAt).
- When an afbrydelse event is registered for any member fordring, propagate the event to all
  other members of the same kompleks within the same database transaction.
- Log each propagated event with its source fordringId, target fordringId, the propagation
  reason (`FORDRINGSKOMPLEKS_PROPAGATION`), and the legal reference (GIL § 18a, stk. 2).
- Partial propagation (some members updated, some not) is a failure condition (NFR-4).

Membership management:
- `POST /fordringskompleks` — create a new kompleks with initial members.
- `POST /fordringskompleks/{kompleksId}/members/{fordringId}` — add a member.
- `GET /fordringskompleks/{kompleksId}/members` — list all members.

### FR-5: Tillægsfrister (G.A.2.4.4.2)

When an internal opskrivning (G.A.2.3.4.4) is applied to a fordring, a 2-year tillægsfrist
shall be added to the running forældelsesfrist. The `ForaeldelseService` shall:
- Accept `POST /foraeldelse/{fordringId}/tillaegsfrist` with type `INTERN_OPSKRIVNING`.
- Compute `newFristExpires = max(currentFristExpires, eventDate) + 2 years`.
- Log the extension with date, extension type, and legal reference (G.A.2.4.4.2).
- Update `ForaeldelseRecord.currentFristExpires` atomically.

### FR-6: Forældelsesindsigelse workflow (G.A.2.4.6)

A debtor may formally assert that a fordring is forældet. The service shall support a
two-step caseworker workflow:

**Step 1 — Registration:**
- Endpoint: `POST /foraeldelse/{fordringId}/indsigelse`
- The fordring's status transitions to `INDSIGELSE_PENDING`.
- Inddrivelse activities on the fordring are flagged for caseworker review.
- The indsigelse is assigned a unique `indsigelsesId` and logged.

**Step 2 — Evaluation:**
- Endpoint: `PUT /foraeldelse/{fordringId}/indsigelse/{indsigelsesId}`
- Request body: `{ outcome: VALID | INVALID, rationale: string }`
- If `outcome = VALID`:
  - Fordring status transitions to `FORAELDET`.
  - Fordring lifecycle transitions to a terminal state (removed from inddrivelse).
  - The removal is logged with the caseworker identity and rationale.
- If `outcome = INVALID`:
  - Fordring status returns to `ACTIVE`.
  - The rejection rationale is stored for audit.
- All indsigelse outcomes are logged to the audit log (CLS) per NFR-2.

### FR-7: Caseworker portal visibility

The sagsbehandlerportal shall display, for each fordring under management, a
forældelsesstatus panel containing:
- **Current status:** `Aktiv` / `Forældet` / `Indsigelse under behandling`.
- **Forældelsesfrist udløber:** The computed next expiry date.
- **Udskydelse:** Whether the fordring is in an udskydelse window and the applicable date.
- **Afbrydelse history:** A chronological table with type, date, and legal reference for
  each registered afbrydelse event.
- **Fordringskompleks membership:** Whether the fordring is part of a kompleks and which
  other fordringer are members.
- **Indsigelse:** A button to register a forældelsesindsigelse; a form to evaluate a
  pending indsigelse; the outcome of any prior indsigelse.

The panel shall be accessible from the fordring detail view. Caseworkers with read-only
access shall see the panel but cannot register afbrydelse events or evaluate indsigelser.

---

## Non-functional requirements

- **NFR-1: Deterministic date arithmetic.** All forældelsesfrist calculations shall use
  `java.time.LocalDate` with no implicit timezone conversion. The service shall be testable
  with a fixed `java.time.Clock` injected as a Spring bean, enabling deterministic unit tests
  without date mocking.

- **NFR-2: Full audit trail.** Every afbrydelse event registration, fordringskompleks
  propagation, tillægsfrister application, indsigelse registration, and indsigelse evaluation
  shall be logged to the audit log (CLS) with: caseworker or system identity, timestamp,
  fordringId, event type, and the applicable legal reference (GIL/forældelsesl. section).

- **NFR-3: GDPR — no PII in forældelses tables.** All `ForaeldelseRecord`,
  `AfbrydelseEvent`, `TillaegsfristEvent`, `FordringskompleksLink`, and
  `ForaeldelseIndsigelse` entities shall reference the debtor only via `person_id` (UUID
  from Person Registry). No CPR, name, address, email, or other PII shall be stored in
  these tables or transmitted in API responses.

- **NFR-4: Transactional consistency.** Afbrydelse event registration and all fordringskompleks
  propagation events within the same request shall execute in a single database transaction.
  Partial propagation (some members updated, others not) shall cause the entire transaction
  to roll back.

- **NFR-5: Performance.** The `GET /foraeldelse/{fordringId}` endpoint shall respond within
  200 ms at p99 for a fordring with up to 50 afbrydelse events in its history.

---

## Constraints

- **GIL § 18a, stk. 1 / G.A.2.4.1:** Udskydelse is NOT an afbrydelse. The start-date
  postponement is computed once at fordring registration and stored as a non-mutable field.
  It cannot be modified by subsequent events.

- **SKM2015.718.ØLR / GIL § 18, stk. 4:** Lønindeholdelse varsel alone does NOT afbryde.
  The service must reject afbrydelse registration requests of type `LOENINDEHOLDELSE` where
  `afgoerelseRegistreret` is `false` or absent, returning HTTP 422.

- **Forældelsesl. § 18, stk. 1 / G.A.2.4.4.1:** Forgæves udlæg (insolvenserklæring) has
  the same afbrydelse effect as a successful udlæg. The service must not distinguish between
  them in terms of afbrydelse outcome or new frist calculation.

- **G.A.2.4.3:** The 3-year inddrivelse-forældelsesfrist applies to all fordringer under
  PSRM management, including those that had a longer frist before entering PSRM. The sole
  exception is udlæg on a fordring with `retsgrundlag = SAERLIGT_RETSGRUNDLAG`, which
  gives a new 10-year frist.

- **GIL § 18a, stk. 2 / G.A.2.4.2:** Fordringskompleks membership is determined under GIL
  § 18a, stk. 2. Membership shall not be inferred from obligationId alone without explicit
  GIL § 18a, stk. 2 linkage registered in `FordringskompleksLink`.

- **G.A.2.4.4.1 lønindeholdelse inactivity:** If lønindeholdelse has been inactive for
  ≥ 1 year, a new 3-year frist starts. The system must track lønindeholdelse activity state
  to trigger this reset at the correct point.

---

## Out of scope

- **Strafbare forhold (G.A.2.4.5):** Special forældelsesfrist rules for bøder and
  konfiskationsbeløb from criminal matters, including the forvandlingsstraf non-forældelse
  period. Deferred — requires court integration logic.
- **International forældelses rules:** Cross-border prescription under EU law or bilateral
  agreements.
- **Catala encoding:** Formal Catala specification of forældelsesl. §§ 3, 5, 18 rules is
  tracked in petition 070 (separate spike).
- **Automatic bortfald trigger:** Automated removal of forældet fordringer from inddrivelse
  is scoped to petition 065 (bortfald og afskrivning), which depends on this petition's
  data model.
- **Rentegodtgørelse:** Interest compensation related to retroactive prescription events is
  tracked in TB-039.

---

## Deliverables

| Deliverable | Path / Location |
|-------------|-----------------|
| `ForaeldelseRecord` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/ForaeldelseRecord.java` |
| `AfbrydelseEvent` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/AfbrydelseEvent.java` |
| `TillaegsfristEvent` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/TillaegsfristEvent.java` |
| `FordringskompleksLink` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/FordringskompleksLink.java` |
| `ForaeldelseIndsigelse` entity (JPA) | `opendebt-debt-service/src/main/java/.../entity/ForaeldelseIndsigelse.java` |
| `ForaeldelseService` (domain logic + date arithmetic) | `opendebt-debt-service/src/main/java/.../service/ForaeldelseService.java` |
| `FordringskompleksService` | `opendebt-debt-service/src/main/java/.../service/FordringskompleksService.java` |
| `ForaeldelseApiController` (REST) | `opendebt-debt-service/src/main/java/.../controller/ForaeldelseApiController.java` |
| `ForaeldelseStatusDto` | `opendebt-debt-service/src/main/java/.../dto/ForaeldelseStatusDto.java` |
| `AfbrydelseEventDto` (request/response) | `opendebt-debt-service/src/main/java/.../dto/AfbrydelseEventDto.java` |
| `TillaegsfristDto` | `opendebt-debt-service/src/main/java/.../dto/TillaegsfristDto.java` |
| `ForaeldelseIndsigelsesDto` | `opendebt-debt-service/src/main/java/.../dto/ForaeldelseIndsigelsesDto.java` |
| Sagsbehandlerportal forældelsesstatus panel | `opendebt-caseworker-portal/src/main/resources/templates/fordring/foraeldelse-status.html` |
| Sagsbehandlerportal indsigelse workflow form | `opendebt-caseworker-portal/src/main/resources/templates/fordring/foraeldelse-indsigelse.html` |
| Danish message bundle additions | `opendebt-caseworker-portal/src/main/resources/messages_da.properties` |
| English message bundle additions | `opendebt-caseworker-portal/src/main/resources/messages_en_GB.properties` |
| Liquibase migration (forældelses tables) | `opendebt-debt-service/src/main/resources/db/changelog/` |
| Gherkin feature file | `petitions/petition059-foraeldelse.feature` |

---

## Definition of Done

- `ForaeldelseService` calculates `currentFristExpires` correctly for all cases: PSRM
  ordinary, DMI/SAP38, særligt retsgrundlag, with and without afbrydelse events.
- Udskydelse rules (GIL § 18a, stk. 1) applied correctly at fordring registration; no
  PSRM fordring (from 19-11-2015) has a forældelsesfrist commencing before 2021-11-20.
- All four afbrydelse event types are registered with correct date logic and legal references.
- Lønindeholdelse varsel alone is rejected (HTTP 422); only confirmed afgørelse afbryder.
- Forgæves udlæg is treated identically to successful udlæg for afbrydelse purposes.
- Udlæg on særligt retsgrundlag fordring sets a new 10-year frist.
- Fordringskompleks afbrydelse propagation is atomic and logged with GIL § 18a, stk. 2 reference.
- Tillægsfrister (2-year extension) applied correctly on `INTERN_OPSKRIVNING` event.
- Forældelsesindsigelse workflow: register → evaluate (VALID removes from inddrivelse;
  INVALID returns to ACTIVE) — all steps audited.
- `GET /foraeldelse/{fordringId}` returns correct `ForaeldelseStatusDto` with full history.
- Sagsbehandlerportal displays forældelsesstatus panel with history and indsigelse controls.
- No CPR or PII in forældelses-service entities or API responses — only `person_id` UUID.
- All AC covered by at least one Gherkin scenario.
- `behave --dry-run` passes on `petitions/petition059-foraeldelse.feature`.
