# Agent Diaries

This directory contains per-petition diary entries written by SDLC pipeline agents. Each diary
captures patterns, decisions, and constraints discovered during a petition's lifecycle so that
future runs of the same agent can recall them via `mempalace search`.

## Structure

```
diaries/
  code-reviewer-strict/    ← one file per petition reviewed
  solution-architect/      ← one file per petition architected
```

## File naming

`petition<ID>.md` — e.g. `petition042.md`

## Diary format (agents must follow this exactly)

```markdown
# Diary: <AgentName> — Petition <ID>
**Date**: YYYY-MM-DD
**Petition**: <title>
**Outcome**: Approved | Rejected | Blocked | Completed

## Patterns Well-Applied
- <pattern name>: <brief description of how it was applied>

## Patterns Violated or Discarded
- DISCARD: <what was discarded and why>

## Facts and Constraints Discovered
- FACT: <constraint, business rule, or technical fact that surprised or was non-obvious>

## Quality Score (code-reviewer-strict only)
<N>/100 — <brief rationale>

## Deviations to Avoid in Future
- <specific thing to avoid, with context>
```

## Recall command

Before starting work on a petition, agents recall prior patterns:

```bash
python -m mempalace --palace ~/.mempalace/palace search "<topic> pattern" --wing opendebt --room petitions
```
