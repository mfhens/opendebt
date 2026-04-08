#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
seed-beads.py — Seed Beads from petitions/program-status.yaml.

Seeds:
  • not_started and in_progress petitions as epics (P0–P2 by legal footprint)
  • Open/not_started/blocked technical backlog items as tasks (P0–P3 by priority)
  • Dependencies between seeded issues (skips deps that are already implemented)

Usage:
    python seed-beads.py           # Seed all active work
    python seed-beads.py --dry-run # Print commands without running them
    python seed-beads.py --tb-only # Seed only technical backlog items
    python seed-beads.py --petitions-only # Seed only petitions
"""

import subprocess
import sys
import re
import argparse
from pathlib import Path

# Force UTF-8 output on Windows
if sys.stdout.encoding.lower() != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

try:
    import yaml
except ImportError:
    print("Installing pyyaml...")
    subprocess.run([sys.executable, "-m", "pip", "install", "pyyaml", "-q"], check=True)
    import yaml

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

REPO_ROOT = Path(__file__).parent
PROGRAM_STATUS = REPO_ROOT / "petitions" / "program-status.yaml"

# Statuses to skip (already done)
SKIP_STATUSES = {"implemented", "validated", "done", "wont_fix", "superseded"}

# Map program-status priority strings → bd priority numbers
TB_PRIORITY_MAP = {
    "critical": 0,
    "high": 1,
    "medium": 2,
    "low": 3,
}

# ---------------------------------------------------------------------------
# bd command wrapper
# ---------------------------------------------------------------------------

def bd(args: list[str], dry_run: bool = False) -> str:
    cmd = ["bd"] + args
    cmd_str = " ".join(cmd)
    if dry_run:
        print(f"  [dry-run] {cmd_str}")
        return ""
    result = subprocess.run(cmd_str, shell=True, capture_output=True, text=True,
                            cwd=str(REPO_ROOT))
    output = result.stdout.strip()
    if result.returncode != 0:
        err = result.stderr.strip() or result.stdout.strip()
        print(f"  WARNING: bd command failed: {cmd_str}\n    {err}")
        return ""
    return output


def create_issue(title: str, issue_type: str, priority: int,
                 dry_run: bool = False) -> str | None:
    """Create a bead and return its ID (e.g. 'opendebt-abc')."""
    # Truncate title to avoid shell issues
    safe_title = title.replace('"', "'")[:160]
    output = bd(["create", safe_title, "--type", issue_type, "--priority", str(priority)],
                dry_run=dry_run)
    if dry_run:
        return f"dry-{re.sub(r'[^a-z0-9]', '-', title[:10].lower())}"
    # Parse: "✓ Created issue: opendebt-abc — ..."
    match = re.search(r"Created issue:\s+(\S+)", output)
    if match:
        return match.group(1)
    print(f"  WARNING: could not parse ID from: {output!r}")
    return None


def add_dep(child_id: str, parent_id: str, dry_run: bool = False) -> None:
    bd(["dep", "add", child_id, parent_id], dry_run=dry_run)


# ---------------------------------------------------------------------------
# Priority helpers
# ---------------------------------------------------------------------------

def petition_priority(petition: dict, status: str) -> int:
    if status == "in_progress":
        return 0
    # legal_footprint True = higher urgency
    if petition.get("legal_footprint", False):
        return 1
    return 2


def tb_priority(tb: dict) -> int:
    p = tb.get("priority", "medium")
    return TB_PRIORITY_MAP.get(str(p).lower(), 2)


# ---------------------------------------------------------------------------
# Seed petitions
# ---------------------------------------------------------------------------

def seed_petitions(program: dict, dry_run: bool) -> dict[str, str]:
    """Returns mapping: petition_key → bead_id"""
    petitions = program.get("petitions", {})
    mapping: dict[str, str] = {}
    active_statuses = {"not_started", "in_progress"}
    count = 0

    print("\n=== PETITIONS ===")
    for key, petition in petitions.items():
        status = petition.get("status", "not_started")
        if status not in active_statuses:
            continue

        title_text = petition.get("title", key)
        title = f"{key}: {title_text}"
        priority = petition_priority(petition, status)
        phase = petition.get("phase", "")
        label = f"[P{priority}] [{phase}] {title}"

        print(f"  Creating: {label}")
        bead_id = create_issue(title, "epic", priority, dry_run=dry_run)
        if bead_id:
            mapping[key] = bead_id
            count += 1

    print(f"  -> {count} petitions seeded")
    return mapping


# ---------------------------------------------------------------------------
# Seed technical backlog
# ---------------------------------------------------------------------------

def seed_tb(program: dict, dry_run: bool) -> dict[str, str]:
    """Returns mapping: tb_id → bead_id"""
    backlog = program.get("technical_backlog", [])
    mapping: dict[str, str] = {}
    count = 0

    print("\n=== TECHNICAL BACKLOG ===")
    for tb in backlog:
        tb_id = tb.get("id", "")
        status = str(tb.get("status", "not_started")).lower()

        if status in SKIP_STATUSES:
            continue

        title = f"{tb_id}: {tb.get('title', tb_id)}"
        priority = tb_priority(tb)
        print(f"  Creating: [P{priority}] {title}")
        bead_id = create_issue(title, "task", priority, dry_run=dry_run)
        if bead_id:
            mapping[tb_id] = bead_id
            count += 1

        # Handle nested sub-tasks (e.g. TB-DR-001-a, TB-028-a)
        for sub in tb.get("tasks", []):
            sub_id = sub.get("id", "")
            sub_status = str(sub.get("status", "not_started")).lower()
            if sub_status in SKIP_STATUSES:
                continue

            sub_title = f"{sub_id}: {sub.get('title', sub_id)}"
            sub_priority = TB_PRIORITY_MAP.get(str(sub.get("priority", "medium")).lower(), 2)
            print(f"    Creating: [P{sub_priority}] {sub_title}")
            sub_bead_id = create_issue(sub_title, "task", sub_priority, dry_run=dry_run)
            if sub_bead_id:
                mapping[sub_id] = sub_bead_id
                count += 1
                # Sub-task depends on parent
                if bead_id:
                    print(f"    Linking: {sub_id} depends on {tb_id}")
                    add_dep(sub_bead_id, bead_id, dry_run=dry_run)

    print(f"  -> {count} TB items seeded")
    return mapping


# ---------------------------------------------------------------------------
# Wire petition dependencies
# ---------------------------------------------------------------------------

def wire_petition_deps(program: dict, petition_map: dict[str, str],
                       dry_run: bool) -> None:
    petitions = program.get("petitions", {})
    print("\n=== PETITION DEPENDENCIES ===")
    dep_count = 0
    for key, petition in petitions.items():
        child_id = petition_map.get(key)
        if not child_id:
            continue
        for dep_key in petition.get("depends_on", []):
            parent_id = petition_map.get(dep_key)
            if not parent_id:
                continue  # Dep already implemented — no need to track
            print(f"  {key} → {dep_key}")
            add_dep(child_id, parent_id, dry_run=dry_run)
            dep_count += 1
    print(f"  -> {dep_count} dependencies wired")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main() -> None:
    parser = argparse.ArgumentParser(description="Seed Beads from program-status.yaml")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print bd commands without executing them")
    parser.add_argument("--tb-only", action="store_true",
                        help="Seed only technical backlog items")
    parser.add_argument("--petitions-only", action="store_true",
                        help="Seed only petitions")
    args = parser.parse_args()

    if not PROGRAM_STATUS.exists():
        print(f"ERROR: {PROGRAM_STATUS} not found")
        sys.exit(1)

    print(f"Loading {PROGRAM_STATUS}")
    with open(PROGRAM_STATUS, encoding="utf-8") as f:
        data = yaml.safe_load(f)

    program = data.get("program", {})

    if args.dry_run:
        print("\n[DRY RUN — no bd commands will execute]\n")

    petition_map: dict[str, str] = {}
    tb_map: dict[str, str] = {}

    if not args.tb_only:
        petition_map = seed_petitions(program, dry_run=args.dry_run)

    if not args.petitions_only:
        tb_map = seed_tb(program, dry_run=args.dry_run)

    if not args.tb_only:
        wire_petition_deps(program, petition_map, dry_run=args.dry_run)

    total = len(petition_map) + len(tb_map)
    print(f"\nDone -- {total} issues seeded")
    print("  Run 'bd ready' to see unblocked work")
    print("  Run 'bd list' to see all seeded issues")


if __name__ == "__main__":
    main()
