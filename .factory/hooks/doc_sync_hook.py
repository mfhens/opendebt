#!/usr/bin/env python3
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path


POLICY_CONTEXT = (
    "OpenDebt documentation policy: when you change source, API, or runtime configuration files, "
    "you must review and update impacted documentation if needed. Check "
    "docs/architecture-overview.md, docs/development-process-rules-and-workflows.md, "
    "AGENTS.md/agents.md, and relevant docs/adr/*.md files. Before finishing, include a line "
    "starting with 'Documentation impact:' that lists updated docs or says 'reviewed; no updates "
    "needed' with a short reason."
)


def main():
    payload = load_payload()
    event = payload.get("hook_event_name", "")
    project_dir = resolve_project_dir(payload)
    if not project_dir:
        return 0

    state_path = get_state_path(project_dir, payload.get("session_id", "default"))

    if event == "SessionStart":
        state = load_state(state_path)
        state["session_snapshot"] = build_snapshot(project_dir)
        state["prompt_snapshot"] = None
        state["prompt_transcript_offset"] = transcript_size(payload.get("transcript_path"))
        save_state(state_path, state)
        emit_additional_context("SessionStart", POLICY_CONTEXT)
        return 0

    if event == "UserPromptSubmit":
        state = load_state(state_path)
        state["prompt_snapshot"] = build_snapshot(project_dir)
        state["prompt_transcript_offset"] = transcript_size(payload.get("transcript_path"))
        save_state(state_path, state)
        emit_additional_context("UserPromptSubmit", POLICY_CONTEXT)
        return 0

    if event in {"Stop", "SubagentStop"}:
        state = load_state(state_path)
        baseline = state.get("prompt_snapshot") or state.get("session_snapshot")
        if not baseline:
            return 0

        current = build_snapshot(project_dir)
        changed_this_turn = diff_snapshots(baseline, current)
        relevant_source_changes = [
            path for path in changed_this_turn if is_relevant_source_change(path)
        ]

        if not relevant_source_changes:
            return 0

        transcript_path = payload.get("transcript_path")
        transcript_offset = state.get("prompt_transcript_offset", 0)
        if transcript_mentions_doc_impact(transcript_path, transcript_offset):
            return 0

        documentation_changes = [path for path in changed_this_turn if is_documentation_change(path)]
        block_with_reason(relevant_source_changes, documentation_changes)
        return 0

    if event == "SessionEnd":
        try:
            state_path.unlink(missing_ok=True)
        except OSError:
            pass
        return 0

    return 0


def load_payload():
    try:
        return json.load(sys.stdin)
    except json.JSONDecodeError:
        return {}


def resolve_project_dir(payload):
    env_dir = os.environ.get("FACTORY_PROJECT_DIR")
    if env_dir:
        return Path(env_dir)

    cwd = payload.get("cwd")
    if cwd:
        return Path(cwd)

    return None


def get_state_path(project_dir, session_id):
    state_dir = project_dir / ".factory" / "hook-state" / "doc-sync"
    state_dir.mkdir(parents=True, exist_ok=True)
    safe_session_id = session_id.replace(os.sep, "_") if session_id else "default"
    return state_dir / f"{safe_session_id}.json"


def load_state(state_path):
    if not state_path.exists():
        return {}

    try:
        return json.loads(state_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}


def save_state(state_path, state):
    state_path.write_text(json.dumps(state, indent=2, sort_keys=True), encoding="utf-8")


def emit_additional_context(event_name, context):
    print(
        json.dumps(
            {
                "hookSpecificOutput": {
                    "hookEventName": event_name,
                    "additionalContext": context,
                }
            }
        )
    )


def block_with_reason(relevant_source_changes, documentation_changes):
    summarized_paths = ", ".join(relevant_source_changes[:5])
    if len(relevant_source_changes) > 5:
        summarized_paths += ", ..."

    if documentation_changes:
        reason = (
            "Documentation files were changed but the required summary is missing. Source/config changes in this turn: "
            f"{summarized_paths}. Add a final line starting with 'Documentation impact:' listing the docs you updated."
        )
    else:
        reason = (
            "Documentation review is missing for source/config changes in this turn: "
            f"{summarized_paths}. Update impacted documentation or include a final line starting with "
            "'Documentation impact:' explaining which docs were updated or why no doc changes were required."
        )
    print(json.dumps({"decision": "block", "reason": reason}))


def build_snapshot(project_dir):
    changed_files = get_changed_files(project_dir)
    return {
        path: hash_file(project_dir / Path(path.replace("/", os.sep)))
        for path in changed_files
    }


def get_changed_files(project_dir):
    files = set()
    has_head = run_git(project_dir, ["rev-parse", "--verify", "HEAD"]).returncode == 0

    commands = []
    if has_head:
        commands.extend(
            [
                ["diff", "--name-only", "HEAD", "--"],
                ["diff", "--cached", "--name-only", "HEAD", "--"],
            ]
        )
    else:
        commands.append(["ls-files", "--modified"])

    commands.append(["ls-files", "--others", "--exclude-standard"])

    for command in commands:
        result = run_git(project_dir, command)
        if result.returncode != 0:
            continue
        for line in result.stdout.splitlines():
            normalized = normalize_path(line.strip())
            if normalized:
                files.add(normalized)

    return sorted(files)


def run_git(project_dir, args):
    return subprocess.run(
        ["git", *args],
        cwd=project_dir,
        check=False,
        capture_output=True,
        text=True,
    )


def normalize_path(path):
    return path.replace("\\", "/").strip()


def hash_file(path):
    if not path.exists() or path.is_dir():
        return None

    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def diff_snapshots(before, after):
    changed = set()
    for path in set(before) | set(after):
        if before.get(path) != after.get(path):
            changed.add(path)
    return sorted(changed)


def is_relevant_source_change(path):
    lower = path.lower()
    if lower.startswith(("docs/", "petitions/", ".factory/", ".github/", "target/")):
        return False
    if "/src/test/" in lower or lower.startswith("src/test/"):
        return False
    if lower.endswith((".md", ".txt", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".pdf")):
        return False
    if "/src/main/" in lower or lower.startswith("src/main/"):
        return True
    if lower == "pom.xml" or lower.endswith("/pom.xml"):
        return True
    if lower.startswith(("api-specs/", "config/", "k8s/", "rules-dmn/")):
        return True
    if lower.startswith("docker-compose") and lower.endswith((".yml", ".yaml")):
        return True
    return lower.endswith((
        ".java",
        ".kt",
        ".xml",
        ".yml",
        ".yaml",
        ".properties",
        ".sql",
        ".drl",
        ".json",
    ))


def is_documentation_change(path):
    lower = path.lower()
    return lower.startswith("docs/") or lower == "agents.md" or lower.endswith("/agents.md")


def transcript_size(transcript_path):
    if not transcript_path:
        return 0
    try:
        return Path(transcript_path).stat().st_size
    except OSError:
        return 0


def transcript_mentions_doc_impact(transcript_path, offset):
    if not transcript_path:
        return False

    try:
        with Path(transcript_path).open("rb") as handle:
            if offset:
                handle.seek(offset)
            text = handle.read().decode("utf-8", errors="ignore")
    except OSError:
        return False

    return "documentation impact:" in text.lower()


if __name__ == "__main__":
    sys.exit(main())
