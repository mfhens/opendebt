# ADR 0031: Statutory Codes Are Defined as Enums, Not Configuration

## Status
Accepted

## Context

OpenDebt processes debt collection under Danish law (primarily *Lov om inddrivelse af gæld til det offentlige*, LIG). Several domain concepts map directly onto legal classifications: write-down reason codes (årsagskoder for nedskrivning), claim categories, action types, and similar controlled vocabularies are enumerated in statute or ministerial guidelines.

When implementing the write-up reason code dropdown (petition 034), the initial design included a per-creditor `allowedWriteUpReasonCodes` field in `CreditorAgreementDto`, following the pattern established for `allowedClaimTypes`, `allowedDebtorTypes`, and `allowedInterestRules`. This pattern is correct for *operational business configuration* — choices that differ by creditor due to their agreement with UFST or the nature of their claims portfolio.

Write-down reason codes are different in character. The three codes (NED_INDBETALING, NED_FEJL_OVERSENDELSE, NED_GRUNDLAG_AENDRET) enumerate the legally admissible grounds for a write-down as defined in gæld.bekendtg. § 7 stk. 2. They do not vary by creditor. They vary only when the law changes. (Note: the original write-up codes DINDB, OMPL, AFSK were RIM-internal and were removed from the portal in petition053; `WriteUpReasonCode.java` was deleted.)

The distinction matters because the two categories have different change management implications:

| | Operational configuration | Statutory codes |
|---|---|---|
| **Who decides the value?** | Business agreement / creditor | Legislature / ministerial order |
| **Change frequency** | Routine (months) | Rare (law amendments) |
| **Change process** | Admin UI, DB update | Code change + release + legal review |
| **Varies by creditor?** | Yes | No |
| **Audit trail needed?** | Yes, in DB | In changelog / release notes |

## Decision

Values that are defined by statute, PSRM specifications, or other legal/regulatory sources shall be represented as Java enums, not as database configuration or per-creditor agreement fields.

Concretely:

1. `WriteDownReasonCode` is a Java enum. It is the single source of truth for the allowed set of write-down reason codes.
2. The creditor portal presents all values from the enum in the write-down form. No per-creditor filtering is applied.
3. The controller validates submitted reason codes against `WriteDownReasonCode` directly — not against a database-stored or agreement-stored list.
4. When the law changes and a code must be added, removed, or renamed, the change is made in the enum, reviewed as a code change, and released. This ensures the change is traceable, testable, and subject to normal release governance.

**Note (petition053):** `WriteUpReasonCode` (which originally enumerated DINDB, OMPL, AFSK) was removed in commit `a93052f`. Those codes are RIM-internal (G.A.2.3.4.4) and must not appear in the creditor portal. `WriteDownReasonCode` (NED_INDBETALING, NED_FEJL_OVERSENDELSE, NED_GRUNDLAG_AENDRET) is now the primary statutory enum in the portal and in debt-service.

Fields of the form `allowedXxxCodes` in `CreditorAgreementDto` are reserved for *operational business configuration* only — cases where the set of permitted values genuinely varies by creditor agreement.

## Consequences

**Easier:**
- No risk of production data being out of sync with valid legal codes
- Invalid codes cannot be stored in the agreement table, because they are never configurable there
- Adding or removing a code requires a code review, which ensures legal awareness is captured
- Simpler controller and DTO: no fallback logic, no null checks, no parsing from stored strings
- Tests are straightforward: no need to mock agreement data for reason code assertions

**Harder:**
- A code change and release is required when statute changes (accepted: this is the correct process for a legal change)
- No runtime-only hotfix path for statutory codes (accepted: a legal change to an approved enumeration should not be a hotfix)

## Examples

The following are **operational configuration** (per-creditor, DB-stored, `CreditorAgreementDto` fields):
- `allowedClaimTypes` — not all creditors submit all claim categories
- `allowedDebtorTypes` — certain debt types apply only to specific creditor portfolios
- `allowedInterestRules` — interest method depends on the underlying statutory basis per creditor type

The following are **statutory codes** (enum-only):
- `WriteDownReasonCode` — NED_INDBETALING, NED_FEJL_OVERSENDELSE, NED_GRUNDLAG_AENDRET as defined in gæld.bekendtg. § 7 stk. 2 (portal and debt-service; introduced petition053)
- `ClaimAdjustmentType` — adjustment operation types tied to PSRM transaction semantics
- `PrioritetKategori` — 5 GIL § 4 priority categories (INDDRIVELSESRENTER, OPKRAEVNINGSRENTER, GEBYRER, AFDRAG, ANDRE) as defined in GIL § 4 stk. 1–4 (payment-service; introduced petition057)
- `RenteKomponent` — 6 interest sub-position codes (INDDRIVELSESRENTER_STK1, INDDRIVELSESRENTER_FORDRINGSHAVER, INDDRIVELSESRENTER_FOER_TILBAGEFOERSEL, OPKRAEVNINGSRENTER, OEVRIGE_RENTER_PSRM, INGEN) as defined in GIL § 4 stk. 1–4 (payment-service; introduced petition057)
- `InddrivelsesindsatsType` — 4 collection effort types (LOENINDEHOLDELSE, UDLAEG, BEGGE, INGEN) determining stk. 3 surplus routing under GIL § 10b and Retsplejelovens § 507 (payment-service; introduced petition057)

**Note (P054 Catala spike):** The P054 Catala compliance spike (branch `feature/p054-catala-spike`)
confirmed that the `WriteDownReasonCode` enum approach aligns naturally with Catala's enumeration
model. In the Catala encoding of G.A.1.4.4 (`catala/ga_1_4_4_nedskrivning.catala_da`),
`NED_INDBETALING`, `NED_FEJL_OVERSENDELSE`, and `NED_GRUNDLAG_AENDRET` are represented as distinct
members of a Catala scope enumeration, and `UGYLDIG_GRUND` is explicitly modeled as a rejected enum
value — confirming that the Java enum-only approach produces a clean one-to-one mapping with
Catala's formal type system. See `catala/SPIKE-REPORT.md` for full coverage details.
