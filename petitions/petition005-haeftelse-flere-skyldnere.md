# Petition 005: Explicit hæftelse and support for multiple skyldnere

## Summary

OpenDebt shall model the relation between `fordring` and `skyldner` through an explicit `hæftelse` concept. A `fordring` may be linked to one or more `skyldnere`, and each relation shall state the relevant liability type. OpenDebt shall support at least `enehæftelse`, `solidarisk hæftelse`, and `delt hæftelse`.

## Context and motivation

The current codebase assumes a single debtor reference on a debt-like object. That is too crude for the begrebsmodel. The business needs an explicit liability model because:

- one claim may have more than one liable party
- different liable parties may have different liability forms
- communication, case handling, and collection logic depend on who is liable and how

Without `hæftelse`, OpenDebt cannot represent several central public-sector claim situations correctly.

## Functional requirements

1. OpenDebt shall represent the relationship between a `fordring` and a `skyldner` through an explicit `hæftelse` object.
2. A `fordring` shall be allowed to have one or more `hæftelser`.
3. Each `hæftelse` shall relate exactly one `fordring` to exactly one `skyldner`.
4. OpenDebt shall support at least these liability types:
   - `enehæftelse`
   - `solidarisk hæftelse`
   - `delt hæftelse`
5. A `fordring` with one liable party shall be expressible as `enehæftelse`.
6. A `fordring` with multiple jointly liable parties shall be expressible as `solidarisk hæftelse`.
7. A `fordring` with multiple parties responsible for defined portions shall be expressible as `delt hæftelse`.
8. When OpenDebt models `delt hæftelse`, the system shall preserve the relevant share or amount for each liable relation.
9. Communication and collection handling shall be able to determine which `skyldnere` are relevant from the registered `hæftelser`.

## Constraints and assumptions

- This petition establishes the liability structure but does not define every legal variant beyond the three listed forms.
- **G.A.1.3.3 (Dual-phase constraint):** The same fordring cannot simultaneously be under opkrævning hos fordringshaver AND under inddrivelse hos restanceinddrivelsesmyndigheden. If a fordring with multiple debtors is under inddrivelse for one debtor, the fordringshaver cannot simultaneously exercise opkrævning-phase rights (e.g., own modregning) against another debtor on the same claim. OpenDebt must not allow a state where the same fordring has active opkrævning for one hæftelse and active inddrivelse for another.
- **Delt hæftelse scope:** PSRM only recognises solidarisk hæftelse in the inddrivelse system. For delt hæftelse, the fordringshaver must split the fordring into separate fordringer before submission. OpenDebt may model delt hæftelse for opkrævning-phase tracking, but the inddrivelse layer works on post-split fordringer only.
- **Hæftelsesstruktur changes on active claims:** A fordring that is under inddrivelse must be tilbagekaldt before its hæftelsesstruktur can be changed (G.A.1.3.3 + G.A.1.3.5). OpenDebt must not allow modification of hæftelse on a fordring in active inddrivelse without a preceding tilbagekald.
- This petition does not define final UI behaviour for presenting multiple liable parties.
- This petition does not define the detailed enforcement allocation rules for payments across solidarity relationships.
- Person and organization identity remain stored through technical UUID references only.

## PSRM Reference Context

### PSRM solidarisk hæftelse

- PSRM only supports solidarisk hæftelse (joint and several liability).
- For non-solidary liability (e.g. delt hæftelse): the fordringshaver must split the fordring themselves before submission.
- Sædvanlig rykkerprocedure and individual underretning is required for each skyldner.

### Adding/removing skyldnere

- **To add skyldner(e):** tilbagekald the fordring with årsagskode "HAFT", notify all parties, then resubmit.
- Dækninger (payments already applied) are NOT reversed; inddrivelsesrenter are returned.
- Use REINDGI fordringstype for re-submitting collection interest (inddrivelsesrenter).
- Only the originally reported skyldnere hæfte for inddrivelsesrenter (exception: I/S partnerships).
- **To remove skyldner(e):** contact Fordringshaversupport.
- A resubmitted fordring gets a new plads i dækningsrækkefølge (new modtagelsestidspunkt).

### I/S and PEF rules

- **I/S (Interessentskab):** submit the CVR for the partnership AND the CPR/CVR for all liable interessenter at the time of debt creation.
- **PEF (Personligt ejet firma):** submit CVR if the firm is active, CPR if the firm has ceased.
- PSRM automatically adds the CPR for a PEF owner after submission.

Sources:
- https://gaeldst.dk/fordringshaver/find-vejledning/tilfoej-eller-fjern-skyldner-paa-en-fordring
- https://gaeldst.dk/fordringshaver/find-vejledning/is-og-pef-skyldnere

## Out of scope

- Detailed payment allocation rules between multiple liable parties
- Full legal edge-case catalogue for liability changes
- Detailed debtor self-service UX
