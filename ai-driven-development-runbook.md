# AI-Driven Development Demo Runbook

## Purpose

Use OpenDebt as a concrete showcase for a governed AI SDLC.

The objective is not to impress the audience with autonomous agents.
The objective is to help a customer with no current AI development practice understand:

- what an AI-enabled SDLC is
- why governance matters more than raw code generation
- how the model accelerates a complex national debt collection system

## Audience Assumptions

- The customer has no established AI development practice today
- They need concepts, not deep agent internals
- They will care about risk, governance, and credibility before novelty
- They may be skeptical of claims such as "fully autonomous" or "always up to date"

## Core Message

**AI SDLC is not "AI writes code faster."**

**AI SDLC is "change stays governable while delivery accelerates."**

For this demo, the four anchor points are:

1. Guardrails exist before coding starts
2. Cross-cutting NFRs are explicit and reused
3. The delivery chain is traceable through artifacts
4. Documentation and portfolio status are treated as outputs of delivery

## Recommended Format

- Use the Marp deck in `ai-driven-development.md`
- Keep the session to `40` minutes of presentation, `10` minutes of controlled repo demo, and `10` minutes of Q&A
- Keep any live wizard submission to an optional `3-5` minute extension only if the demo environment is already warm
- Do not run a full live pipeline on stage
- Use prepared repository artifacts as evidence instead

## 60-Minute Agenda

| Time | Deck slide(s) | Goal | Demo cue |
|---|---|---|---|
| 0-5 min | 1-2 | Reset expectations: coding speed is not the main problem | None |
| 5-10 min | 3-4 | Define what enterprise delivery must guarantee | None |
| 10-16 min | 5 | Show that guardrails exist before implementation | Open bootstrap artifacts |
| 16-24 min | 6 | Walk through one understandable business change | Open `petition050` |
| 24-30 min | 7 | Explain traceability honestly and concretely | Keep `petition050` visible |
| 30-36 min | 8 | Show NFRs as explicit, reusable controls | Open `compliance/nfr-register.yaml` |
| 36-42 min | 9 | Show architecture as a checked artifact | Open `architecture/workspace.dsl` |
| 42-47 min | 10 | Prove the same SDLC scales to legal and financial complexity | Open `petition058` |
| 47-52 min | 11 | Show docs and portfolio visibility as delivery outputs | Open `program-status.yaml`, optionally `mkdocs.yml` |
| 52-60 min | 12 | Takeaway and Q&A | Stay on closing slide |

## Slide-By-Slide Talk Track

### Slides 1-2: Opening

Key message:
- Most people have seen AI generate code
- Very few have seen AI govern delivery of a regulated system
- OpenDebt is the proof point because it is complex, public-sector, and multi-service

What to say:
- "This is a talk about governed delivery, not agent theatre."
- "The hard part is not generating code; the hard part is keeping change controlled."

### Slides 3-4: Evaluation Criteria And SDLC Shape

Key message:
- Before talking about tools, define what good delivery must guarantee
- The SDLC is an artifact chain with clear purposes
- Humans remain accountable at the critical gates

What to say:
- "If AI helps only with coding, but not with governance, it does not solve enterprise delivery."
- "This is not one super-agent. It is a delivery system."

### Slide 5: Guardrails Before Coding

Key message:
- `project-bootstrap` is not just setup; it establishes the control plane
- Architecture, policies, NFRs, status, and conventions are explicit before code changes begin

Files to show:
- `.factory/project.yaml`
- `architecture/workspace.dsl`
- `architecture/policies.yaml`
- `compliance/nfr-register.yaml`
- `AGENTS.md`
- `petitions/program-status.yaml`

What to say:
- "The pipeline starts by making the system legible to both humans and agents."

### Slide 6: Simple Example

Key message:
- Start with a relatable change before showing statutory complexity
- Even a straightforward UI improvement is cross-cutting in a real system

Primary file:
- `petitions/petition050-unified-case-timeline-ui.md`

What to say:
- "This is exactly the kind of change a customer can understand immediately."
- "It looks simple, but it already touches privacy, reuse, UX, and architecture."

### Slide 7: Traceability

Key message:
- Do not overclaim
- Traceability is strongest when attached to durable artifacts

What to say:
- "I am not claiming every conversation is captured."
- "I am claiming the delivery chain is traceable through the artifacts that matter."

### Slide 8: NFRs As Code

Key message:
- NFRs are not rediscovered on every change
- They are declared once, matched to scope, and checked

Primary file:
- `compliance/nfr-register.yaml`

NFR examples to point out:
- authentication on non-public endpoints
- structured audit events
- trace propagation
- no cross-service database access

What to say:
- "This is what 'baked in from the start' means in practice."

### Slide 9: Architecture As Code

Key message:
- Architecture is not slideware
- The model is versioned and checked
- Drift becomes visible early

Primary file:
- `architecture/workspace.dsl`

What to say:
- "In many projects, architecture is a stale picture."
- "Here it is a source artifact with validators and governance around it."

### Slide 10: Complexity Proof

Key message:
- Show that the same SDLC handles much harder work than a UI change
- Use complexity as credibility, not as a teaching baseline

Primary files:
- `petitions/petition058-modregning-korrektionspulje-outcome-contract.md`
- `petitions/petition058-modregning-korrektionspulje-solution-architecture.md`

What to say:
- "You do not need to understand every legal rule here."
- "What matters is that the delivery model still works when the domain becomes this complex."

### Slide 11: Documentation And Portfolio Visibility

Key message:
- Docs and status are outputs of delivery
- The honest claim is controlled drift, not magical perfection

Primary files:
- `petitions/program-status.yaml`
- `mkdocs.yml`

Useful snapshot to mention:
- `52` implemented
- `6` validated
- `3` in progress
- `1` blocked

What to say:
- "The point is not that nobody ever forgets documentation."
- "The point is that the process makes drift visible and harder to ignore."

### Slide 12: Closing

Key message:
- Acceleration means less ambiguity, less drift, and less rework
- Human effort moves to governance and decisions

One-line close:
- "This is not AI replacing software delivery. This is AI making governed delivery faster."

## Controlled Live Demo Plan

Keep the live part short and deterministic.

Recommended order:

1. Open `petitions/petition050-unified-case-timeline-ui.md`
2. Open `compliance/nfr-register.yaml`
3. Open `architecture/workspace.dsl`
4. Open `petitions/petition058-modregning-korrektionspulje-outcome-contract.md`
5. Open `petitions/program-status.yaml`
6. Optionally open `mkdocs.yml`

Rules for the live segment:

- Do not run network-dependent orchestration on stage
- Do not switch rapidly between many files
- Spend more time explaining meaning than proving navigation speed
- Use one file per point, then move on

### Optional Live Transaction: Submit One Debt Post Via The Claim Wizard

Use this only if the environment is already running cleanly and the audience wants one concrete transactional step after the repo walkthrough.

Suggested flow:

1. Open `http://localhost:8085/creditor-portal/fordring/opret`
2. If prompted, sign in as `creditor` / `creditor123`
3. In the creditor picker, choose `00000000-0000-0000-0000-000000000001` (`SKAT-DEMO-001`)
4. Step 1: debtor type `CPR`, identifier `0503581234`, first name `Lars`, last name `Andersen`
5. Step 2: claim type `SKAT`, amount `1250.50`, principal `1000.00`, creditor reference `DEMO-<date>`, due date around `30` days ago, limitation date `2035-12-31`, estate processing `No`
6. Step 3-4: submit and show the receipt outcome

Data that must already be in place:

- `person-registry` must contain the demo debtor `0503581234` / `Lars Andersen`
- `creditor-service` must expose the active demo creditor `SKAT-DEMO-001` with `allow_portal_actions=true` and `allow_create_recovery_claims=true`
- `creditor-portal` must be able to resolve the debtor through `PERSON_REGISTRY_URL`
- The demo startup path must seed the demo organizations and persons needed for the wizard path

Failure cues to recognize quickly:

- If the wizard redirects back to the dashboard, the selected creditor agreement does not allow claim creation
- If step 1 fails verification, the person seed or the `person-registry` connection is missing
- If the final result is rejected, use it as a validation-chain example or skip the live transaction and return to the artifact story

What to say:

- "This is optional. The point is not the click path; the point is that even one transaction depends on identity, permissions, and a GDPR-safe person lookup."
- "The debtor details are resolved from Person Registry and downstream services work with technical IDs."

## Preparation Checklist

Before the meeting:

- Confirm the Marp deck opens correctly
- Keep the repository at the root ready to browse
- Preload or bookmark the key files listed above
- Rehearse the two examples: `petition050` and `petition058`
- Be ready to explain why `petition050` is the primary teaching example
- Be ready to explain why `petition058` is the credibility proof
- If you may run the wizard, verify `http://localhost:8085/creditor-portal/fordring/opret` opens and `SKAT-DEMO-001` is selectable
- If you may run the wizard, verify the demo debtor `0503581234` / `Lars Andersen` resolves successfully in step 1
- If you may run the wizard, keep one ready-made creditor reference value so you do not improvise on stage

## Backup Plan

If the repo demo becomes awkward:

- Stay in the slides
- Describe the artifact names verbally
- Use the slide notes to keep the story tight
- Skip straight from slide 8 to slide 11 if time is compressed
- Skip the wizard immediately; it is supporting evidence, not the core story

If the audience is highly executive:

- Reduce technical file navigation
- Emphasize governance, risk reduction, and portfolio visibility

If the audience is more technical:

- Spend a little more time on `nfr-register.yaml` and `workspace.dsl`
- Keep `petition058` focused on why the workflow is hard, not on every statute detail

## Q&A Anchors

### "Does this remove developers?"

No. It changes where human effort is spent:
- less manual boilerplate and coordination
- more review, architecture judgment, and decision-making

### "Where is the acceleration, exactly?"

Primarily in:
- less ambiguity at start
- less rework later
- less architecture drift
- less manual status and documentation chasing

### "How do you trust the output?"

By treating delivery as a governed chain of artifacts, reviews, and gates.

### "Is documentation really always current?"

Use the honest answer:
- not magically perfect
- but actively synchronized and much harder to let drift silently

### "Why OpenDebt as the showcase?"

Because it combines:
- multiple services
- public-sector compliance
- statutory logic
- financial integrity requirements
- real delivery governance needs

## Phrases To Use

- "governed delivery"
- "artifact-level traceability"
- "cross-cutting NFRs"
- "architecture as a checked artifact"
- "documentation drift is actively controlled"
- "human judgment concentrated at the critical gates"

## Phrases To Avoid

- "full traceability of every decision"
- "documentation is always perfect"
- "fully autonomous delivery"
- "the agents just build the system for you"
- "developers are no longer needed"
