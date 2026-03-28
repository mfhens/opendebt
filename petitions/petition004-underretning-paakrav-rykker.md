# Petition 004: Formalization of underretning, påkrav, and rykker

## Summary

OpenDebt shall model `underretning` as a common business object for formal communication to `skyldner` about a `fordring` or `restance`. `Påkrav` and `rykker` shall be represented as distinct specializations of `underretning`. The system shall record sender, recipient(s), channel, relation to the relevant claim, and delivery state.

## Context and motivation

The current solution uses letters and communication concepts informally, but the begrebsmodel requires a stricter distinction:

- `underretning` is the common concept
- `påkrav` is a specific payment demand
- `rykker` is a follow-up communication after non-payment

Without a shared model, OpenDebt risks scattering legal communication semantics across letters, payment flows, and case workflows without a consistent audit trail.

## Functional requirements

1. OpenDebt shall model `underretning` as a formal communication object linked to a specific `fordring` or `restance`.
2. OpenDebt shall distinguish at least these subtypes of `underretning`:
   - `påkrav`
   - `rykker`
3. Every `underretning` shall record:
   - the sending `fordringshaver` or sending authority
   - one or more recipient `skyldnere`
   - the related `fordring` or `restance`
   - the communication channel
   - the sending time
   - the delivery or processing state
4. OpenDebt shall support issuing a `påkrav` for a relevant `fordring`.
5. If a `påkrav` supports OCR-based payment handling, the `påkrav` shall carry the relevant `OCR-linje`.
6. OpenDebt shall support issuing a `rykker` when a `fordring` or `restance` has not been paid in time and a reminder is legally or operationally required.
7. A `rykker` shall be linked to the same `fordring` or `restance` as the underlying unpaid obligation.
8. OpenDebt shall preserve the communication history for each `underretning` so that later case handling and legal review can see what was sent and to whom.

## Constraints and assumptions

- This petition defines business semantics and audit needs, not final document template layout.
- This petition does not define every possible communication subtype beyond `påkrav` and `rykker`.
- This petition does not define channel-specific delivery integrations in detail.
- This petition assumes debtor identity is still handled through UUID references in person-registry.

## PSRM Reference Context

> **Note — Two distinct underretning flows:** Implementation must not conflate these:
> (a) **Skyldnerunderretning:** the fordringshaver's obligation to notify the debtor (skyldner) in writing before overdragelse til inddrivelse (GIL § 2, stk. 4 / G.A.1.3.2). This is directed at the debtor and is a pre-overdragelse ordensforskrift handled by the fordringshaver or opkræver — it is modelled in this petition as the skriftlig underretning subtype.
> (b) **Fordringshaverunderretninger:** Gældsstyrelsen's operational messages sent FROM the inddrivelse system TO fordringshavere (Afregning, Udligning, Allokering, Renter, Afskrivning, Tilbagesend). These are directed at the creditor's own system and track saldo movements, settlements, and lifecycle events during inddrivelse. These are NOT modelled as `underretning` subtypes in this petition — they are described below as reference context only.
> These two flows must be kept separate in the data model, UI, and implementation.

### PSRM underretningsmeddelelser

Gældsstyrelsen sends six types of notification (underretningsmeddelelser) to fordringshavere:

1. **Afregning** — monthly settlement, sent on the last business day of each month. Contains CPR/beløb/dato/fordring.
2. **Udligning** — daily notification when there is saldo movement on a fordring. Used to track saldi for nedskrivning.
3. **Allokering** — daily notification, similar to udligning but shows afdrag/hovedstol/renter breakdown. Mutually exclusive with udligning (a fordringshaver receives one or the other).
4. **Renter** — monthly tilskrevet renter or detailed daily renter statement (system-to-system only).
5. **Afskrivning** — sent when a fordring loses retskraft (forældelse, konkurs, dødsbo, gældssanering).
6. **Tilbagesend/Returnering** — sent when the creditor requests return of a fordring or Gældsstyrelsen returns a claim.

Key constraints:
- Notifications older than 3 months cannot be fetched.
- System-to-system: fetch via service in own debitorsystem.
- Portal users: fetch in Fordringshaverportalen.

### Skriftlig underretning requirement

- Written notification (skriftlig underretning) to the skyldner is required before overdragelse til inddrivelse.
- The underretning must be given by the fordringshaver or opkræver.
- All medhæftere (co-liable parties) need individual underretning.
- Underretning is also required before opskrivning (increase) of an existing fordring.

Source: https://gaeldst.dk/fordringshaver/find-vejledning/underretningsmeddelelser

## Out of scope

- Physical mail provider integration
- Digital Post transport details
- Detailed text template wording
- Detailed escalation rules after repeated reminders
