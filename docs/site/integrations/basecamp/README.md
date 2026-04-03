# Basecamp integration (OpenDebt)

Stakeholder-facing mirror of **`petitions/program-status.yaml`**, Beads, and ADRs in Basecamp — without duplicating canonical truth in Basecamp.

`program-status.yaml` uses the **nested** `program` block (petition map + `program.phases` + `program.critical_path`); the scripts normalize this automatically (same scripts as osm2).

## Principles

1. **Repo + Beads stay authoritative.** Basecamp is for readability, assignment, and discussion.
2. **Stable IDs.** Card titles use petition keys (`petition001`, …) as prefixes; optional **`scripts/basecamp/card-map.json`** maps petition IDs to Basecamp card recording IDs for comment sync.
3. **Automation publishes; humans triage.**

## One-time setup

1. Install the [Basecamp CLI](https://github.com/basecamp/basecamp-cli) and authenticate: `basecamp auth login`.
2. Copy **`.basecamp/config.example.json`** to **`.basecamp/config.json`** and fill in `project_id`, `card_table_id`, `todolist_id`, and **`columns`** (IDs from `basecamp cards columns --in <project>`).
3. Run `basecamp config trust` for the repo config file.

Optional: set **`BASECAMP_PROJECT`** instead of `project_id`.

## Scripts (`scripts/basecamp/`)

| Script | Purpose |
|--------|---------|
| **`Publish-SteerCoMessage.ps1`** | SteerCo digest Message from `program-status.yaml`. |
| **`Sync-PetitionBoard.ps1`** | Kanban cards `petitionXXX — title` + column by status. |
| **`Sync-TechnicalBacklog.ps1`** | TB items as todos. |
| **`Sync-BeadsCommentsToBasecamp.ps1`** | Beads comments → card comment. |

### Examples

```powershell
.\scripts\basecamp\Publish-SteerCoMessage.ps1 -DryRun
.\scripts\basecamp\Publish-SteerCoMessage.ps1 -NoSubscribe
.\scripts\basecamp\Sync-PetitionBoard.ps1 -DryRun
.\scripts\basecamp\Sync-TechnicalBacklog.ps1
```

## Petition status → columns

Map YAML **`status`** values (see `program.status_values` in `program-status.yaml`) to column IDs. The example config includes: `not_started`, `architecture_ready`, `blocked`, `ready_for_implementation`, `in_progress`, `implemented`, `validated`, plus **`default`**.

## Gauges

The digest includes completion % (petitions `implemented` or `validated`). Needle **position** is not updatable via CLI except by creating a new needle; see Basecamp CLI `gauges` help.

## Privacy

Do not sync secrets or unpublished legal text without review.
