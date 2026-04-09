# Agent Diaries

This directory contains per-petition diary entries written by SDLC pipeline agents. Each diary
captures patterns, decisions, and constraints discovered during a petition's lifecycle so that
future runs of the same agent can recall them via `mempalace search`.

This replaces `bd comment add "PATTERN/FACT/..."` which was write-only and never recalled.

## Structure

```
diaries/
  code-reviewer-strict/         ← specialist
  solution-architect/           ← specialist
  code-minimality-reviewer/
  specs-reviewer/
  specs-minimality-reviewer/
  gherkin-minimality-reviewer/
  unit-test-minimality-reviewer/
  petition-translator-reviewer/
  solution-architecture-reviewer/
  bdd-test-coverage-auditor/
  specs-translator/
  tdd-enforcer/
  bdd-test-generator/
  playwright-test-generator/
  petition-translator/
  petition-to-gherkin/
  application-architect/
  concept-model-curator/
  c4-architecture-governor/
  c4-model-validator/
  catala-encoder/
  implementation-doc-sync/
  user-testing-flow-validator/
  hotfix-conductor/
  delivery-orchestrator/
  portal-tdd-enforcer/
  scrutiny-feature-reviewer/
```

## File naming

`petition<ID>.md` — e.g. `petition042.md`

## Diary format — specialist agents (code-reviewer-strict, solution-architect)

See their individual agent prompts for full format spec. They use enriched formats.

## Diary format — all other agents (generic)

```markdown
# Diary: <agent-name> — Petition <ID>
**Date**: YYYY-MM-DD
**Petition**: <title>
**Outcome**: <Completed | Approved | Rejected | Blocked | Passed | Failed>

## Key Findings
- PATTERN: <positive pattern confirmed or anti-pattern found>
- FACT: <constraint, business rule, or technical gotcha>
- DECISION: <decision made with rationale>
- DEVIATION: <anti-pattern or scope creep to prevent in future>
- INVESTIGATION: <root cause of a problem or ambiguity resolved>
- LEARNED: <anything else worth remembering>

## Deviations to Avoid in Future
- <specific anti-pattern with context — highest-value memory for recall>
```

Include at least one entry under **Key Findings**. Use multiple bullet points when the petition
surfaces more than one finding. Leave sections empty only if genuinely nothing applies.

## Recall command

Before starting work on a petition, search for prior findings in the same domain:

```bash
python -m mempalace --palace ~/.mempalace/palace search "<topic> PATTERN" --wing opendebt --room petitions
python -m mempalace --palace ~/.mempalace/palace search "<topic> DEVIATION" --wing opendebt --room petitions
```

`<topic>` = primary domain of the petition (fordring, payment, portal, catala, gherkin, c4, specs,
architecture, test, translation, etc.)

## KG write (optional — agents that record structural decisions)

```bash
python -m mempalace kg --kg ./mempalace/knowledge_graph.sqlite3 add \
  "<subject>" "<predicate>" "<object>" \
  --valid-from "YYYY-MM-DD" --source "petition<ID>"
```
