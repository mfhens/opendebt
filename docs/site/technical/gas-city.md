# Gas City — AI Agent Orchestration

Gas City is the orchestration engine used to automate the OpenDebt petition pipeline.
When active, it runs a background controller that manages AI agent sessions (Claude,
Copilot, etc.), routes work from the Beads issue tracker to the right agent, and
advances each petition through the pipeline without manual dispatch.

This guide covers everything needed to work with Gas City in this project: how to
start and stop it, how to submit petitions, how to handle the mandatory human review
gates, and how to diagnose problems.

---

## Concepts

| Term | What it means |
|------|--------------|
| **City** | A project being orchestrated — this repository is the city |
| **Rig** | A service sub-directory (e.g. `opendebt-rules-engine`) registered for per-service agents |
| **Agent** | An AI coding session (tmux window running Claude). Each agent has a role and a prompt template |
| **Bead** | A work item in the Beads issue tracker (`bd`). Petitions, steps, and human review items are all beads |
| **Molecule** | A bead hierarchy created from a formula — the running instance of a pipeline |
| **Formula** | A workflow template (TOML) that defines the sequence of agent steps for a petition |
| **Human gate** | A bead assigned to `human` — the pipeline pauses until a human closes it |
| **Controller** | The `gc start` process that reconciles running agents to desired state |

The two formulas in this project are:

- **`mol-petition-scaffold`** — Phase 1: translates a petition into outcome contract + Gherkin + implementation spec, then pauses for human review.
- **`mol-petition-implement`** — Phase 2: runs TDD implementation, code review, optional Catala encoding, doc-sync, and a final merge gate.

### Relationship to Claude `pipeline-conductor`

`mol-petition-implement` (v3) now includes **`playwright-test-generator`** and **`user-testing-flow-validator`** beads aligned with the conductor’s Phase 5 and 6.5. The full graph in `~/.claude/agents/pipeline-conductor.agent.md` is still richer: architecture / C4 gates, specifications as YAML, optional Catala **before** specs for legal-footprint work, BDD routing when not Playwright, deployment drift gates, release-manager, etc. Use `@pipeline-conductor` when you need that end-to-end path. **Catala ordering still differs:** Gas City runs **catala-encode after code review**; the conductor places Catala encoding **after architecture and before specifications** in the full pipeline. See ADR 0034 and the agents README.

**Retroactive E2E backfill:** Petitions implemented before Playwright wiring may need GREEN tests instead of RED generator output. In `opendebt-e2e/playwright.config.ts`, when `CI` is set, tests whose titles contain `@backlog` are excluded (`grepInvert`) so unfinished Playwright work does not fail GitHub Actions. See `petitions/e2e-backfill-triage.md`. Creditor-portal flows that complete Keycloak in the browser need the runner to resolve the hostname `keycloak` (the E2E job appends `127.0.0.1 keycloak` to `/etc/hosts`). Wave 4 adds `petition030-038-creditor-portal-surfaces.spec.ts` for Phase 9 shells (detail, hearing, rejected, wizard, adjustment, notifications, reconciliation, reports, dashboard, settings).

**ADR 0034 TDD template:** Copy `opendebt-e2e/tests/creditor-portal/petition012-bff-manual-submission.spec.ts` for new portal petitions — one test per Gherkin scenario (petition012 is GREEN); until implemented, add `@backlog` to the title and throw `Not implemented: petitionNNN — "…"`.

---

## Prerequisites

=== "Install gc"

    `gc` is the Gas City CLI. Build it once from source:

    ```bash
    cd ~/GitHub/gascity
    make install     # installs to $(GOPATH)/bin
    gc version       # verify: should print a version number
    ```

    You also need:

    - `tmux` — agent sessions run in tmux windows
    - `jq` — used by agent scripts
    - `bd` — the Beads CLI (already configured in `.beads/`)

=== "Verify setup"

    Run the health check from the opendebt root:

    ```bash
    cd ~/GitHub/opendebt
    gc doctor
    ```

    Expected output (on a healthy setup):

    ```
    ✓ city-structure — city.toml present
    ✓ config-valid   — agents, rigs, and services valid
    ✓ beads-store    — store accessible
    ✓ dolt-server    — reachable on 127.0.0.1:44723
    ...
    ```

    The warnings `agent-sessions` (no sessions yet) and `events-log` (not started yet)
    are normal before `gc start` is run.

---

## Quick reference

```bash
# Start the controller (all agents come online)
gc start ~/GitHub/opendebt

# Stop the controller
gc stop ~/GitHub/opendebt

# See what is running
gc status

# Submit a petition for processing
bd create --title "P055 Forældelse regler" \
  --label petition --label ready \
  --set-metadata service=rules-engine \
  --set-metadata petition_path=petitions/petition055/petition055.md \
  --set-metadata catala_tier=A

# Find items waiting for your review
bd list --assignee human

# Approve a human gate
bd close <bead_id> "Approved"

# See all open work
bd list --status open

# Watch the event log live
gc events --tail 20 --follow

# Attach to an agent session (read-only observation)
gc session attach opendebt/petition-translator
```

---

## Starting Gas City

### 1. Register the pilot rig

A rig maps a service directory to the pool of rig-scoped agents (currently `tdd-enforcer`).
Edit `city.toml` and uncomment the `[[rigs]]` block:

```toml
[[rigs]]
name = "rules-engine"
path = "/home/markus/GitHub/opendebt/opendebt-rules-engine"
includes = ["packs/opendebt"]
```

Then register it:

```bash
gc rig add rules-engine ~/GitHub/opendebt/opendebt-rules-engine
```

> **Pilot scope.** Start with `rules-engine` only. Add other services once the pilot
> demonstrates value. Each new rig means a new `tdd-enforcer` pool for that service.

### 2. Start the controller

```bash
cd ~/GitHub/opendebt
gc start
```

This launches tmux sessions for all city-scoped agents:

| Agent | Role |
|-------|------|
| `backlog-planner` | Patrols `program-status.yaml` for new ready petitions |
| `petition-translator` | Translates petition markdown → outcome contract |
| `petition-to-gherkin` | Converts outcome contract → `.feature` + step stubs |
| `specs-translator` | Produces implementation spec + `validation-contract.md` from outcome contract |
| `playwright-test-generator` | Generates failing Playwright TS tests in `opendebt-e2e/` after scaffold approval |
| `user-testing-flow-validator` | Runs VAL-* E2E assertions (skipped when no contract / N/A) |
| `code-reviewer` | Reviews code against petition, spec, GDPR, and coding standards |
| `catala-encoder` | Encodes statutory rules in Catala (Tier A petitions only) |
| `doc-sync` | Synchronises docs after approved implementation |

And per-rig agents for each registered service:

| Agent | Role |
|-------|------|
| `{backend-service}/tdd-enforcer` | Implements Java backend code (red → green → refactor) |
| `{portal}/portal-tdd-enforcer` | Implements portal UI code — Thymeleaf + HTMX (red → green → refactor) |

**Backend service rigs** (get `tdd-enforcer`): rules-engine, debt-service,
payment-service, creditor-service, case-service, integration-gateway,
person-registry, letter-service, wage-garnishment-service.

**Portal rigs** (get `portal-tdd-enforcer`): creditor-portal, caseworker-portal,
citizen-portal.

### 3. Verify

```bash
gc status
```

You should see all agents listed as `running` or `idle`.

---

## Submitting a petition

The pipeline starts when you create a petition bead. There are two paths depending
on whether the petition is already tracked in `program-status.yaml`.

### Automatic submission (normal path)

If the petition is already in `petitions/program-status.yaml`, this is all you need:

**1. Write the petition markdown** (if not already present):
```
petitions/petition0XX/petition0XX.md
```

**2. Set the status in `program-status.yaml`:**
```yaml
- id: petition055
  title: "Forældelse — statutory limitation rules"
  status: ready_for_implementation   # ← this triggers automatic pickup
  service: rules-engine
  catala_tier: A                     # A=statutory, B=workflow, C=reference
```

**3. Start (or let run) the controller:**
```bash
gc start ~/GitHub/opendebt
```

The `backlog-planner` agent patrols `program-status.yaml` every 30 seconds. As soon
as it sees a petition with `status: ready_for_implementation` that has no active
molecule yet, it creates the petition bead and pours the `mol-petition-scaffold`
formula automatically. You do not need to run `bd create` yourself.

Watch for pickup:
```bash
gc events --tail 10          # backlog-planner logs its actions here
bd list --label petition     # molecule bead appears within ~30s
```

### Manual submission (one-off)

Use this only for petitions that are **not** tracked in `program-status.yaml`:

```bash
bd create \
  --title "P055 Forældelse — statutory limitation rules" \
  --label petition \
  --label ready \
  --set-metadata service=rules-engine \
  --set-metadata petition_path=petitions/petition055/petition055.md \
  --set-metadata catala_tier=A
```

| Metadata field | Values | Description |
|----------------|--------|-------------|
| `service` | `rules-engine`, `debt-service`, … | Target Maven module (without `opendebt-` prefix) |
| `petition_path` | `petitions/pXXX/pXXX.md` | Relative path to petition markdown |
| `catala_tier` | `A`, `B`, `C` | A = statutory rules (triggers Catala encoding); B = workflow; C = reference |

### What happens next (full pipeline)

The pipeline runs across two formulas. Each named step is a bead assigned to the
relevant agent. Steps in the same column run in parallel.

```
── Phase 1: mol-petition-scaffold ──────────────────────────────────────────────

  backlog-planner detects petition → pours mol-petition-scaffold
          ↓
  [translate]  petition-translator → outcome-contract.md
          ↓
  [gherkin]    petition-to-gherkin → .feature + failing step stubs
  [specs]      specs-translator    → implementation-spec.md          (parallel)
          ↓
  ⏸ GATE 1: human-review-scaffold — review before any code is written

── Phase 2: mol-petition-implement (triggered by Gate 1 approval) ───────────────

  [test-generate]  playwright-test-generator → failing Playwright tests in opendebt-e2e/
          ↓
  [implement]      tdd-enforcer / portal-tdd-enforcer → feature branch, Java/Cucumber TDD
          ↓
  [e2e-acceptance] user-testing-flow-validator → VAL-* E2E (or skip if N/A)
          ↓
  [review]         code-reviewer  → reviews code, GDPR, standards, Snyk scan
          ↓
  [catala-encode]  catala-encoder → Catala formal encoding     (Tier A only)
          ↓
  ⏸ GATE 2: human-review-code — review code + Catala before merge
          ↓
  [doc-sync]  doc-sync agent → architecture/overview.md, ADR, user guides,
                               program-status.yaml → 'implemented'
          ↓
  ⏸ GATE 3: human-merge-gate — final approval to merge
          ↓
  gh pr create / git merge → main
```

The pipeline pauses at each `⏸` gate until a human explicitly closes the bead.

---

## Human review gates

Three human gates are mandatory in every petition pipeline.
**Gas City will not proceed past a gate until you explicitly close it.**

### Finding your gates

```bash
bd list --assignee human
```

Each gate bead contains a full review checklist in its description (`bd show <id>`).

### Gate 1 — Scaffold review (`human-review-scaffold`)

Triggered after `petition-to-gherkin` and `specs-translator` both complete.
No code has been written yet. Review:

| Artefact | What to check |
|----------|--------------|
| `petitions/<id>/<id>-outcome-contract.md` | Accurately represents petition intent — no scope creep, no omissions |
| `.feature` file in `opendebt-<service>/src/test/resources/features/` | Scenarios are complete, testable, 1:1 with acceptance criteria |
| `petitions/<id>/<id>-implementation-spec.md` | Spec is accurate, minimal, feasible; GDPR notes correct |
| `petitions/validation/<id>/validation-contract.md` | VAL-* assertions trace scenarios for Phase 2 E2E, or explicit N/A |
| `catala_tier` metadata | Correct tier — Tier A triggers Catala encoding in Phase 2 |

```bash
# Approve → Phase 2 begins automatically
bd close <bead_id> "Approved"

# Reject → reassign the step that needs rework with notes
bd update <translate_step_bead_id> \
  --assignee opendebt/petition-translator \
  --status open \
  --notes "Rework: AC-3 is ambiguous — clarify what 'active' means for a claim"
```

### Gate 2 — Code review (`human-review-code`)

Triggered after `code-reviewer` closes (and `catala-encoder` for Tier A).
Review:

| Check | Command |
|-------|---------|
| CI passes on feature branch | `gh run list --branch feature/<id>-<service> --limit 5` |
| `mvn verify` green | `mvn verify -pl opendebt-<service>` |
| Code review step closed | `bd list --label review --status closed` |
| Tier A: Catala matches Java | `catala typecheck --language en --no-stdlib catala/<id>-rule.catala_en` |
| No scope creep | `git diff main...feature/<id>-<service> --stat` |

```bash
# Approve → doc-sync runs automatically
bd close <bead_id> "Approved for merge"

# Reject → reassign back to tdd-enforcer with reason
bd update <implement_step_bead_id> \
  --assignee <service>/tdd-enforcer \
  --status open \
  --notes "Tests fail on edge case: <details>"
```

### Gate 3 — Merge gate (`human-merge-gate`)

Triggered after `doc-sync` completes. All automated steps are done.
The branch contains: production code, passing tests, Catala encoding (Tier A),
updated documentation, and `program-status.yaml` set to `implemented`.

```bash
# Open a PR (recommended for traceability)
gh pr create \
  --base main \
  --head feature/<petition_id>-<service> \
  --title "feat(<petition_id>): <description>" \
  --body "Closes petition <petition_id>"

# Approve and merge directly
bd close <bead_id> "Merge approved"
git checkout main && git merge --no-ff feature/<petition_id>-<service>
git push origin main
git branch -d feature/<petition_id>-<service>
git push origin --delete feature/<petition_id>-<service>

# After merge
bd update <petition_bead_id> --status closed
bd dolt push
```

---

## Monitoring progress

### Pipeline overview

```bash
# All open beads (shows entire pipeline state)
bd list --status open

# Just the human gates
bd list --assignee human

# All beads for a specific petition
bd list --metadata-field petition_id=<id>

# Event log (what agents have done)
gc events --tail 30
```

### Agent sessions

```bash
# See all sessions and their status
gc status

# Attach to a running agent session (observe without interrupting)
gc session attach opendebt/petition-translator

# Detach: Ctrl-B D (standard tmux)

# View session logs
gc session logs opendebt/petition-translator
```

### Formula progress

```bash
# Show where a molecule is in its workflow
bd mol current <molecule_id>

# Example output:
#   [done]    translate: Translate petition P055 to outcome contract
#   [done]    gherkin:   Generate Gherkin feature file
#   [current] specs:     Generate implementation spec
#   [ready]   human-review-scaffold: HUMAN GATE
```

---

## Troubleshooting

### An agent is stuck

Check if the session is alive:

```bash
gc status                       # look for "stuck" or stale lastSeen
gc session logs <agent-name>    # read recent output
gc session attach <agent-name>  # observe the live session
```

If stuck, nudge it:

```bash
gc nudge opendebt/petition-translator "Check your hook and resume your current bead."
```

If unresponsive, restart the session:

```bash
gc agent suspend opendebt/petition-translator
gc agent resume  opendebt/petition-translator
```

### A formula step was closed incorrectly

Reopen the bead:

```bash
bd reopen <bead_id> "Reopened: step was closed prematurely"
bd update <bead_id> --assignee opendebt/<agent-name> --status open
```

### The tdd-enforcer pushed code that breaks tests

Check out the branch and diagnose:

```bash
git fetch origin
git checkout feature/<petition_id>-<service>
mvn verify -pl opendebt-<service>
```

Then reassign back to the tdd-enforcer with context:

```bash
bd update <implement_step_bead_id> \
  --assignee rules-engine/tdd-enforcer \
  --status open \
  --notes "Build broken: <paste the failing test output>"
```

### Controller not starting

```bash
gc doctor        # read all warnings and failures
gc doctor --fix  # materialise missing system files
```

Common issues:
- `tmux not found` — install tmux: `sudo apt install tmux`
- `beads-store unreachable` — start the Dolt server: `bd dolt server start`
- `config-refs` warning — check that all `prompt_template` paths exist in `packs/opendebt/prompts/`

### Catala step blocks on Tier B/C petitions

The `catala-encode` step always appears in the formula DAG — there is no conditional
branching in Gas City formulas. For Tier B/C petitions, the catala-encoder agent closes
the step immediately with "Skipped: catala_tier=B". This adds a few seconds of latency
but avoids formula complexity. If the catala-encoder agent is not running, the pipeline
will block; nudge or restart it:

```bash
gc nudge opendebt/catala-encoder "Skip this Tier B step and close the bead."
```

### Checking what Gas City wrote to Beads

```bash
# All beads created by the pipeline (not just open ones)
bd list --limit 50

# A specific bead with all metadata
bd show <bead_id>

# The formula step graph for a molecule
bd mol current <molecule_id>
```

---

## Stopping Gas City

```bash
gc stop ~/GitHub/opendebt
```

This drains running agent sessions gracefully (up to the `shutdown_timeout` of 10 seconds
configured in `city.toml`) and stops the controller. All bead state is preserved in the
Dolt store — restarting with `gc start` picks up where it left off.

After stopping, push all state to the remotes:

```bash
bd dolt push                                                    # Beads store
cd ~/.hop/commons/mfhens/ufst && dolt push origin main          # Wasteland
cd ~/GitHub/opendebt && git push                                # Code
```

---

## Registered services

All services and portals are registered in `city.toml`. Each backend service gets
a `tdd-enforcer` pool; each portal gets a `portal-tdd-enforcer` pool. City-scoped
agents (petition-translator, code-reviewer, etc.) are shared across all rigs.

### Adding a new service

1. Add a rig entry in `city.toml`:
   ```toml
   [[rigs]]
   name = "new-service"
   path = "/home/markus/GitHub/opendebt/opendebt-new-service"
   includes = ["packs/opendebt"]
   ```

2. Register the rig:
   ```bash
   gc rig add new-service ~/GitHub/opendebt/opendebt-new-service
   ```

3. Restart the controller:
   ```bash
   gc restart ~/GitHub/opendebt
   ```

4. Update `pack.toml` — add the new rig name to the appropriate agent's `rig_filter`:
   - Backend services → add to `tdd-enforcer` rig_filter
   - Portals → add to `portal-tdd-enforcer` rig_filter

---

## Further reading

- [`city.toml`](https://github.com/mfhens/opendebt/blob/main/city.toml) — agent config, daemon settings, rig template
- [`packs/opendebt/`](https://github.com/mfhens/opendebt/blob/main/packs/opendebt/) — pack manifest, formulas, prompt templates
- [Gas City docs](https://github.com/steveyegge/gascity) — SDK reference and CLI documentation
- [`gc prime`](https://github.com/mfhens/opendebt) — print the agent onboarding prompt (run inside a gc session)
