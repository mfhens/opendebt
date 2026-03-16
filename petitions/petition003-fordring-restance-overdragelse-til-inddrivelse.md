# Petition 003: Formalization of fordring, restance, and overdragelse til inddrivelse

## Summary

OpenDebt shall distinguish formally between `fordring`, `restance`, and `overdragelse til inddrivelse`. A submitted or created claim starts as a `fordring` with a `kravgrundlag` and `betalingsfrist`. If the betalingsfrist is exceeded without timely payment, the claim becomes a `restance`. Only a `restance` may be transferred to the `restanceinddrivelsesmyndighed`, and that transfer shall be recorded as an explicit business action with audit information.

## Context and motivation

The current codebase represents most claim semantics through `DebtEntity`, but the begrebsmodel requires a clearer business distinction:

- a `fordring` exists before it is overdue
- a `restance` is an overdue `fordring`
- `overdragelse til inddrivelse` is a separate legal and operational action

Without that distinction, OpenDebt cannot correctly express when a claim is merely registered, when it has become overdue, and when it is lawfully handed over for public collection.

## Functional requirements

1. OpenDebt shall represent a `fordring` as a claim with at least:
   - a `kravgrundlag`
   - a `betalingsfrist`
   - one `fordringshaver`
   - one or more `skyldnere`
2. A `fordring` shall remain a `fordring` until its betalingsfrist is exceeded without sufficient payment.
3. When the betalingsfrist is exceeded and the claim is not fully paid, OpenDebt shall classify the claim as a `restance`.
4. If a claim is fully paid before or by the betalingsfrist, OpenDebt shall not classify it as a `restance`.
5. OpenDebt shall support an explicit `overdragelse til inddrivelse` action performed by a `fordringshaver` for a `restance`.
6. An `overdragelse til inddrivelse` shall be allowed only for a `restance`, not for a non-overdue `fordring`.
7. When a `restance` is transferred to collection, OpenDebt shall record:
   - the transferred `restance`
   - the transferring `fordringshaver`
   - the receiving `restanceinddrivelsesmyndighed`
   - the tidspunkt for transfer
8. When a `restance` is transferred to collection, OpenDebt shall make it eligible for subsequent `inddrivelsesskridt` and case handling.

## Constraints and assumptions

- This petition defines the business distinction and lifecycle, not the final persistence strategy.
- This petition does not define the full attribute set for `kravgrundlag`.
- This petition does not define the detailed legal rules for whether a specific `restance` may be transferred beyond the overdue-state requirement.
- This petition assumes payment evaluation already exists and can determine whether the claim is fully paid.

## PSRM Reference Context

The following reference material from Gældsstyrelsen's PSRM documentation provides concrete domain constraints that inform the implementation of restance and overdragelse til inddrivelse in OpenDebt.

**Sources:**
- [Generelle krav til fordringer](https://gaeldst.dk/fordringshaver/find-vejledning/generelle-krav-til-fordringer)
- [Renteregler](https://gaeldst.dk/fordringshaver/find-vejledning/renteregler)

### PSRM pre-conditions for overdragelse

Before a restance may be overdraget til inddrivelse, PSRM requires:

1. **Sidste rettidige betalingsdato (SRB) must have expired** — PSRM rejects fordringer where SRB has not yet passed
2. **Sædvanlig rykkerprocedure** must have been completed without result (forgæves)
3. **Skriftlig underretning** must have been given to each skyldner individually before overdragelse — may be embedded in the opkrævning/rykkerskrivelse; must be written and given by fordringshaver or the entity performing opkrævning
4. **All medhæftere must be reported simultaneously** — each medhæfter receives individual underretning, but indberetning of all must happen at the same time

**Undtagelser fra underretningskrav:** When it is not possible to notify (e.g., not registered in folkeregister and exempt from Digital Post), udlæg without notification, konkursbegæring for economic crime, and arrestation.

### Claim separation rules

PSRM enforces strict separation of fordringer:

- **Hovedfordringer** and **"rente og lignende ydelse"** (rente, provisioner, gebyrer) must be submitted as **separate fordringer** in their correct fordringstype
- "Rente og lignende ydelse" includes kredit/morarente, provisioner, and gebyrer (rykker, PBS) but does NOT include inddrivelsesomkostninger
- Each fordring must be **submitted individually** — two or more fordringer must NOT be aggregated
- Fordringer with **different lovgrundlag must not be mixed** — this affects the forældelsesfrist
- Underfordringer (renter) must reference the hovedfordring they were calculated from
- If the hovedfordring is already paid, a **0-fordring** (saldo 0 kr.) must be submitted first, then the renter related to it

### Post-overdragelse lifecycle

Once a restance is overdraget and accepted by Gældsstyrelsen:

1. **Inddrivelsesrente tilskrives** from the 1st of the month following modtagelse — the rate is **5.75% per year** (as of 2026), calculated as simpel dag-til-dag rente
2. **Fordringshaver receives underretningsmeddelelser** for all handlinger performed on the fordring during inddrivelse
3. The claim becomes eligible for **inddrivelsesskridt**, including:
   - **Afdragsordning** — instalment arrangements
   - **Modregning** — offsetting against amounts owed to the skyldner
   - **Lønindeholdelse** — wage garnishment
   - **Udlæg** — enforcement/seizure
4. The kvittering returned at submission contains a **fordrings-ID**, any **hæftelsesforhold** and **AKR-nummer**, and a slutstatus of **UDFØRT**, **AFVIST**, or **HØRING**

## Out of scope

- Detailed modelling of `hæftelse`
- Detailed handling of `indsigelse`
- Detailed implementation of `inddrivelsesskridt`
- Detailed authority integration flows after transfer
