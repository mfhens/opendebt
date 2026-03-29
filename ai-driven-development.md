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

### *OpenDebt — replacing EFI/DMI for Denmark's public sector*

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

> Replacing EFI/DMI — systems responsible for billions in public revenue.
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
```

**9 specialised agents. 9 phases. Zero manual handoffs.**

---

## Step 1: The Petition

**A structured customer request — not a ticket, not a story**

<br>

```
petition001 · OCR Payment Matching
  status: implemented
  phase: 0 — Existing Foundation

petition019 · Legacy SOAP Endpoints (OCES3)
  status: in_progress
  component: opendebt-integration-gateway
  rationale: Protocol adaptation — not an exception to REST,
             but additive SOAP capability for legacy creditors

petition050 · Unified Case Timeline UI
  status: planned
  personas: caseworker · citizen · creditor
  principle: one view, all history, role-filtered
```

<br>

50 petitions. 9 phases. Full traceability from business need → code → test.

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

## Step 4: The AI Agent Pipeline

**9 phases — each with a dedicated, stateless, auditable agent**

<br>

| Phase | Agent(s) | Produces |
|---|---|---|
| 0 · Translate | `petition-translator` + reviewer | Validated Gherkin scenarios |
| 1 · Assign | `component-assigner` + `application-architect` | Component routing |
| 2 · Architect | `solution-architect` + `c4-model-validator` | C4 model · Architecture review |
| 3 · Specify | `specs-translator` + `specs-reviewer` | Implementation specification |
| 4 · Test | `bdd-test-generator` + coverage auditor | Failing BDD step definitions |
| 5 · Implement | `tdd-enforcer` | Green implementation |
| 6 · Review | `code-reviewer-strict` + `code-minimality-reviewer` | Review findings |
| 7 · Fix | `tdd-enforcer` (rerun) | All findings resolved |
| 8 · Track | `implementation-doc-sync` + `sprint-tracker` | Docs updated · status synced |

<br>

> Each agent is **stateless, scoped, and auditable**. No single model owns the whole flow.

---

## Step 5: Law as Code — Juridisk Vejledning → Catala

**Who checks that the PSRM encoding of Danish tax law is actually correct?**

<br>

```
  Juridisk Vejledning (G.A.)
        │
        │  G.A.1.4.3 Opskrivning · G.A.1.4.4 Nedskrivning · GIL §18k
        ▼
   Catala DSL           ◄── Formally typed, machine-checkable rules
        │                   Anchored to exact G.A. article citations
        ├──► Test suite ◄── Boundary conditions derived from legal text
        │
        └──► Compare vs. PSRM implementation
```

<br>

**Spike result: 4 discrepancies found between G.A. prose and PSRM**

| # | Discrepancy | Impact |
|---|---|---|
| 1 | Retroaktivitet: portal compares to *today*, G.A. compares to *fordring.receivedAt* | Differs for old debts |
| 2 | GIL §18k: portal requires retroaktiv **AND** krydssystem; G.A. only requires retroaktiv | Under-application of rule |
| 3 | §7 stk. 1 (6. pkt.) krydssystem case: no Gherkin scenario existed | Untested legal branch |
| 4 | Høring banner (UI) conflated with modtagelsestidspunkt (legal timestamp) | Traceability gap |

<br>

> **Go verdict.** Full G.A. Inddrivelse chapter: ~50 sections · ~1–2 person-days each.

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
┌──────────────┐  ┌──────────────┐  ┌────────────────────┐
│citizen-portal│  │creditor-portal│  │integration-gateway │
│  (MitID/     │  │  (MitID      │  │  (OCES3 · SOAP ·   │
│  TastSelv)   │  │   Erhverv)   │  │   DUPLA · REST)    │
└──────┬───────┘  └──────┬───────┘  └─────────┬──────────┘
       │                 │                     │
       └─────────────────┴─────────────────────┘
                         │
    ┌────────┬────────────┼───────────┬──────────────┐
    │        │            │           │              │
debt-svc  case-svc  payment-svc  letter-svc  rules-engine
(Fordring) (Flowable)  (OCR match) (DigPost)  (Drools)
    │        │            │           │              │
    └────────┴────────────┴───────────┴──────────────┘
                         │
            ┌────────────┴────────────┐
            │     person-registry     │
            │    creditor-service     │
            └─────────────────────────┘
```

PostgreSQL 16 · Keycloak · OpenTelemetry · Kubernetes

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
| "It works on my machine" | CI/CD · Snyk · automated docs |
| GDPR as an afterthought | GDPR enforced by architecture |
| Law interpreted loosely | G.A. encoded in Catala · discrepancies surfaced |

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
**The difference is the process around the model.**

<br>

*Petition → Outcome Contract → BDD → Spec → TDD → Review → Law as Code → Ship*

---

<!-- _class: lead -->

# Thank You

<br>

**Questions?**

<br>

`github.com/opendebt` · Java 21 · Spring Boot 3.3 · 50 petitions · 12 services

---
