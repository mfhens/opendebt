#!/usr/bin/env bash
# start-demo.sh — Start the full OpenDebt demo (citizen portal + creditor portal + caseworker portal + all backend services)
# Requires: Java 21, Maven, Docker (postgres/keycloak/observability via compose)
#
# Usage:  ./start-demo.sh                          # start everything (fast dev mode, no auth)
#         ./start-demo.sh --security-demo          # start with Keycloak auth enabled
#         ./start-demo.sh --stop                   # stop all Java services (leaves Docker infra running)
#         ./start-demo.sh --only caseworker        # start only caseworker portal + its backends
#         ./start-demo.sh --only creditor          # start only creditor portal + its backends
#         ./start-demo.sh --only citizen           # start only citizen portal + its backends
#         ./start-demo.sh --security-demo --only creditor

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PID_DIR="$SCRIPT_DIR/.demo-pids"
LOG_DIR="$SCRIPT_DIR/.demo-logs"

PG_PORT=5432
PG_USER="opendebt"
PG_PASS="opendebt"

# AES-256 key for person-registry (Base64 of 32 bytes). Same default as docker-compose.yml — dev/local only.
ENCRYPTION_KEY="${ENCRYPTION_KEY:-YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=}"

DOCKER_INFRA_SERVICES=(postgres keycloak otel-collector tempo loki promtail prometheus grafana immudb)
KEYCLOAK_ISSUER_URI="http://localhost:8080/realms/opendebt"
CASEWORKER_PORTAL_CLIENT_SECRET="caseworker-portal-dev-secret"
CREDITOR_PORTAL_CLIENT_SECRET="creditor-portal-dev-secret"
CITIZEN_PORTAL_CLIENT_SECRET="citizen-dev-secret"

# ---------------------------------------------------------------------------
# Colour helpers
# ---------------------------------------------------------------------------
status() { printf '\033[33m%s\033[0m\n' "$*"; }
ok()     { printf '\033[32m%s\033[0m\n' "$*"; }
err()    { printf '\033[31m%s\033[0m\n' "$*" >&2; }

# ---------------------------------------------------------------------------
# Wait for an HTTP 200 from a URL (timeout in seconds, default 60)
# ---------------------------------------------------------------------------
wait_for_url() {
    local url="$1"
    local timeout="${2:-60}"
    local deadline=$(( $(date +%s) + timeout ))
    while (( $(date +%s) < deadline )); do
        if curl -sf --max-time 3 "$url" -o /dev/null 2>/dev/null; then
            return 0
        fi
        sleep 2
    done
    return 1
}

# ---------------------------------------------------------------------------
# TCP port probe (for PostgreSQL which doesn't speak HTTP)
# ---------------------------------------------------------------------------
wait_for_tcp() {
    local host="$1"
    local port="$2"
    local timeout="${3:-30}"
    local deadline=$(( $(date +%s) + timeout ))
    while (( $(date +%s) < deadline )); do
        if (echo > /dev/tcp/"$host"/"$port") 2>/dev/null; then
            return 0
        fi
        sleep 2
    done
    return 1
}

# ---------------------------------------------------------------------------
# Ensure Docker infra is running
# ---------------------------------------------------------------------------
ensure_docker_infra() {
    status "Ensuring Docker infra is running (postgres, keycloak, observability)..."

    if ! command -v docker &>/dev/null; then
        err "  Docker CLI not found. Install/start Docker first."
        exit 1
    fi

    local app_compose="$SCRIPT_DIR/docker-compose.yml"
    local obs_compose="$SCRIPT_DIR/docker-compose.observability.yml"
    local demo_compose="$SCRIPT_DIR/docker-compose.demo.yml"

    local running
    running=$(docker compose -f "$app_compose" -f "$obs_compose" -f "$demo_compose" \
        ps --status running --services 2>/dev/null || true)

    local missing=()
    for svc in "${DOCKER_INFRA_SERVICES[@]}"; do
        if ! grep -qx "$svc" <<< "$running"; then
            missing+=("$svc")
        fi
    done

    if (( ${#missing[@]} == 0 )); then
        ok "  Docker infra already running."
        return
    fi

    echo "  Missing infra services: ${missing[*]}"

    docker compose \
        -f "$app_compose" -f "$obs_compose" -f "$demo_compose" \
        up -d "${DOCKER_INFRA_SERVICES[@]}"

    if ! wait_for_tcp localhost "$PG_PORT" 30; then
        err "  PostgreSQL container did not open port $PG_PORT in time."
        exit 1
    fi

    if ! wait_for_url "http://localhost:8080" 90; then
        err "  Keycloak did not become ready in time."
        exit 1
    fi

    if ! wait_for_url "http://localhost:9497/metrics" 30; then
        err "  immudb did not become ready on port 9497 in time."
        exit 1
    fi

    ok "  Docker infra ready."
}

# ---------------------------------------------------------------------------
# Stop all Java services
# ---------------------------------------------------------------------------
stop_java_services() {
    status "Stopping Java services..."
    if [[ -d "$PID_DIR" ]]; then
        for pid_file in "$PID_DIR"/*.pid; do
            [[ -f "$pid_file" ]] || continue
            local name pid
            name="$(basename "$pid_file" .pid)"
            pid="$(cat "$pid_file")"
            if kill -0 "$pid" 2>/dev/null; then
                echo "  Stopping $name (PID $pid)"
                kill "$pid" 2>/dev/null || true
            fi
            rm -f "$pid_file"
        done
    fi
    ok "Java services stopped."
}

# ---------------------------------------------------------------------------
# Start a single Java service in the background
# ---------------------------------------------------------------------------
start_service() {
    local name="$1"
    local jar_glob="$2"
    local profile="$3"
    local db_name="$4"     # empty string = no DB args
    shift 4
    local extra_args=("$@")  # remaining args are --key=value pairs

    local jar
    jar=$(find . -path "./$jar_glob" ! -name "*.original" 2>/dev/null | sort | head -1)
    if [[ -z "$jar" ]]; then
        err "  JAR not found for $name ($jar_glob)"
        return 1
    fi

    local java_args=("-jar" "$jar" "--spring.profiles.active=$profile")

    if [[ -n "$db_name" ]]; then
        java_args+=(
            "--spring.datasource.url=jdbc:postgresql://localhost:${PG_PORT}/${db_name}"
            "--spring.datasource.username=$PG_USER"
            "--spring.datasource.password=$PG_PASS"
        )
    fi

    java_args+=("${extra_args[@]}")

    java "${java_args[@]}" \
        >"$LOG_DIR/$name.log" \
        2>"$LOG_DIR/$name-err.log" &

    local pid=$!
    echo "$pid" > "$PID_DIR/$name.pid"
    printf "  %-24s PID %s\n" "$name" "$pid"
}

# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------
STOP=false
ONLY="all"
SECURITY_DEMO=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --stop|-s)          STOP=true ;;
        --only|-o)          ONLY="${2:-all}"; shift ;;
        --security-demo)    SECURITY_DEMO=true ;;
        *)  err "Unknown argument: $1"; exit 1 ;;
    esac
    shift
done

if $STOP; then
    stop_java_services
    exit 0
fi

# ---------------------------------------------------------------------------
# Determine which portals/backends to start
# ---------------------------------------------------------------------------
START_CASEWORKER=false
START_CREDITOR=false
START_CITIZEN=false

case "$ONLY" in
    all)        START_CASEWORKER=true; START_CREDITOR=true; START_CITIZEN=true ;;
    caseworker) START_CASEWORKER=true ;;
    creditor)   START_CREDITOR=true ;;
    citizen)    START_CITIZEN=true ;;
    *) err "Invalid --only value: $ONLY (use all|caseworker|creditor|citizen)"; exit 1 ;;
esac

BACKEND_PROFILE="dev"
PORTAL_PROFILE="dev"
if $SECURITY_DEMO; then
    BACKEND_PROFILE="demo-auth"
    PORTAL_PROFILE="local"
    status "Security demo mode enabled: Keycloak/OIDC login required."
fi

# Build module list
MODULES=("opendebt-debt-service")
($START_CASEWORKER || $START_CITIZEN) && MODULES+=("opendebt-case-service" "opendebt-payment-service")
$START_CASEWORKER && MODULES+=("opendebt-caseworker-portal")
$START_CITIZEN    && MODULES+=("opendebt-citizen-portal")
$START_CREDITOR   && MODULES+=("opendebt-creditor-service" "opendebt-creditor-portal")
MODULES+=("opendebt-person-registry")

TOTAL_STEPS=5
STEP=0

# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

stop_java_services 2>/dev/null || true
mkdir -p "$PID_DIR" "$LOG_DIR"

# --- Step 1: Docker infra ---
STEP=$(( STEP + 1 ))
status "[$STEP/$TOTAL_STEPS] Checking/starting Docker infra..."
ensure_docker_infra

# --- Activate mise environment (if available) ---
if command -v mise &>/dev/null; then
    echo "  Activating mise environment..."
    eval "$(mise env --shell bash 2>/dev/null)" || true
fi

# --- Step 2: Build JARs ---
STEP=$(( STEP + 1 ))
MODULE_LIST=$(IFS=','; echo "${MODULES[*]}")
status "[$STEP/$TOTAL_STEPS] Building service JARs ($MODULE_LIST)..."
mvn package -pl "$MODULE_LIST" -am -B -DskipTests -q
ok "  Build complete."

# --- Step 3: Start backend services ---
STEP=$(( STEP + 1 ))
status "[$STEP/$TOTAL_STEPS] Starting backend services..."

start_service "debt-service" \
    "opendebt-debt-service/target/opendebt-debt-service-*.jar" \
    "$BACKEND_PROFILE" "opendebt_debt" \
    "--KEYCLOAK_ISSUER_URI=$KEYCLOAK_ISSUER_URI" \
    "--KEYCLOAK_JWK_URI=$KEYCLOAK_ISSUER_URI/protocol/openid-connect/certs"

start_service "person-registry" \
    "opendebt-person-registry/target/opendebt-person-registry-*.jar" \
    "$BACKEND_PROFILE" "opendebt_person" \
    "--KEYCLOAK_ISSUER_URI=$KEYCLOAK_ISSUER_URI" \
    "--KEYCLOAK_JWK_URI=$KEYCLOAK_ISSUER_URI/protocol/openid-connect/certs" \
    "--opendebt.encryption.key=$ENCRYPTION_KEY" \
    "--opendebt.demo.seed-organizations=true" \
    "--opendebt.demo.seed-persons=true"

if $START_CASEWORKER || $START_CITIZEN; then
    start_service "case-service" \
        "opendebt-case-service/target/opendebt-case-service-*.jar" \
        "$BACKEND_PROFILE" "opendebt_case" \
        "--KEYCLOAK_ISSUER_URI=$KEYCLOAK_ISSUER_URI" \
        "--KEYCLOAK_JWK_URI=$KEYCLOAK_ISSUER_URI/protocol/openid-connect/certs"

    start_service "payment-service" \
        "opendebt-payment-service/target/opendebt-payment-service-*.jar" \
        "$BACKEND_PROFILE" "opendebt_payment" \
        "--KEYCLOAK_ISSUER_URI=$KEYCLOAK_ISSUER_URI" \
        "--KEYCLOAK_JWK_URI=$KEYCLOAK_ISSUER_URI/protocol/openid-connect/certs" \
        "--opendebt.immudb.enabled=true" \
        "--opendebt.immudb.host=localhost" \
        "--opendebt.immudb.port=3322" \
        "--opendebt.immudb.username=immudb" \
        "--opendebt.immudb.password=immudb" \
        "--opendebt.immudb.database=defaultdb"
fi

if $START_CREDITOR; then
    start_service "creditor-service" \
        "opendebt-creditor-service/target/opendebt-creditor-service-*.jar" \
        "$BACKEND_PROFILE" "opendebt_creditor" \
        "--KEYCLOAK_ISSUER_URI=$KEYCLOAK_ISSUER_URI" \
        "--KEYCLOAK_JWK_URI=$KEYCLOAK_ISSUER_URI/protocol/openid-connect/certs"
fi

# --- Step 4: Wait for backend services ---
STEP=$(( STEP + 1 ))
status "[$STEP/$TOTAL_STEPS] Waiting for backend services..."

if ! wait_for_url "http://localhost:8082/debt-service/actuator/health"; then
    err "  debt-service failed! Check $LOG_DIR/debt-service-err.log"; exit 1
fi
ok "  debt-service ready."

if ! wait_for_url "http://localhost:8090/person-registry/actuator/health"; then
    err "  person-registry failed! Check $LOG_DIR/person-registry-err.log"; exit 1
fi
ok "  person-registry ready."

if $START_CASEWORKER || $START_CITIZEN; then
    if ! wait_for_url "http://localhost:8081/case-service/actuator/health"; then
        err "  case-service failed! Check $LOG_DIR/case-service-err.log"; exit 1
    fi
    ok "  case-service ready."

    if ! wait_for_url "http://localhost:8083/payment-service/actuator/health"; then
        err "  payment-service failed! Check $LOG_DIR/payment-service-err.log"; exit 1
    fi
    ok "  payment-service ready."
fi

if $START_CREDITOR; then
    if ! wait_for_url "http://localhost:8092/creditor-service/actuator/health"; then
        err "  creditor-service failed! Check $LOG_DIR/creditor-service-err.log"; exit 1
    fi
    ok "  creditor-service ready."
fi

# --- Step 5: Start portal(s) ---
STEP=$(( STEP + 1 ))
status "[$STEP/$TOTAL_STEPS] Starting portal(s)..."

if $START_CASEWORKER; then
    start_service "caseworker-portal" \
        "opendebt-caseworker-portal/target/opendebt-caseworker-portal-*.jar" \
        "$PORTAL_PROFILE" "" \
        "--KEYCLOAK_ISSUER_URI=$KEYCLOAK_ISSUER_URI" \
        "--KEYCLOAK_CLIENT_SECRET=$CASEWORKER_PORTAL_CLIENT_SECRET"

    if ! wait_for_url "http://localhost:8087/caseworker-portal/actuator/health" 30; then
        err "  caseworker-portal failed! Check $LOG_DIR/caseworker-portal-err.log"; exit 1
    fi
    ok "  caseworker-portal ready."
fi

if $START_CREDITOR; then
    start_service "creditor-portal" \
        "opendebt-creditor-portal/target/opendebt-creditor-portal-*.jar" \
        "$PORTAL_PROFILE" "" \
        "--KEYCLOAK_ISSUER_URI=$KEYCLOAK_ISSUER_URI" \
        "--KEYCLOAK_CLIENT_SECRET=$CREDITOR_PORTAL_CLIENT_SECRET"

    if ! wait_for_url "http://localhost:8085/creditor-portal/" 30; then
        err "  creditor-portal failed! Check $LOG_DIR/creditor-portal-err.log"; exit 1
    fi
    ok "  creditor-portal ready."
fi

if $START_CITIZEN; then
    start_service "citizen-portal" \
        "opendebt-citizen-portal/target/opendebt-citizen-portal-*.jar" \
        "$PORTAL_PROFILE" "" \
        "--KEYCLOAK_ISSUER_URI=$KEYCLOAK_ISSUER_URI" \
        "--TASTSELV_CLIENT_SECRET=$CITIZEN_PORTAL_CLIENT_SECRET"

    if ! wait_for_url "http://localhost:8086/borger/actuator/health" 30; then
        err "  citizen-portal failed! Check $LOG_DIR/citizen-portal-err.log"; exit 1
    fi
    ok "  citizen-portal ready."
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo ""
ok "============================================="
ok "  OpenDebt demo is running!"
ok "============================================="
echo ""

if $START_CASEWORKER; then
    if $SECURITY_DEMO; then
        echo "  Caseworker Portal:  http://localhost:8087/caseworker-portal/"
    else
        echo "  Caseworker Portal:  http://localhost:8087/caseworker-portal/demo-login"
    fi
fi
if $START_CREDITOR; then
    echo "  Creditor Portal:    http://localhost:8085/creditor-portal/"
    echo "    Fordringer:       http://localhost:8085/creditor-portal/fordringer"
    echo "    Opret fordring:   http://localhost:8085/creditor-portal/fordring/opret"
fi
if $START_CITIZEN; then
    echo "  Citizen Portal:     http://localhost:8086/borger/"
fi

echo ""
echo "  Backend APIs:"
echo "    Debt API:         http://localhost:8082/debt-service/swagger-ui.html"
echo "    Person Registry:  http://localhost:8090/person-registry/swagger-ui.html"

if $START_CASEWORKER || $START_CITIZEN; then
    echo "    Case API:         http://localhost:8081/case-service/swagger-ui.html"
    echo "    Payment API:      http://localhost:8083/payment-service/swagger-ui.html"
fi
if $START_CREDITOR; then
    echo "    Creditor API:     http://localhost:8092/creditor-service/swagger-ui.html"
fi

echo ""
if $SECURITY_DEMO; then
    echo "  Keycloak demo users:"
    echo "    caseworker / caseworker123   (role: CASEWORKER)"
    echo "    creditor   / creditor123     (role: CREDITOR)"
    echo "    citizen    / citizen123      (role: CITIZEN)"
    echo "    admin      / admin123        (role: ADMIN)"
    echo "  Keycloak admin:"
    echo "    admin / admin  -> http://localhost:8080/admin/"
    echo ""
fi
echo "  Stop with:          ./start-demo.sh --stop"
echo "  Logs in:            .demo-logs/"
echo ""
echo "  Observability:"
echo "    Grafana:          http://localhost:3000"
echo "    Prometheus:       http://localhost:9090"
echo "    (Traces via OTLP to localhost:4317/4318 -> Tempo)"
echo ""
