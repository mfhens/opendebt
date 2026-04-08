---
marp: true
theme: default
paginate: true
style: |
  /* ===== SKAT Design System — Marp theme ===========================
     Colors derived from skat-design-system/src/.../skat-tokens.css
     Fonts: Republic (headings) → Georgia fallback
             Academy Sans (body) → "Segoe UI" / Arial fallback
  ================================================================== */

  :root {
    --skat-navy:       #14143c;
    --skat-purple:     #434363;
    --skat-gray:       #72728a;
    --skat-silver:     #a1a1b1;
    --skat-mischka:    #d0d0d8;
    --skat-blue:       #2e99d9;
    --skat-blue-light: #e3ebf2;
    --skat-green:      #008139;
    --skat-green-bg:   #b2d9c4;
    --skat-red:        #c80000;
    --skat-red-bg:     #f5b4b9;
    --skat-yellow:     #ffbb16;
    --skat-yellow-bg:  #ffebb9;
    --skat-concrete:   #f2f2f2;
    --skat-text:       #2c2c2c;
  }

  section {
    background: #ffffff;
    color: var(--skat-text);
    font-family: "Segoe UI", "Arial", sans-serif;
    font-size: 17px;
    padding: 40px 52px;
  }

  section::after {
    font-size: 12px;
    color: var(--skat-silver);
  }

  /* --- Slide header bar ------------------------------------------- */
  section::before {
    content: "";
    display: block;
    position: absolute;
    top: 0; left: 0; right: 0;
    height: 6px;
    background: var(--skat-blue);
  }

  h1 {
    font-family: Georgia, "Times New Roman", serif;
    font-size: 2em;
    color: var(--skat-navy);
    border-bottom: 2px solid var(--skat-blue);
    padding-bottom: 8px;
    margin-bottom: 20px;
  }

  h2 {
    font-family: Georgia, "Times New Roman", serif;
    font-size: 1.55em;
    color: var(--skat-navy);
    margin-bottom: 14px;
  }

  h3 {
    font-family: Georgia, "Times New Roman", serif;
    color: var(--skat-purple);
    font-size: 1.1em;
  }

  a { color: var(--skat-blue); }

  strong { color: var(--skat-navy); }

  /* --- Lead slide (title / section divider) ----------------------- */
  section.lead {
    background: var(--skat-navy);
    color: #ffffff;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
    padding: 60px 64px;
  }

  section.lead::before { background: var(--skat-blue); }

  section.lead h1 {
    color: #ffffff;
    border-bottom-color: var(--skat-blue);
    font-size: 2.6em;
  }

  section.lead h3 {
    color: var(--skat-blue);
    font-family: "Segoe UI", Arial, sans-serif;
    font-weight: 400;
    font-size: 1.3em;
  }

  section.lead strong { color: #ffffff; }

  /* --- Tables ----------------------------------------------------- */
  table {
    font-size: 14px;
    width: 100%;
    border-collapse: collapse;
  }

  th {
    background: var(--skat-navy);
    color: #ffffff;
    padding: 8px 10px;
    font-weight: 600;
    text-align: left;
  }

  td {
    padding: 6px 10px;
    border-bottom: 1px solid var(--skat-concrete);
  }

  tr:nth-child(even) td {
    background: var(--skat-concrete);
  }

  /* --- Blockquote (callout box) ----------------------------------- */
  blockquote {
    border-left: 4px solid var(--skat-blue);
    background: var(--skat-blue-light);
    margin: 12px 0;
    padding: 10px 18px;
    border-radius: 3px;
    font-size: 0.95em;
  }

  /* --- Small-text appendix slide ---------------------------------- */
  section.small {
    font-size: 12px;
  }

  section.small table { font-size: 11px; }
  section.small th { padding: 5px 7px; }
  section.small td { padding: 4px 7px; }
---

<!-- _class: lead -->

# OpenDebt

### OpenDebt Status Update · April 2026

**1 April 2026 · Status as of 31 March 2026**

---

## Executive Summary

- **72 petitions** tracked across 20 delivery phases — **49 delivered (68%)**
- Phases 1–13 substantially complete; creditor portal, caseworker tools, and observability are live
- Active work: **G.A.2 payment rules** (Phase 18) and **Citizen self-service** (Phase 8, unblocked)
- **Critical risk:** DR infrastructure not provisioned — `ENCRYPTION_KEY` unprotected in production (TB-DR-001)
- Next milestone: launch Phase 8 citizen self-service and close G.A.2 prescription rules (P059)

---

## Delivery Progress

| Phase | Total | ✅ Done | 🔧 Active | ⏳ Planned |
|---|:---:|:---:|:---:|:---:|
| Ph. 0 — Foundation | 2 | 2 | — | — |
| Ph. 1 — Core Domain | 4 | 4 | — | — |
| Ph. 2 — Creditor Channels & UI | 5 | 5 | — | — |
| Ph. 3 — Collection Model | 5 | 5 | — | — |
| Ph. 4 — Claim Validation Rules | 4 | 4 | — | — |
| Ph. 5–7 — Quality, Auth & Portals | 6 | 6 | — | — |
| Ph. 8 — Citizen Self-Service | 3 | — | — | 3 |
| Ph. 9 — Creditor Portal Features | 10 | 9 | — | — |
| Ph. 10–13 — Caseworker, Config & RBAC | 12 | 10 | — | 2 |
| Ph. 15–17 — immudb, NemKonto & G.A.1 | 5 | — | — | 5 |
| Ph. 18 — G.A.2 Payment Rules | 7 | 4 | 1 | 2 |
| Ph. 19–20 — G.A.3 Inddrivelse Tools | 10 | — | — | 10 |

**49 of 72 petitions complete (68%)** · 1 petition superseded (P034 → P053)

---

## Critical Path

All 12 critical path petitions are **✅ complete**.

| Petition | Title | Status |
|---|---|---|
| P008 | Fordringshaver data model | ✅ Implemented |
| P009 | Fordringshaver master data service | ✅ Validated |
| P010 | Channel binding and access resolution | ✅ Implemented |
| P012 | Fordringshaverportal BFF | ✅ Validated |
| P015–P018 | Claim validation rules (4 petitions) | ✅ Implemented |
| P019 | Legacy SOAP endpoints | ✅ Implemented |
| P029 | Claims in recovery list | ✅ Implemented |
| P033 | Claim creation wizard | ✅ Implemented |
| P038 | Dashboard, navigation & settings | ✅ Implemented |

> Critical path is clear. No blockers on any foundational dependency.

---

## Current Phase Detail — Phase 18: G.A.2 Payment Rules

The primary active development front. Two Catala spikes now complete; implementation sprint begins.

| Petition | Title | Status | Next Step |
|---|---|---|---|
| P057 | Dækningsrækkefølge GIL § 4 | ✅ Implemented | Wire DEP-1 endpoint (see blockers) |
| P058 | Modregning + korrektionspulje | ✅ Implemented | Integration tests with P057 |
| P069 | Catala spike — GIL § 4 oracle | 🔧 In Progress | Run `catala typecheck` in CI (AC-18) |
| P070 | Catala spike — forældelse oracle | ✅ Implemented | Wire as oracle for P059 tests |
| P059 | Forældelse (prescription rules) | ⏳ Not Started | Sprint now unblocked by P070 |
| P060 | Retskraftvurdering | ⏳ Not Started | After P059 prescription model |

<br>

**Phase 8 (Citizen Self-Service) is fully unblocked** — P026–P028 can start immediately; all dependencies (P022, P024, P025) are implemented.

---

## Risks & Blockers

| ID | Title | Severity | What is blocked |
|---|---|---|---|
| **TB-DR-001** | DR infrastructure not provisioned | 🔴 Critical | Production `ENCRYPTION_KEY` is not in Azure Key Vault — loss = permanent PII data loss. k8s manifests ready; no infra team allocation yet. |
| **DEP-1** | `GET /internal/debtors/{id}/fordringer/active` missing | 🟡 High | P057 (dækningsrækkefølge) passes unit tests but cannot run live integration with real debt data until this debt-service endpoint exists. |
| **TB-004** | EDIFACT pipeline blocked | 🟡 High | Smooks CREMUL/DEBMUL path in integration-gateway is a stub — waiting for real CREMUL sample files from SKB/Bankdata. |
| **TB-028-a** | immudb platform validation pending | 🟡 High | P051 (immudb production hardening) cannot start until UFST HDP platform team validates immudb support. |

---

## Decisions Needed

Three open decisions require SteerCo input:

**1. Formally resource the Catala G.A. encoding programme**
- **Why:** ADR-0032 accepted Catala as the compliance verification layer. Two spikes (P069 Go, P070 Go) validate the approach. Phases 18–20 plan 4 more Catala companions. This is a multi-quarter architectural commitment requiring dedicated resourcing.
- **Result requested:** Endorse G.A. encoding roadmap (P057→P072) and allocate 1 FTE Catala analyst for Phases 18–19.

**2. Prioritise DR infrastructure provisioning (TB-DR-001)**
- **Why:** ADR-0028 defines a complete backup and DR strategy (RTO/RPO 4h). All k8s manifests are ready. The critical gap is `ENCRYPTION_KEY` not stored in Azure Key Vault — a production deployment without this fix risks permanent PII data loss.
- **Result requested:** Allocate infra team to TB-DR-001-a/b/c before the next production release.

**3. Commit UFST HDP platform validation timeline for immudb (TB-028-a)**
- **Why:** P051 (immudb ledger integrity production hardening — ADR-0029) is fully specified and ready to implement, but is blocked on a platform-side validation. Without a platform commitment, the cryptographic audit trail for financial ledger entries cannot be hardened for production.
- **Result requested:** UFST HDP platform team to provide a signed-off validation date within 30 days.

---

## Recent Architectural Decisions

Five ADRs accepted since the programme baseline (Q1 2026):

| ADR | Title | Status | Decision |
|---|---|---|---|
| ADR-0028 | Backup and Disaster Recovery | Accepted | pgBackRest + Azure Blob WAL archiving; RTO/RPO 4h; Velero PVC snapshots |
| ADR-0029 | immudb for Financial Ledger Integrity | Accepted | Cryptographic dual-write on all ledger entries; conditional on UFST HDP validation |
| ADR-0030 | SOAP Legacy Gateway | Accepted | Smooks-based OIO/SKAT dual-namespace SOAP adapter in integration-gateway |
| ADR-0031 | Statutory Codes as Enums | Accepted | Legal classifications (reason codes, art types) are Java enums, not DB config |
| ADR-0032 | Catala as Formal Compliance Layer | Accepted | Catala encodes G.A. Inddrivelse rules; CI typecheck enforces formal correctness |

> ADR-0032 is the architectural foundation for the Catala resourcing decision (Decision 1).

---

## Technical Backlog — Key Items

| ID | Title | Priority | Status |
|---|---|---|---|
| TB-DR-001 | Provision DR infrastructure (ENCRYPTION_KEY, pgBackRest, hot standby) | 🔴 Critical | Not started |
| TB-002 | Enable CLS audit integration for production | 🟡 High | Not started — pending mTLS provisioning |
| TB-004 | Wire Smooks EDIFACT pipeline | 🟡 High | 🚫 Blocked (CREMUL samples) |
| TB-008 | Replace readiness validation stub with Drools call | 🟡 High | Not started (P015 is done — can proceed) |
| TB-012 | Wrap payment matching in saga/outbox pattern | 🟡 High | Not started |
| TB-019 | Address JaCoCo coverage overrides | 🟡 Medium | 🔧 In progress |
| TB-022 | Wire case-service workflow delegates | 🟡 Medium | Not started |
| TB-028-a | immudb UFST HDP platform validation | 🟡 High | Awaiting platform team |
| DEP-1 | debt-service fordringer/active endpoint | 🟡 High | Not started (blocks P057 live integration) |

---

## Next Steps

1. **Activate Phase 8** — launch citizen self-service sprint (P026–P028); all dependencies implemented
2. **Register DEP-1** as a technical backlog item and assign to debt-service team; unblocks P057 live integration
3. **Allocate infra capacity** to TB-DR-001-a (ENCRYPTION_KEY → Azure Key Vault) before next production release
4. **Obtain UFST HDP commitment** on immudb validation timeline (TB-028-a) — gates P051
5. **Begin P059 sprint** (forældelse prescription rules) with P070 Catala oracle as acceptance test reference

---

<!-- _class: lead -->

# Appendix

### Full Petition Status

---

<!-- _class: small -->

## Appendix — Full Petition Status (Phases 0–9)

| ID | Title | Phase | Status |
|---|---|---|---|
| P001 | OCR-based payment matching | 0 | ✅ Implemented |
| P002 | Creditor creation of a new fordring | 0 | ✅ Implemented |
| P003 | Fordring, restance, and overdragelse formalization | 1 | ✅ Implemented |
| P008 | Fordringshaver data model | 1 | ✅ Implemented |
| P009 | Fordringshaver master data service | 1 | ✅ Validated |
| P010 | Channel binding and access resolution | 1 | ✅ Implemented |
| P011 | M2M ingress via integration-gateway | 2 | ✅ Implemented |
| P012 | Fordringshaverportal BFF | 2 | ✅ Validated |
| P013 | UI webtilgaengelighed compliance | 2 | ✅ Validated |
| P014 | Accessibility statements | 2 | ✅ Validated |
| P019 | Legacy SOAP endpoints | 2 | ✅ Implemented |
| P004 | Underretning, paakrav og rykker | 3 | ✅ Implemented |
| P005 | Haeftelse for multiple skyldnere | 3 | ✅ Implemented |
| P006 | Indsigelse workflow blockering | 3 | ✅ Implemented |
| P007 | Inddrivelsesskridt (modregning, lønindeholdelse, udlæg) | 3 | ✅ Implemented |
| P053 | Opskrivning og nedskrivning (full G.A. spec) | 3 | ✅ Implemented |
| P015 | Core claim validation rules | 4 | ✅ Implemented |
| P016 | Claimant authorization rules | 4 | ✅ Implemented |
| P017 | Claim lifecycle and reference rules | 4 | ✅ Implemented |
| P018 | Claim content validation rules | 4 | ✅ Implemented |
| P020 | OpenTelemetry observability | 5 | ✅ Validated |
| P021 | i18n infrastructure | 5 | ✅ Validated |
| P022 | Citizen portal landing page | 6 | ✅ Implemented |
| P023 | Person Registry CPR lookup API | 7 | ✅ Implemented |
| P024 | Citizen debt summary endpoint | 7 | ✅ Implemented |
| P025 | MitID/TastSelv OAuth2 browser flow | 7 | ✅ Implemented |
| P026 | Citizen debt overview page | 8 | ⏳ Not Started |
| P027 | Citizen payment initiation | 8 | ⏳ Not Started |
| P028 | Digital Post letter retrieval | 8 | ⏳ Not Started |
| P029 | Claims in recovery list | 9 | ✅ Implemented |
| P030 | Claim detail view | 9 | ✅ Implemented |
| P031 | Claims in hearing | 9 | ✅ Implemented |
| P032 | Rejected claims | 9 | ✅ Implemented |
| P033 | Claim creation wizard | 9 | ✅ Implemented |
| P034 | Write-up and write-down (basic) | 9 | ~~Superseded → P053~~ |
| P035 | Notifications search and download | 9 | ✅ Implemented |
| P036 | Reconciliation | 9 | ✅ Implemented |
| P037 | Monthly reports | 9 | ✅ Implemented |
| P038 | Dashboard, navigation & settings | 9 | ✅ Implemented |

---

<!-- _class: small -->

## Appendix — Full Petition Status (Phases 10–20)

| ID | Title | Phase | Status |
|---|---|---|---|
| P039 | Crossing transactions with interest recalculation | 10 | ✅ Implemented |
| P040 | Ledger query API and posteringslog | 10 | ✅ Implemented |
| P041 | Caseworker portal demo mode | 10 | ✅ Implemented |
| P042 | OIO Sag v2.0 case data model | 10 | ⏳ Not Started |
| P043 | Batch lifecycle transitions and interest accrual | 10 | ✅ Implemented |
| P044 | MkDocs documentation site | 11 | ✅ Implemented |
| P045 | Multi-regime interest and fee compliance | 12 | ✅ Implemented |
| P046 | Versioned business configuration | 12 | ✅ Implemented |
| P047 | Configuration administration UI | 12 | ✅ Implemented |
| P050 | Unified case timeline UI | 12 | ✅ Implemented |
| P048 | Role-based data access control hardening | 13 | ✅ Implemented |
| P049 | Case handler assignment and workload management | 13 | ⏳ Not Started |
| P051 | immudb ledger integrity production hardening | 15 | ⏳ Not Started (blocked TB-028-a) |
| P052 | Manual NemKonto payout with 4-eyes approval | 16 | ⏳ Not Started |
| P054 | Catala spike — G.A.1.4.3/1.4.4 | 18 | ✅ Implemented |
| P055 | Suspension pga. datafejl og tilbagekaldelse | 17 | ⏳ Not Started |
| P056 | Ikkeinddrivelsesparate fordringer og ophør | 17 | ⏳ Not Started |
| P057 | Dækningsrækkefølge GIL § 4 | 18 | ✅ Implemented |
| P058 | Modregning + korrektionspulje G.A.2.3.3–2.3.4 | 18 | ✅ Implemented |
| P059 | Forældelse (prescription rules) G.A.2.4 | 18 | ⏳ Not Started |
| P060 | Retskraftvurdering | 18 | ⏳ Not Started |
| P069 | Catala spike — Dækningsrækkefølge GIL § 4 oracle | 18 | 🔧 In Progress |
| P070 | Catala spike — Forældelse oracle | 18 | ✅ Implemented |
| P061 | Afdragsordninger GIL § 11 | 19 | ⏳ Not Started |
| P062 | Lønindeholdelse (full specification) | 19 | ⏳ Not Started |
| P063 | Henstand (deferral) | 19 | ⏳ Not Started |
| P064 | Ægtefællehæftelse | 19 | ⏳ Not Started |
| P065 | Afskrivning og bortfald | 19 | ⏳ Not Started |
| P066 | Udlæg — skærpet inddrivelse workflow | 20 | ⏳ Not Started |
| P067 | Særlige inddrivelsesværktøjer | 20 | ⏳ Not Started |
| P068 | Inddrivelse af særlige fordringstyper | 20 | ⏳ Not Started |
| P071 | Catala spike — Afdragsordninger GIL § 11 | 19 | ⏳ Not Started |
| P072 | Catala spike — Lønindeholdelsesprocent GIL § 14 | 19 | ⏳ Not Started |
