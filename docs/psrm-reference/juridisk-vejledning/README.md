# G.A Inddrivelse — Den juridiske vejledning (snapshot)

This folder contains a scraped snapshot of the **G.A Inddrivelse** section of
*Den juridiske vejledning 2026-1*, published by Gældsstyrelsen / Skattestyrelsen.

| | |
|---|---|
| **Source** | <https://info.skat.dk/data.aspx?oid=9672&chk=220619> |
| **Section** | G.A — Inddrivelse af gæld til det offentlige |
| **Version** | Den juridiske vejledning 2026-1 (chk=220619) |
| **Scraped** | 2026-03-28 |
| **Pages** | 438 |

## Purpose

Offline reference for the OpenDebt development team. The vejledning defines
the legal framework that governs claim processing, write-ups, write-downs,
enforcement, insolvency handling, and all other operations implemented in
OpenDebt.

## Structure

Files are named after their section code exactly as published:

```
G.A Inddrivelse.md          ← root / index
G.A.1 ...md                 ← chapter level
G.A.1.1 ...md               ← section level
G.A.1.1.1 ...md             ← sub-section
...
```

The three top-level chapters are:

| Chapter | Topic |
|---|---|
| **G.A.1** | Skatteforvaltningen som inddrivelsesmyndighed |
| **G.A.2** | Regler for restanceinddrivelsesmyndighedens inddrivelsesværktøjer |
| **G.A.3** | Værktøjer (opdelt på person/virksomhed) |

## Important notices

> **This is a point-in-time snapshot.** The vejledning is actively maintained
> by Gældsstyrelsen and updated when legislation changes. Always verify against
> the live source before making legal or architectural decisions.

The content is reproduced here solely for development reference. The authoritative
and legally binding version is the live publication at info.skat.dk.

## Refreshing the snapshot

Re-run the scraper script from the session files:

```powershell
python scrape_ga.py
```

The script is in the Copilot session files. Clear this folder first, then run.
