# Petition 057: Dækningsrækkefølge — GIL § 4 payment application order (G.A.2.3.2)

## Summary

Formalise the mandatory order in which received payments are applied across a debtor's
outstanding fordringer and their cost components. This petition implements the full GIL § 4
rule engine in OpenDebt, replacing the two-level sketch introduced in petition 007 with a
complete, legally-grounded specification covering all priority tiers, within-category FIFO
ordering, the inddrivelsesindsats exception, opskrivningsfordring positioning, and the
interest-before-principal sequence. It also delivers a query API and a sagsbehandler portal
view for the resulting ordered list.

**Supersedes:** The dækningsrækkefølge description in petition 007 (high-level sketch only).  
**Depended on by:** Petition 059 (forældelse — prescription interruption depends on correct
ordering), petition 062 (pro-rata distribution across joint debtors).  
**G.A. snapshot:** v3.16 (2026-03-28).  
**Catala companion:** Petition 069 (separate petition — Tier A Catala encoding of GIL § 4).

---

## Context and motivation

Under gældsinddrivelsesloven § 4 (GIL § 4), Gældsstyrelsen is legally obligated to apply
received payments in a fixed, hierarchical order across a debtor's outstanding fordringer.
This order — the *dækningsrækkefølge* — is not a system configuration choice; it is mandated
law. Incorrect application:

- Distributes money to the wrong fordring or the wrong creditor.
- Creates legally invalid dækning records that must be reversed and replayed.
- Causes audit failures when the CLS log cannot confirm GIL § 4 compliance for each dækning.
- Exposes Gældsstyrelsen to liability under forvaltningslovens § 22 (obligation to
  document the basis of decisions).

Petition 007 described the inddrivelsesskridt model and provided a brief, two-level sketch
of priority ordering. That sketch was sufficient to model the data but does not implement
the rule. This petition specifies the full rule engine, including every sub-rule of GIL § 4,
stk. 1–4, so that:

1. Each payment application produces a deterministic, auditable result.
2. The result can be queried and explained, both programmatically and in the sagsbehandler
   portal.
3. The engine can be validated against the Catala companion (P069) once that spike
   concludes.

### Why now?

Petition 059 (forældelse — prescription interruption) and petition 062 (pro-rata
distribution) both assume a stable dækningsrækkefølge model. P057 must be specified and
implemented before those petitions enter implementation.

---

## Domain terms

| Danish | English | Definition |
|--------|---------|------------|
| Dækningsrækkefølge | Payment application order | The legally mandated sequence in which received payments are applied across a debtor's outstanding fordringer and their cost components |
| Dækning | Coverage / allocation | The act of applying a received payment (or part thereof) to a specific fordring or cost component |
| Fordring | Claim / debt | A debt owed to a public authority or private creditor and transferred to Gældsstyrelsen for inddrivelse |
| Inddrivelse | Debt recovery | The process by which Gældsstyrelsen recovers outstanding fordringer on behalf of creditors |
| Inddrivelsesindsats | Enforcement action | A specific debt recovery measure applied to a debtor (e.g. udlæg, lønindeholdelse, modregning) |
| Opskrivningsfordring | Write-up claim | A separate fordring submitted by a fordringshaver to increase the amount of an existing fordring under inddrivelse |
| Opkrævningsrenter | Collection-period interest | Interest accrued during the fordringshaver's collection period, before the fordring was transferred to inddrivelse |
| Inddrivelsesrenter | Recovery-period interest | Interest accrued during the inddrivelse period, calculated according to GIL § 9 |
| Øvrige renter | Other interest | Residual interest categories calculated by PSRM under GIL § 9, stk. 3, 1. or 3. pkt. |
| Betalingstidspunkt | Payment timestamp | The legally decisive moment when the debtor lost control of the funds (e.g. bank transfer initiated), from which dækning takes effect |
| Modtagelsesdato | Receipt date | The date a fordring was received at the inddrivelsessystem; used as the FIFO sort key for ordering fordringer within a priority category |
| FIFO | First In, First Out | Ordering rule: the fordring received earliest for inddrivelse is covered first within a priority category |
| GIL | Gældsinddrivelsesloven | Consolidated Act on Debt Recovery (lovbekendtgørelse om gælds­inddrivelse), the primary legal basis for PSRM and Gældsstyrelsen's inddrivelse activity |
| PSRM | Provenu System til Restanceinddrivelse | The IT system managing debt recovery for Gældsstyrelsen; OpenDebt is its open-source successor |
| RIM | Restanceinddrivelsesmyndighed | Gældsstyrelsen in its statutory capacity as the Danish debt recovery authority |
| Udlæg | Attachment / levy | A judicial enforcement measure (execution over debtor assets) under retsplejelovens § 507 |
| Bøder | Fines | Criminal fines subject to GIL § 4, stk. 1, nr. 2 priority |
| Tvangsbøder | Coercive fines | Administrative coercive fines added to the GIL § 4 second-priority category by lov nr. 288/2022 |
| Underholdsbidrag | Maintenance payments | Alimony and child support claims (third priority category, privatretlige before offentlige) |

---

## Legal basis

| Reference | Content |
|-----------|---------|
| GIL § 4, stk. 1 | Four priority categories for payment application: (1) rimelige omkostninger ved udenretlig inddrivelse i udlandet, (2) bøder, tvangsbøder og tilbagebetalingskrav, (3) underholdsbidrag, (4) andre fordringer |
| GIL § 4, stk. 2 | Within-category FIFO ordering by modtagelsesdato; interest-before-principal sequence within each fordring; earlier period before later period |
| GIL § 4, stk. 3 | Inddrivelsesindsats rule: payment from an indsats covers indsats-fordringer first; surplus covers same-indsats-type fordringer; udlæg exception (retsplejelovens § 507) |
| GIL § 4, stk. 4 | Dækningsrækkefølge determined at time of application; effect from betalingstidspunkt |
| GIL § 6a, stk. 1 og stk. 12 | Rimelige omkostninger ved udenretlig inddrivelse i udlandet — first-priority category definition |
| GIL § 9, stk. 1 og stk. 3 | Inddrivelsesrenter — calculation basis and sub-types defining the interest coverage sequence |
| GIL § 10b | Bøder, tvangsbøder og tilbagebetalingskrav — second-priority category definition |
| Gæld.bekendtg. § 4, stk. 3 | Interest ordering within each PSRM fordring: opkrævningsrenter first, then inddrivelsesrenter sub-types by source, then øvrige renter; principal last |
| Retsplejelovens § 507 | Udlæg payments may only cover udlæg fordringer — basis for the udlæg exception in FR-4 |
| Lov nr. 288/2022 | Added tvangsbøder to the GIL § 4, stk. 1, nr. 2 priority category |
| G.A.2.3.2 (v3.16, 2026-03-28) | G.A. formulation of the complete dækningsrækkefølge procedure, including opskrivningsfordring positioning and PSRM-specific rules |

---

## PSRM Reference Context

In PSRM, a payment (indbetaling) received for a debtor triggers a dækning run that assigns
the payment amount — in full or in part — to fordringer in the legally mandated sequence.
OpenDebt models this as a **dækningsrækkefølge rule engine** that:

1. Retrieves all active fordringer for the debtor.
2. Sorts them according to the GIL § 4 hierarchy (FR-1 through FR-5).
3. Applies the payment amount sequentially, from top to bottom, until the payment is exhausted
   or all fordringer are covered.
4. Records each dækning with full traceability: fordring ID, amount applied, component type
   (interest sub-type vs. principal), and the GIL § 4 rule applied.

Key PSRM-specific rules that this petition makes explicit:

- **Renter dækkes before the principal** of the same fordring (Gæld.bekendtg. § 4, stk. 3).
  The interest sequence within a fordring has five sub-tiers (see FR-3).
- **Opskrivningsfordringer are not independent fordringer** for ordering purposes. They are
  placed immediately after the fordring they extend, following the rules in FR-5.
- **Legacy modtagelsesdato** applies for fordringer received before 1 September 2013. For
  these, PSRM uses the registered modtagelsesdato stored in the system at the time of
  migration, not the FIFO timestamp derived from the overdragelse API call.

---

## Functional requirements

### FR-1: Priority categories — GIL § 4, stk. 1 rule engine

The rule engine shall apply received payments in the following four-tier priority order.
Only fordringer of a higher category are covered before any fordring of a lower category
is touched.

| Priority | Category | Legal basis |
|----------|----------|-------------|
| 1 | Rimelige omkostninger ved udenretlig inddrivelse i udlandet | GIL § 6a, stk. 1 og stk. 12 |
| 2 | Bøder, tvangsbøder og tilbagebetalingskrav | GIL § 10b; tvangsbøder added by lov nr. 288/2022 |
| 3 | Underholdsbidrag — privatretlige first, then offentlige | GIL § 4, stk. 1, nr. 3 |
| 4 | Andre fordringer (all remaining) | GIL § 4, stk. 1, nr. 4 |

The rule engine shall expose the assigned priority category as a named label in all API
responses and portal views so that the legal basis for each coverage decision is auditable.

### FR-2: Within-category FIFO ordering — GIL § 4, stk. 2

Within a priority category, fordringer shall be ordered by ascending modtagelsesdato (the
date the fordring was received at the inddrivelsessystem):

- The fordring received earliest is covered first.
- For fordringer received before 1 September 2013: the modtagelsesdato registered in the
  source system at the time of PSRM migration shall be used as the FIFO sort key, not the
  overdragelse API timestamp.
- Ties in modtagelsesdato within the same fordringshaver shall be broken by the internal
  system sequence number (ascending).

The FIFO sort key (modtagelsesdato + sequence) shall be stored on the fordring entity and
included in the dækningsrækkefølge API response so that it can be audited independently.

### FR-3: Interest ordering within each fordring — Gæld.bekendtg. § 4, stk. 3

For each fordring, the received payment shall be applied in the following sequence of
cost components before the principal (Hovedfordring) is touched:

| Sub-position | Component | Legal basis |
|--------------|-----------|-------------|
| 1 | Opkrævningsrenter (fordringshaver collection period) | GIL § 9 / Gæld.bekendtg. § 4, stk. 3 |
| 2 | Inddrivelsesrenter beregnet af fordringshaver (§ 9, stk. 3, 2. or 4. pkt.) | GIL § 9, stk. 3 |
| 3 | Inddrivelsesrenter paid before main fordring returned to fordringshaver (§ 9, stk. 1 or 3) | GIL § 9, stk. 1 og stk. 3 |
| 4 | Inddrivelsesrenter — standard (§ 9, stk. 1) | GIL § 9, stk. 1 |
| 5 | Øvrige renter beregnet af PSRM (§ 9, stk. 3, 1. or 3. pkt.) | GIL § 9, stk. 3 |
| 6 | Hovedfordring (principal) | GIL § 4, stk. 2 |

Within sub-positions 2–5, if multiple interest accrual periods exist, earlier periods shall
be covered before later periods. For the same period across multiple sub-positions, the
modtagelsessystem registration order shall break ties.

The rule engine shall produce a line-item allocation record for each component type covered,
referencing the sub-position label and legal basis.

### FR-4: Inddrivelsesindsats rule — GIL § 4, stk. 3

When a payment is received as a direct consequence of an inddrivelsesindsats (e.g., a
lønindeholdelse or udlæg realisation), the following special ordering applies:

1. The payment covers fordringer that belong to the triggering inddrivelsesindsats first,
   in the GIL § 4, stk. 1 priority order among those indsats-fordringer.
2. Any surplus remaining after those fordringer are fully covered is applied to other
   fordringer coverable by the same inddrivelsesindsats type, in normal GIL § 4 order.

**Udlæg exception (retsplejelovens § 507):** Payments realised through udlæg shall only
cover fordringer that are part of the udlægsforretning. They shall not flow to other
fordringer, even if surplus remains after the udlægs-fordringer are fully covered.

The payment record shall carry the inddrivelsesindsats type (`UDLAEG`, `LOENINDEHOLDELSE`,
`MODREGNING`, or `FRIVILLIG`) to enable correct rule branching at application time.

### FR-5: Opskrivningsfordring positioning

Opskrivningsfordringer shall be positioned in the dækningsrækkefølge as follows:

1. An opskrivningsfordring is placed **immediately after the fordring it extends** (its
   parent fordring) in the ordered list.
2. If the parent fordring has already been fully covered at the time of ordering, the
   opskrivningsfordring takes the position immediately after where the parent fordring
   would have appeared, applying the same FIFO sort key as the parent.
3. When multiple opskrivningsfordringer exist for the same parent fordring, they are ordered
   among themselves by FIFO (ascending modtagelsesdato of the opskrivningsfordring itself).
4. Inddrivelsesrenter accrued on an opskrivningsfordring are covered before the
   opskrivningsfordring's principal, following the same interest sequence defined in FR-3.

The rule engine shall link opskrivningsfordringer to their parent fordring via the
`opskrivningAfFordringId` reference stored on the fordring entity, so that the positioning
rule can be applied deterministically.

### FR-6: Timing — GIL § 4, stk. 4

The dækningsrækkefølge is determined at the **time of payment application**, not at the
time the payment was received. The following timing rules apply:

- If a new fordring arrives or an existing fordring changes status between payment receipt
  and application, the updated state is used for ordering.
- The dækning takes **legal effect from the betalingstidspunkt** — the moment the debtor
  lost control of the funds (e.g., the bank's posting timestamp, not the date PSRM
  processes the payment).
- The betalingstidspunkt shall be stored on each dækningsrecord and included in the
  audit log entry to allow retrospective verification.

The rule engine shall record both the application timestamp and the betalingstidspunkt
on every dækning event.

### FR-7: Payment application API

The payment-service shall expose the following endpoints:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/debtors/{debtorId}/daekningsraekkefoelge` | Returns the current ordered list of fordringer and cost components for a debtor, at an optional `asOf` query parameter date |
| `POST` | `/debtors/{debtorId}/daekningsraekkefoelge/simulate` | Simulates the application of a given payment amount; returns the allocation plan without persisting any changes |

The `GET` response shall include, for each position in the ordered list:
- `fordringId`, `fordringshaverId`, `kategori` (GIL § 4, stk. 1 priority label), `komponent`
  (principal / interest sub-type), `tilbaestaaendeBeloeb`, `modtagelsesdato`, `gilParagraf`
  (the GIL § 4 sub-rule justifying this position).

The `POST` simulation response shall additionally include a projected `daekningBeloeb` for
each position and a `fullyCovers` flag indicating whether the payment would exhaust the
position.

Both endpoints shall require OAuth2 scope `payment-service:read` and the caller must hold
a role that grants access to the debtor (sagsbehandler or system-to-system).

### FR-8: Sagsbehandler portal — dækningsrækkefølge view

The sagsbehandlerportal shall display the current dækningsrækkefølge for a debtor's
active fordringer as a ranked list, ordered exactly as the rule engine produces it.

For each entry in the list, the portal shall show:
- Rank (1-based position)
- Fordring reference and fordringshaver name
- GIL § 4, stk. 1 priority category label (translated, e.g. "Underholdsbidrag")
- Cost component type (e.g. "Opkrævningsrenter", "Inddivelsesrenter § 9 stk. 1", "Hovedfordring")
- Outstanding amount (tilbaaestaaendeBeloeb)
- Modtagelsesdato (FIFO sort key)
- Whether the entry is an opskrivningsfordring (and which parent fordring it extends)

The view shall be reachable from the debtor's case overview. It shall be read-only.
It shall use the `GET /debtors/{debtorId}/daekningsraekkefoelge` endpoint (FR-7).

---

## Non-functional requirements

### NFR-1: Determinism and auditability

The rule engine shall be **deterministic**: given the same set of fordringer, the same
payment amount, and the same betalingstidspunkt, it shall always produce the same ordered
list and the same allocation. Non-determinism (e.g. from unsorted sets or random tie-breaking)
is a compliance defect.

Every dækning event produced by the rule engine shall be logged to the Central Logging Service
(CLS) with the following structured fields:
- `fordringId`, `komponent`, `daekningBeloeb`, `betalingstidspunkt`, `applicationTimestamp`
- `gilParagraf` (e.g. `"GIL § 4, stk. 1, nr. 3"`)
- `prioritetKategori` (e.g. `"UNDERHOLDSBIDRAG_PRIVATRETLIG"`)
- `fifoSortKey` (ISO-8601 date + sequence number)

### NFR-2: Payment application log references GIL § 4

Every CLS audit log entry for a dækning shall include the full GIL § 4 sub-rule reference
(e.g. `"GIL § 4, stk. 2, opkrævningsrenter — sub-position 1 of 6"`) so that an auditor
can trace the allocation back to the statutory rule without requiring system access.

### NFR-3: No change to betalingsflow

This petition formalises the existing payment application logic; it does not alter the
externally observable betalingsflow (payment receipt, OCR matching, NemKonto settlement).
The rule engine is inserted **at the dækning step** of the existing flow as modelled in
petition 001 (OCR matching) and petition 052 (manual NemKonto payout). No API contracts
for upstream or downstream systems are changed.

---

## Constraints

- **GIL § 4, stk. 4 timing:** The ordering is always computed from the state of fordringer
  at application time. A fordring that arrives after betalingstidspunkt but before application
  is included in the ordering. Implementations that snapshot the fordringer at receipt time
  violate this rule.

- **Pre-2013 modtagelsesdato (GIL § 4, stk. 2):** Fordringer received before 1 September 2013
  use the modtagelsesdato stored at system migration, not a computed overdragelse timestamp.
  The fordring entity must store a `legacyModtagelsesdato` field to support this rule.

- **Udlæg surplus (retsplejelovens § 507):** Surplus from udlæg realisations cannot flow to
  non-udlæg fordringer. This is an absolute constraint, not a configurable behaviour.

- **Opskrivningsfordring ordering (G.A.2.3.2):** If the parent fordring of an
  opskrivningsfordring is already fully covered, the opskrivningsfordring's position is
  computed as if the parent were still present. It is not moved to the bottom of its
  priority category.

- **DMI legacy rules:** DMI used a different dækning logic during paralleldrift (GIL § 49).
  DMI-originated payments under the paralleldrift period are **not** subject to this rule
  engine. They remain governed by the legacy DMI logic. Out of scope for this petition.

---

## Deliverables

| # | Deliverable | Path / Location |
|---|-------------|-----------------|
| D-1 | `DaekningsRaekkefoeigen` rule engine (service class) | `opendebt-payment-service/src/main/java/.../service/DaekningsRaekkefoeigenService.java` |
| D-2 | Priority category enum | `opendebt-payment-service/src/main/java/.../domain/PrioritetKategori.java` |
| D-3 | Interest component enum (sub-positions 1–5 + principal) | `opendebt-payment-service/src/main/java/.../domain/RenteKomponent.java` |
| D-4 | `DaekningRecord` entity (audit fields incl. gilParagraf) | `opendebt-payment-service/src/main/java/.../domain/DaekningRecord.java` |
| D-5 | `DaekningsRaekkefoeigenController` (FR-7 endpoints) | `opendebt-payment-service/src/main/java/.../controller/DaekningsRaekkefoeigenController.java` |
| D-6 | OpenAPI spec for FR-7 endpoints | `opendebt-payment-service/src/main/resources/openapi/daekningsraekkefoelge.yaml` |
| D-7 | Sagsbehandler portal — dækningsrækkefølge view template | `opendebt-sagsbehandler-portal/src/main/resources/templates/debtor/daekningsraekkefoelge.html` |
| D-8 | Sagsbehandler portal — view controller | `opendebt-sagsbehandler-portal/src/main/java/.../controller/DaekningsRaekkefoeigenViewController.java` |
| D-9 | Liquibase migration (legacyModtagelsesdato field, DaekningRecord table) | `opendebt-payment-service/src/main/resources/db/changelog/` |
| D-10 | Danish i18n message bundle additions (priority labels, component labels) | `opendebt-sagsbehandler-portal/src/main/resources/messages_da.properties` |
| D-11 | English i18n message bundle additions | `opendebt-sagsbehandler-portal/src/main/resources/messages_en_GB.properties` |
| D-12 | Gherkin feature file | `petitions/petition057-daekningsraekkefoeigen.feature` |

---

## Out of scope

| Item | Reason |
|------|--------|
| DMI paralleldrift dækning logic (GIL § 49) | Different statutory rules; DMI mechanics are not replicated in PSRM |
| Catala encoding of GIL § 4 | Tracked in companion petition P069 (Tier A — separate petition) |
| Pro-rata distribution across joint debtors | Tracked in petition 062; depends on P057 ordering model |
| Forældelsesfrist interruption rules | Tracked in petition 059; depends on P057 for which fordring receives payment first |
| Automatic modregning triggering | Modelled in petition 007; FR-4 only governs ordering within an existing inddrivelsesindsats payment |
| Rentegodtgørelse (GIL § 18 l) | Tracked in TB-039 |
| Full retroactive timeline replay | Tracked in TB-038 |
| Betalingsflow changes (OCR matching, NemKonto) | NFR-3: this petition adds ordering inside the existing flow, not a new flow |

---

## Definition of Done

- [ ] `DaekningsRaekkefoeigenService` produces a deterministic ordered list for any input set
      of fordringer, validated by unit tests covering all four priority categories (FR-1)
- [ ] FIFO ordering by modtagelsesdato is implemented and tested, including the pre-2013
      legacyModtagelsesdato path (FR-2)
- [ ] All six interest sub-positions within a fordring are covered before principal (FR-3),
      with unit tests for each sub-position
- [ ] Inddrivelsesindsats rule implemented; udlæg exception prevents surplus from flowing to
      non-udlæg fordringer (FR-4)
- [ ] Opskrivningsfordringer positioned immediately after parent fordring; multiple
      opskrivningsfordringer for same parent ordered by FIFO (FR-5)
- [ ] Dækning records carry betalingstidspunkt and applicationTimestamp; ordering is computed
      from application-time state (FR-6)
- [ ] `GET /debtors/{debtorId}/daekningsraekkefoelge` returns ordered list with `gilParagraf`
      field for each position (FR-7)
- [ ] `POST /debtors/{debtorId}/daekningsraekkefoelge/simulate` returns allocation plan without
      persisting (FR-7)
- [ ] Sagsbehandler portal displays ordered list with GIL § 4 category labels and component
      types (FR-8)
- [ ] Every dækning event logged to CLS with `gilParagraf`, `prioritetKategori`, and
      `fifoSortKey` (NFR-1, NFR-2)
- [ ] All new i18n labels present in both DA and EN message bundles
- [ ] Liquibase migration adds `legacy_modtagelsesdato` column and `daekning_record` table
- [ ] `behave --dry-run` passes on `petitions/petition057-daekningsraekkefoeigen.feature`
- [ ] Architecture overview updated in `docs/architecture-overview.md`
