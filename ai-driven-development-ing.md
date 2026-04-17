---

## marp: true
theme: ey
paginate: true

---

<!-- _class: lead bg-image -->

# AI-Driven Software Development

### From Copilot to Governed Delivery

**ING · Getting Started · 2026**



---

## The Journey: From One-Liners to a Pipeline

| Phase | What you say to the AI | What you actually get |
| ----- | ---------------------- | --------------------- |
| **1. Ad hoc prompts** | "Fix this function" | A fix and new bugs two files away |
| **2. Context injection** | `AGENTS.md` tells it your stack | Consistent style; still reactive |
| **3. Structured roles** | "Write a Gherkin spec for this story" | Artifact, not just code |
| **4. Chained agents** | "Translate this petition to specs" | Multiple artifacts, one command |
| **5. Governed pipeline** | "Implement petition 058" | Requirements to tests to code to review to docs |

Each step removed one class of rework. None of them required a new tool, only more explicit intent.



---

## What Changed: The Agent Layer

The shift happened when agents got **specialised roles and handoff contracts**.

```
petition-translator   ->  specs-translator  ->  bdd-test-generator
       v                                              v
solution-architect    ->  tdd-enforcer      ->  code-reviewer-strict
       v                                              v
c4-architecture-governor               implementation-doc-sync
```

- Each agent has a narrow mandate and a defined output artifact
- An agent cannot skip a gate; it can only fail it
- The human approves intent at the top and merge readiness at the bottom
- Everything in between is automated, auditable, and reproducible

**This lives in `.claude/agents/` - plain YAML files that any team can adopt, extend, or replace.**



---

## What Makes Agents Work Over Time

- `AGENTS.md` gives repo-local rules for every session
- `mempalace` provides long-term cross-session memory for decisions, validated facts, and next-step continuity
- `dual-graph` captures code structure so the agent can navigate files and symbols with less blind searching
- Together, they reduce blank-slate behaviour, repeated discovery, and context loss

**Without memory and structure, even good agents keep re-learning the same system.**



---

## The Governed Delivery Pipeline

| Step | Main output | Why it exists |
| ---- | ----------- | ------------- |
| Petition / user story | Business intent | Bound the scope |
| Outcome contract + Gherkin | Observable behaviour | Remove ambiguity early |
| Architecture + policies | Governed design | Keep change platform-aligned |
| Specs + tests | Executable expectations | Drive implementation |
| Implementation + review | Code with evidence | Enforce quality |
| Doc sync + status | Current operating picture | Reduce drift afterwards |

**Human role:** gate intent, accountability, and merge readiness.



---

## Common Pitfalls Are Predictable

| Pitfall | Typical AI miss | Why it matters |
| ------- | --------------- | -------------- |
| Security | Missing auth, weak validation, PII leakage | Compliance and incident risk |
| Migrations | Unsafe schema change on live data | Failed deploy or corrupted state |
| Test quality | Green tests that assert very little | False confidence in CI |
| Architecture drift | Shortcuts across service or layer boundaries | Local speed, platform damage |
| Documentation drift | Code changes without spec and ops updates | Teams lose the current picture |

**The pattern is consistent: when context and gates are weak, the same classes of errors return.**



---

# Takeaway

### Copilot reduces time-to-first-code.

### Governed delivery needs specialised agents, memory, and checked pipelines.

### Make the rules explicit. Run them automatically. Review what Copilot cannot know.

**Questions?**
