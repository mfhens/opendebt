#!/usr/bin/env bash
set -euo pipefail

# Defaults
PROJECT_KEY="mfhens_opendebt"
OUTPUT_PATH="sonar-feedback.json"
BASE_URL="https://sonarcloud.io"
PAGE_SIZE=500
SEVERITIES=""
TOKEN="${SONAR_TOKEN:-}"

usage() {
    cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Fetch unresolved issues from SonarCloud.

Options:
  -p, --project-key KEY    SonarCloud project key (default: $PROJECT_KEY)
  -o, --output PATH        Output JSON file path (default: $OUTPUT_PATH)
  -s, --severities LIST    Comma-separated severities (e.g., BLOCKER,CRITICAL)
  -u, --base-url URL       SonarCloud base URL (default: $BASE_URL)
  -n, --page-size N        Page size 1-500 (default: $PAGE_SIZE)
  -t, --token TOKEN        SonarCloud token (or set SONAR_TOKEN env var)
  -h, --help               Show this help message

Example:
  $(basename "$0") -s BLOCKER,CRITICAL -o issues.json
EOF
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -p|--project-key) PROJECT_KEY="$2"; shift 2 ;;
        -o|--output) OUTPUT_PATH="$2"; shift 2 ;;
        -s|--severities) SEVERITIES="$2"; shift 2 ;;
        -u|--base-url) BASE_URL="$2"; shift 2 ;;
        -n|--page-size) PAGE_SIZE="$2"; shift 2 ;;
        -t|--token) TOKEN="$2"; shift 2 ;;
        -h|--help) usage ;;
        *) echo "Unknown option: $1" >&2; exit 1 ;;
    esac
done

if [[ -z "$TOKEN" ]]; then
    echo "Error: Set SONAR_TOKEN or pass -t/--token." >&2
    exit 1
fi

if [[ "$PAGE_SIZE" -lt 1 || "$PAGE_SIZE" -gt 500 ]]; then
    echo "Error: PageSize must be between 1 and 500." >&2
    exit 1
fi

if ! command -v jq &>/dev/null; then
    echo "Error: jq is required but not installed." >&2
    exit 1
fi

AUTH=$(echo -n "${TOKEN}:" | base64)

fetch_page() {
    local page="$1"
    local url="${BASE_URL}/api/issues/search?componentKeys=${PROJECT_KEY}&resolved=false&ps=${PAGE_SIZE}&p=${page}"
    
    if [[ -n "$SEVERITIES" ]]; then
        url="${url}&severities=${SEVERITIES}"
    fi
    
    curl -s -H "Authorization: Basic ${AUTH}" "$url"
}

# Fetch first page to get total
response=$(fetch_page 1)
total=$(echo "$response" | jq -r '.total')
issues=$(echo "$response" | jq '.issues')

page=2
fetched=$(echo "$issues" | jq 'length')

while [[ "$fetched" -lt "$total" ]]; do
    response=$(fetch_page "$page")
    new_issues=$(echo "$response" | jq '.issues')
    issues=$(echo "$issues" "$new_issues" | jq -s 'add')
    fetched=$(echo "$issues" | jq 'length')
    ((page++))
done

# Build output JSON
jq -n \
    --arg fetchedAt "$(date -Iseconds)" \
    --arg baseUrl "$BASE_URL" \
    --arg projectKey "$PROJECT_KEY" \
    --argjson total "$total" \
    --arg severities "$SEVERITIES" \
    --argjson issues "$issues" \
    '{
        fetchedAt: $fetchedAt,
        baseUrl: $baseUrl,
        projectKey: $projectKey,
        total: $total,
        severities: (if $severities == "" then null else ($severities | split(",")) end),
        issues: $issues
    }' > "$OUTPUT_PATH"

echo "Saved $total issues to $OUTPUT_PATH"

# Print severity summary
echo "$issues" | jq -r 'group_by(.severity) | map({severity: .[0].severity, count: length}) | sort_by(-.count) | .[] | "\(.severity): \(.count)"'
