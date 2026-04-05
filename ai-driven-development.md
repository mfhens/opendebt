---
marp: true
theme: ey
paginate: true
---


<!-- _class: lead -->

# AI-Driven Software Development

### From Vibe to Enterprise Grade

**Go Home Meeting · March 2026**

---

## The Spectrum of AI Development

<br>

| | **Vibe Coding** | **Enterprise AI Dev** |
|---|---|---|
| Prompt style | One-liner | Structured petition |
| Requirements | "Make it cool" | BDD scenarios + outcome contract |
| Tests | ¿Qué? | TDD — failing first |
| Audience | You, maybe a friend | 1,200 public institutions |
| Stakes | Bragging rights | €600M in public debt |
| Agent role | Autocomplete | Orchestrated pipeline |
| **Human role** | **Writes the code** | **Governs the process** |

<br>

> Both are real. Both are powered by AI. The discipline is what changes.

---

<!-- _class: lead -->

# 🕹️ Act I: Vibe Coding

### *"Just build me a Space Invaders game"*

---

## The One-Liner Prompt

<br>

```
Build a Space Invaders game in HTML/JS.
The player ship shoots upward.
Aliens move side to side and descend.
Score counter. Game over screen.
```

<br>

**What happens next?** The AI:
1. Generates ~300 lines of HTML + Canvas API code
2. It runs. First try. Looks great.
3. You show it to someone → they are impressed

<br>

### ✅ This is genuinely useful
Prototyping · Learning · Demos · Personal tools

---

## What Vibe Coding Gives You

<br>

**🟢 The Good**
- Zero friction from idea to running code
- Brilliant for throwaway prototypes
- Democratises creation — "citizen developers"
- Great at well-understood problem domains

<br>

**🔴 The Limits**
- No auditability — what exactly does it do?
- No regression safety — change one line, break everything
- No compliance — GDPR? WCAG? Data isolation?
- No governance — who approved this going to production?

<br>

> Vibe coding is a **proof of concept machine**. Not a production machine.

---

<!-- _class: lead -->

# 🏛️ Act II: Enterprise AI Development

### *OpenDebt — can AI help to replace PSRM/DMI?*

---

## What is OpenDebt?

<br>

An open-source debt collection platform for Danish public institutions

<br>

| Dimension | Detail |
|---|---|
| Scope | ~600 debt types · ~1,200 creditor institutions |
| Money at stake | Public sector debt recovery |
| Tech stack | Java 21 · Spring Boot 3.3 · PostgreSQL 16 · Kubernetes |
| Compliance | GDPR · Fællesoffentlige Arkitekturprincipper · WCAG 2.1 AA |
| Authentication | OAuth2/OIDC · MitID · OCES3 certificates |
| Architecture | 12 microservices · Event-driven · API-first |

<br>

> Replacing PSRM/DMI — systems responsible for billions in public revenue.
> **This cannot vibe.**

---

## The Development Pipeline

<br>

```
  Petition (customer need)
       │
       ▼
  Outcome Contract  ◄── What does "done" mean, exactly?
       │
       ▼
  Gherkin Feature   ◄── BDD: failing acceptance tests first
       │
       ▼
  Specification     ◄── Technical design, data model, API contract
       │           ◄── C4 model updated → architecture policies validated
       ▼
  Java Unit Tests   ◄── TDD: red → green → refactor
       │
       ▼
  Implementation    ◄── AI-generated, discipline-guided
       │
       ▼
  Code Review       ◄── Strict + minimality agents · Snyk security scan
       │
       ▼
  Docs + Status     ◄── Auto-maintained · sprint tracker synced
       │
       ▼
  Wasteland         ◄── Federated registry: completion, patterns, stamps
```

**9 specialised agents. 20 phases. Orchestrated by Gas City. Zero manual handoffs.**

---

## The Inversion

**Traditional software delivery puts humans everywhere.**

<br>

```
Traditional:  Human writes code → Human reviews PR → Human updates docs
              Human writes status → Human presents to steering committee

This process: Human sets intent (petition)
              ↓
              Agents handle everything deterministic
              ↓
              Human gates at exactly two moments:
                  Gate 1 — Does this spec accurately represent the intent?
                  Gate 2 — Does this code responsibly deliver what was promised?
              ↓
              Human receives decision-support views (steerco, Basecamp)
              to exercise ongoing governance
```

<br>

**What a human actually does at Gate 1 (scaffold review):**
- Is the outcome contract faithful to the business intent?
- Are the Gherkin scenarios complete, testable, and non-speculative?
- Is the spec minimal — no gold-plating?
- Is the Catala tier correct — is this statute or workflow?

**What a human actually does at Gate 2 (merge gate):**
- Does the implementation match the spec — no scope creep?
- Do `mvn verify` and the security scan pass cleanly?
- Is the Catala encoding consistent with the legal text?
- Am I willing to put my name on this going to 1,200 public institutions?

<br>

> *Two gates. All the accountability. None of the grunt work.*

---

## Step 1: The Petition

**A structured customer request — not a ticket, not a story**

<br>

```
petition001 · OCR Payment Matching
  status: implemented
  phase: 0 — Existing Foundation

petition019 · Legacy SOAP Endpoints (OCES3)
  status: implemented
  component: opendebt-integration-gateway
  rationale: Protocol adaptation — not an exception to REST,
             but additive SOAP capability for legacy creditors

petition050 · Unified Case Timeline UI
  status: implemented
  personas: caseworker · citizen · creditor
  principle: one view, all history, role-filtered
```

<br>

72 petitions. 20 phases. Full traceability from business need → code → test.

---

## Step 2: BDD — Behaviour-Driven Design

**Real example from `petition001-ocr-payment-matching.feature`**

```gherkin
Feature: OCR-based matching of incoming payments

  Scenario: Unique OCR auto-match even when the amount differs
    Given an issued påkrav contains OCR-linje "OCR-123"
    And OCR-linje "OCR-123" uniquely identifies debt "D1"
    And debt "D1" has an outstanding balance of 1000 DKK
    And an incoming payment references "OCR-123" with amount 900 DKK
    When the payment is processed
    Then the payment is auto-matched to debt "D1"
    And the payment is not routed to manual matching

  Scenario: Payment without unique OCR match → manual queue
    Given an incoming payment does not contain an OCR-linje
      that uniquely identifies a debt
    When the payment is processed
    Then the payment is routed to manual matching on the case
```

**The AI generates this first. Tests fail. Then code is written.**

---

## Step 3: TDD — Test-Driven Implementation

**Real example from `CaseServiceImplTest.java`**

```java
@ExtendWith(MockitoExtension.class)
class CaseServiceImplTest {

  @Mock private CaseRepository caseRepository;
  @Mock private CaseAccessChecker caseAccessChecker;
  @Mock private CaseEventRepository caseEventRepository;

  @InjectMocks private CaseServiceImpl service;

  @Test
  void createCase_validInput_persistsAndReturnsDto() {
    CaseDto input = CaseDto.builder()
        .caseNumber("SAG-001")
        .title("Test case")
        .build();

    when(caseRepository.save(any())).thenReturn(savedEntity);

    CaseDto result = service.createCase(input);

    assertThat(result.getCaseNumber()).isEqualTo("SAG-001");
    verify(caseEventRepository).save(argThat(e ->
        e.getEventType() == CASE_CREATED));
  }
}
```

Red first. AI writes green. Human reviews.

---

<!-- _class: compact -->

## Step 4: Gas City — Agent Orchestration

**Gas City runs the pipeline automatically. Human gates pause it.**

<br>

```bash
gc start ~/GitHub/opendebt   # brings all agents online in tmux
bd list --assignee human      # find your review gates
bd close <id> "Approved"     # release the gate → pipeline resumes
```

<br>

| Phase | Agent(s) | Produces |
|---|---|---|
| 0 · Translate | `petition-translator` + reviewer | Validated Gherkin scenarios |
| 1 · Assign | `component-assigner` + `application-architect` | Component routing |
| 2 · Architect | `solution-architect` + `c4-model-validator` | C4 model · Architecture review |
| 3 · Specify | `specs-translator` + `specs-reviewer` | Implementation specification |
| 4 · Test | `bdd-test-generator` + coverage auditor | Failing BDD step definitions |
| 5 · Implement | `tdd-enforcer` *(per-service rig)* | Green implementation |
| 6 · Review | `code-reviewer-strict` + `code-minimality-reviewer` | Review findings · Snyk + OWASP scan |
| 7 · Fix | `tdd-enforcer` (rerun) | All findings resolved |
| 8 · Track | `implementation-doc-sync` + `sprint-tracker` | Docs updated · status synced |

<br>

> Two **mandatory human gates**: scaffold review (before code) and merge gate (before main).

---

<!-- _class: compact -->

## Step 5: Law as Code — Juridisk Vejledning → Catala

**Who checks that the PSRM encoding of Danish tax law is actually correct?**

<br>

```
  Juridisk Vejledning (G.A.)
        │
        │  G.A.1.4.3 · G.A.1.4.4 · G.A.2.3.2 · G.A.2.4 · GIL §4 · §18k ...
        ▼
   Catala DSL           ◄── Formally typed, machine-checkable rules
        │                   Anchored to exact G.A. article citations
        ├──► Test suite ◄── Boundary conditions derived from legal text
        │
        ├──► Compare vs. PSRM implementation ◄── Discrepancies surfaced
        │
        └──► CI typecheck ◄── catala typecheck in GitHub Actions (ADR-0032)
```

<br>

**3 completed spikes, 3× Go verdict:**

| Spike | Section | Key finding |
|---|---|---|
| P054 | G.A.1.4.3/1.4.4 (opskrivning/nedskrivning) | 4 discrepancies vs. PSRM — retroaktivitet, §18k under-application |
| P069 | G.A.2.3.2 (dækningsrækkefølge GIL §4) | Token mismatch: INDDRIVELSESRENTER_FORDRINGSHAVER_STK3 vs. G.A. text |
| P070 | G.A.2.4 (forældelse — prescription) | SKM2015.718.ØLR varsel/afgørelse distinction; 2 P059 coverage gaps |

<br>

> **Roadmap: ~50 G.A. sections · ~1–2 person-days each.** Phases 18–20 fully planned.

---

## Step 6: The Wasteland — Federated Knowledge

**Work doesn't disappear into a private repo. It's published to a federated registry.**

<br>

```
  Implementation complete
         │
         ▼
  Wasteland (mfhens/ufst on DoltHub)
  ┌──────────────────────────────────────────────────────┐
  │  wanted board    ← open petitions visible to all     │
  │  completions     ← evidence of shipped work          │
  │  patterns        ← reusable architectural knowledge  │
  │  learnings       ← findings anchored to patterns     │
  │  stamps          ← validator trust signals           │
  └──────────────────────────────────────────────────────┘
```

<br>

| Concept | What it means |
|---|---|
| **Rig** | A participant — human, agent, or org — with a DoltHub identity |
| **Wanted** | Open work anyone can claim (petition or TB item) |
| **Completion** | Evidence that work was done (git SHA, service path) |
| **Pattern** | Reusable solution with validated evidence (e.g., *Double-Entry Financial Ledger*) |
| **Stamp** | Trust signal issued by a validator after reviewing a completion |

<br>

> Stored in versioned SQL (Dolt + DoltHub). Fork → work → push → earn reputation.

---

## GDPR by Architecture, Not by Promise

<br>

```
                ┌─────────────────────────────┐
                │      Person Registry        │
                │  CPR · CVR · Name · Address │  ← Only service that
                │  (AES-256 encrypted at rest)│    touches PII
                └────────────┬────────────────┘
                             │ UUID only
           ┌─────────────────┼──────────────────┐
           │                 │                  │
    debt-service      case-service      payment-service
    debtorPersonId    partyPersonId     payerPersonId
    (UUID)            (UUID)            (UUID)
```

<br>

```java
// CORRECT
@Column(name = "debtor_person_id")
private UUID debtorPersonId;   // ← references registry

// WRONG — AI enforced never to do this
private String cprNumber;      // ← never in this service
```

---

## Architecture: 12 Microservices

<br>

```
┌──────────────┐  ┌───────────────┐  ┌──────────────┐  ┌────────────────────┐
│citizen-portal│  │creditor-portal│  │caseworker-   │  │integration-gateway │
│  (MitID/     │  │  (MitID       │  │portal        │  │  (OCES3 · SOAP ·   │
│  TastSelv)   │  │   Erhverv)    │  │  (Keycloak)  │  │   DUPLA · REST)    │
└──────┬───────┘  └──────┬────────┘  └──────┬───────┘  └─────────┬──────────┘
       └─────────────────┴───────────────────┴───────────────────┘
                         │
    ┌────────┬────────────┼──────────┬──────────────┬──────────────┐
    │        │            │          │              │              │
debt-svc  case-svc  payment-svc  letter-svc  rules-engine  wage-garnishment
(Fordring) (Flowable) (OCR match) (DigPost)  (Drools)      (Lønindeholdelse)
    │        │            │          │              │              │
    └────────┴────────────┴──────────┴──────────────┴──────────────┘
                         │
          ┌──────────────┴─────────────────┐
          │  person-registry  (AES-256 PII) │
          │  creditor-service               │
          │  immudb  (cryptographic ledger) │  ◄── ADR-0029
          └─────────────────────────────────┘
```

PostgreSQL 16 · Keycloak · OpenTelemetry · Kubernetes · Double-entry bookkeeping (ADR-0018)

---

## The Core Insight

<br>

> **AI doesn't replace discipline — it amplifies it.**

<br>

| Without discipline | With discipline |
|---|---|
| AI writes fast, breaks silently | AI writes fast, tests catch regressions |
| Requirements drift | Petitions + outcome contracts hold the line |
| One model, one context | Specialised agents, clear handoffs |
| Manual dispatch, context loss | Human judgment concentrated at accountability moments — not diluted across every PR |
| "It works on my machine" | CI/CD · Snyk · OWASP · automated docs |
| GDPR as an afterthought | GDPR enforced by architecture |
| Law interpreted loosely | G.A. encoded in Catala · discrepancies surfaced |
| Financial records on trust | Double-entry bookkeeping · immudb ledger integrity |
| Work buried in a private repo | Wasteland: federated board · completions · patterns · stamps |

<br>

**Vibe coding scales to one developer.**  
**Disciplined AI development scales to an enterprise.**

---

<!-- _class: lead -->

# The Takeaway

<br>

### 🕹️ Space Invaders = proof that AI can generate code

### 🏛️ OpenDebt = proof that AI can deliver enterprise software

<br>

**The difference is not the model.**  
**The difference is knowing what only a human should decide.**

<br>

*Petition → BDD → Spec → TDD → Review → Law as Code → Gas City → ⏸ Human Gate → Wasteland*

---

<!-- _class: lead -->

# Thank You

<br>

**Questions?**

<br>

`github.com/opendebt` · Java 21 · Spring Boot 3.3 · 72 petitions · 12 services · Gas City · Wasteland

---
