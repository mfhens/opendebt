# Petition 006: Indsigelse and workflow blocking during dispute handling

## Summary

OpenDebt shall support `indsigelse` as a first-class business object linked to a specific `fordring` or `restance`. An indsigelse is registered and tracked, but **does not automatically suspend collection**. Per G.A.1.3.1 (gældsinddrivelsesloven), ordinary debtor objections have no opsættende virkning (suspensive effect) on inddrivelse as a rule. Collection is only suspended when a formal klage med opsættende virkning is received, which triggers a KLAG tilbagekald by the fordringshaver.

## Context and motivation

The begrebsmodel treats `indsigelse` as a separate reaction concept, not as a subtype of communication. That matters because an objection changes the legal and operational treatment of the claim.

Without an explicit objection model, OpenDebt cannot reliably express:

- that the debtor disputes the claim
- why collection progression has paused
- when collection may resume
- how the case and communication history relate to the objection

## Functional requirements

1. OpenDebt shall support registration of an `indsigelse` against a specific `fordring` or `restance`.
2. An `indsigelse` shall identify:
   - the relevant `skyldner`
   - the affected `fordring` or `restance`
   - the registration time
   - the stated reason or basis for objection
3. Registration of an `indsigelse` shall **not** automatically block or suspend inddrivelse. Per G.A.1.3.1, indsigelser om kravets eksistens og størrelse have no opsættende virkning on collection as a rule. The system shall continue collection progression unless a specific legal suspension trigger applies.
4. The only trigger that shall cause OpenDebt to suspend collection is a formal klage med opsættende virkning, communicated via a KLAG tilbagekald from the fordringshaver. In that case, collection shall be suspended and the claim marked accordingly.
5. While an active `indsigelse` exists, OpenDebt shall expose the objection on the claim for workflow visibility and caseworker review — but the claim remains in its current collection state.
6. At its discretion, a caseworker may choose to put collection in bero for a specific claim where a well-founded presumption of incorrectness exists. This shall be a manual action, not an automatic system rule.
7. OpenDebt shall support resolving an `indsigelse` with at least these high-level outcomes:
   - objection upheld
   - objection rejected
8. If the objection is upheld, the fordringshaver must handle correction or withdrawal via the normal tilbagekald/opskrivning/nedskrivning workflows. OpenDebt shall not automatically modify the claim.
9. If the objection is rejected, the claim continues normal collection without requiring any system action.
10. OpenDebt shall preserve the objection history for later case handling, legal review, and audit.

## Constraints and assumptions

- **G.A.1.3.1 (Indsigelser om kravets eksistens og størrelse):** "Skyldners indsigelser har som hovedregel ikke opsættende virkning i forhold til inddrivelsen af kravet." Indsigelser do not suspend collection. Only a formal klage med opsættende virkning — communicated via a KLAG tilbagekald — suspends inddrivelse.
- **G.A.2.2 (Klager):** Formal appeals go through different channels (Landsskatteretten, Skatteankestyrelsen, fogedretten). The distinction between a generic indsigelse and a formal klage med opsættende virkning must be maintained in the data model and UI. The klage framework distinguishes: (a) G.A.2.2.2 — klager to Landsskatteretten/Skatteankestyrelsen; (b) G.A.2.2.3 — klager to fogedretten. Each type carries different workflow implications for inddrivelse suspension. The KLAG årsagskode (tilbagekaldelse) covers klager med opsættende virkning per G.A.2.2; a future petition should address the full klage workflow covering both subtypes.
- This petition defines objection lifecycle and workflow effect, not every legal subtype of objection.
- This petition does not define final caseworker UI.
- This petition does not define detailed SLA rules for resolving objections.
- This petition assumes that later communication to the debtor about the objection outcome may be handled by the communication model.

## PSRM Reference Context

### PSRM KLAG withdrawal workflow

When a skyldner has a klage with opsættende virkning, the fordringshaver must tilbagekald the fordring with årsagskode **KLAG**:

- No virkningsdato is required — it is automatically set to bogføringsdato.
- Dækninger are **NOT** reversed; they are retained.
- Inddrivelsesrenter are calculated up to virkningsdato and returned upon tilbagekald.
- The fordring is **locked** after withdrawal.
- After the klage decision is reached:
  - **FEJL** (skyldner gets medhold): all dækninger are reversed/ophæves, inddrivelsesrenter nulstilles from modtagelsestidspunktet, and the fordring is permanently locked.
  - **GenindsendFordring**: the fordring can be resubmitted using the original fordrings-ID.

### PSRM HENS (henstand) withdrawal

When a fordringshaver grants the skyldner henstand, the tilbagekald workflow uses årsagskode **HENS**:

- Works identically to the KLAG workflow (auto virkningsdato, dækninger retained, renter calculated to virkningsdato).
- **Not available** for statsrefusion-fordringer.

### Genindsendelse after KLAG/HENS

After a KLAG or HENS tilbagekald, the fordring may be resubmitted using the **GenindsendFordring** function:

- The original fordrings-ID must be provided.
- Certain stamdata must match the original fordring: stiftelsesdato, forfaldsdato, and periode.
- The resubmitted fordring receives a **new fordrings-ID**.
- Forældelsesdato must be the newest available (from underretning or the fordringshaver's own afbrydelse).
- **FEJL-withdrawn claims CANNOT be resubmitted** via GenindsendFordring — they must be created as entirely new fordringer.

_Source: [Tilbagekald fordring](https://gaeldst.dk/fordringshaver/find-vejledning/regulering-af-fordringer/tilbagekald-fordring) · `docs/psrm-reference/04c-tilbagekald-fordring.md`_

## Out of scope

- Full appeals hierarchy beyond the objection step
- Detailed document templates for objection acknowledgment and resolution notices
- Detailed legal decision support rules
