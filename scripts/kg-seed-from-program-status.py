#!/usr/bin/env python3
"""
Seed the MemPalace Knowledge Graph from petitions/program-status.yaml.

Emits one triple per petition for:
  - has_status       (petition → status value)
  - belongs_to_phase (petition → phase id)
  - depends_on       (petition → dependency petition)
  - last_reviewed    (petition → date string, stored as has_status source date)

Run from the opendebt repo root:
    python scripts/kg-seed-from-program-status.py
"""

import sys
import os
from pathlib import Path
from datetime import date

import yaml

# Resolve paths
REPO_ROOT = Path(__file__).parent.parent
STATUS_FILE = REPO_ROOT / "petitions" / "program-status.yaml"

# Allow running without installing mempalace by adding parent to sys.path
MEMPALACE_REPO = Path(os.environ.get("MEMPALACE_REPO", REPO_ROOT.parent / "mempalace"))
sys.path.insert(0, str(MEMPALACE_REPO))

from mempalace.knowledge_graph import KnowledgeGraph

SOURCE_FILE = "program-status.yaml"
TODAY = str(date.today())
# KG lives in the repo; override with MEMPALACE_KG env var if needed
DEFAULT_KG_PATH = REPO_ROOT / "mempalace" / "knowledge_graph.sqlite3"


def load_yaml(path: Path) -> dict:
    with open(path, encoding="utf-8") as f:
        return yaml.safe_load(f)


def build_phase_map(program: dict) -> dict[str, str]:
    """Return {petition_id: phase_id} from the phases list."""
    mapping = {}
    for phase in program.get("phases", []):
        phase_id = phase["id"]
        for petition in phase.get("petitions", []):
            mapping[petition] = phase_id
    return mapping


def seed(kg: KnowledgeGraph, data: dict) -> tuple[int, int]:
    program = data.get("program", {})
    petitions = program.get("petitions", {})
    phase_map = build_phase_map(program)

    added = 0
    skipped = 0

    for petition_id, info in petitions.items():
        if not isinstance(info, dict):
            skipped += 1
            continue

        valid_from = info.get("last_reviewed") or TODAY
        if valid_from and not isinstance(valid_from, str):
            valid_from = str(valid_from)

        # Ensure entities exist
        kg.add_entity(petition_id)

        # has_status
        status = info.get("status")
        if status:
            kg.add_entity(status)
            kg.add_triple(
                subject=petition_id,
                predicate="has_status",
                obj=status,
                valid_from=valid_from,
                confidence=1.0,
                source_file=SOURCE_FILE,
            )
            added += 1

        # belongs_to_phase
        phase = phase_map.get(petition_id) or info.get("phase")
        if phase:
            kg.add_entity(phase)
            kg.add_triple(
                subject=petition_id,
                predicate="belongs_to_phase",
                obj=phase,
                valid_from=valid_from,
                confidence=1.0,
                source_file=SOURCE_FILE,
            )
            added += 1

        # depends_on
        for dep in info.get("depends_on", []) or []:
            kg.add_entity(dep)
            kg.add_triple(
                subject=petition_id,
                predicate="depends_on",
                obj=dep,
                valid_from=valid_from,
                confidence=1.0,
                source_file=SOURCE_FILE,
            )
            added += 1

    return added, skipped


def main():
    if not STATUS_FILE.exists():
        print(f"ERROR: {STATUS_FILE} not found. Run from the opendebt repo root.")
        sys.exit(1)

    print(f"Loading {STATUS_FILE}...")
    data = load_yaml(STATUS_FILE)

    kg = KnowledgeGraph(db_path=str(Path(os.environ.get("MEMPALACE_KG", DEFAULT_KG_PATH))))
    before = kg.stats()

    print(f"Seeding KG (currently {before.get('triples', 0)} triples)...")
    added, skipped = seed(kg, data)

    after = kg.stats()
    print(f"\nDone.")
    print(f"  Triples added : {added}")
    print(f"  Petitions skipped : {skipped}")
    print(f"  KG totals     : {after.get('entities', 0)} entities, {after.get('triples', 0)} triples")

    # Show predicate breakdown
    predicates = after.get("predicates") or {}
    if predicates:
        print("\n  Predicates:")
        for pred, count in sorted(predicates.items()):
            print(f"    {pred}: {count}")


if __name__ == "__main__":
    main()
