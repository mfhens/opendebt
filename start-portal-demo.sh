#!/usr/bin/env bash
# start-portal-demo.sh — Start the creditor portal with backend services (no Docker for Java)
# Requires: Java 21, Maven, Docker (for PostgreSQL only)
#
# Usage:  ./start-portal-demo.sh          # start everything
#         ./start-portal-demo.sh stop     # stop all services

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PID_DIR="$SCRIPT_DIR/.demo-pids"
LOG_DIR="$SCRIPT_DIR/.demo-logs"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

stop_all() {
    echo -e "${YELLOW}Stopping demo services...${NC}"
    for pidfile in "$PID_DIR"/*.pid; do
        [ -f "$pidfile" ] || continue
        pid=$(cat "$pidfile")
        name=$(basename "$pidfile" .pid)
        if kill -0 "$pid" 2>/dev/null; then
            echo "  Stopping $name (PID $pid)"
            kill "$pid" 2>/dev/null || true
        fi
        rm -f "$pidfile"
    done
    echo -e "${YELLOW}Stopping PostgreSQL container...${NC}"
    docker compose stop postgres 2>/dev/null || true
    echo -e "${GREEN}All stopped.${NC}"
}

if [ "${1:-}" = "stop" ]; then
    stop_all
    exit 0
fi

# Clean up any previous run
stop_all 2>/dev/null || true
mkdir -p "$PID_DIR" "$LOG_DIR"

# --- Step 1: PostgreSQL via Docker ---
echo -e "${YELLOW}[1/5] Starting PostgreSQL...${NC}"
docker compose up -d postgres 2>/dev/null
echo "  Waiting for PostgreSQL to be healthy..."
timeout 60 bash -c 'while ! docker compose ps postgres --format "{{.Status}}" 2>/dev/null | grep -q "healthy"; do sleep 2; done'

# Create databases if they don't exist
for db in opendebt_creditor opendebt_debt; do
    docker compose exec -T postgres psql -U opendebt -tc "SELECT 1 FROM pg_database WHERE datname='$db'" | grep -q 1 || \
        docker compose exec -T postgres psql -U opendebt -c "CREATE DATABASE $db;" 2>/dev/null
done
echo -e "${GREEN}  PostgreSQL ready.${NC}"

# --- Step 2: Build JARs ---
echo -e "${YELLOW}[2/5] Building service JARs...${NC}"
mvn package -pl opendebt-creditor-service,opendebt-debt-service,opendebt-creditor-portal -am -B -DskipTests -q
echo -e "${GREEN}  Build complete.${NC}"

# --- Step 3: Start creditor-service ---
echo -e "${YELLOW}[3/5] Starting creditor-service (port 8092)...${NC}"
java -jar opendebt-creditor-service/target/opendebt-creditor-service-*.jar \
    --spring.profiles.active=local \
    --spring.datasource.url=jdbc:postgresql://localhost:5432/opendebt_creditor \
    --spring.datasource.username=opendebt \
    --spring.datasource.password=opendebt \
    > "$LOG_DIR/creditor-service.log" 2>&1 &
echo $! > "$PID_DIR/creditor-service.pid"
echo "  PID $(cat "$PID_DIR/creditor-service.pid"), log: .demo-logs/creditor-service.log"

# --- Step 4: Start debt-service ---
echo -e "${YELLOW}[4/5] Starting debt-service (port 8082)...${NC}"
java -jar opendebt-debt-service/target/opendebt-debt-service-*.jar \
    --spring.profiles.active=local \
    --spring.datasource.url=jdbc:postgresql://localhost:5432/opendebt_debt \
    --spring.datasource.username=opendebt \
    --spring.datasource.password=opendebt \
    > "$LOG_DIR/debt-service.log" 2>&1 &
echo $! > "$PID_DIR/debt-service.pid"
echo "  PID $(cat "$PID_DIR/debt-service.pid"), log: .demo-logs/debt-service.log"

# Wait for backend services
echo "  Waiting for backend services to start..."
for port in 8092 8082; do
    timeout 60 bash -c "while ! curl -sf http://localhost:$port/$([ $port = 8092 ] && echo creditor-service || echo debt-service)/actuator/health > /dev/null 2>&1; do sleep 2; done" || {
        echo -e "${RED}  Service on port $port failed to start. Check logs.${NC}"
        exit 1
    }
done
echo -e "${GREEN}  Backend services ready.${NC}"

# --- Step 5: Start creditor-portal ---
echo -e "${YELLOW}[5/5] Starting creditor-portal (port 8085)...${NC}"
java -jar opendebt-creditor-portal/target/opendebt-creditor-portal-*.jar \
    --spring.profiles.active=dev \
    > "$LOG_DIR/creditor-portal.log" 2>&1 &
echo $! > "$PID_DIR/creditor-portal.pid"
echo "  PID $(cat "$PID_DIR/creditor-portal.pid"), log: .demo-logs/creditor-portal.log"

timeout 30 bash -c 'while ! curl -sf http://localhost:8085/creditor-portal/ > /dev/null 2>&1; do sleep 2; done' || {
    echo -e "${RED}  Portal failed to start. Check .demo-logs/creditor-portal.log${NC}"
    exit 1
}

echo ""
echo -e "${GREEN}=============================================${NC}"
echo -e "${GREEN}  Portal demo is running!${NC}"
echo -e "${GREEN}=============================================${NC}"
echo ""
echo "  Portal:           http://localhost:8085/creditor-portal/"
echo "  Fordringer:        http://localhost:8085/creditor-portal/fordringer"
echo "  Opret fordring:    http://localhost:8085/creditor-portal/fordring/ny"
echo "  Tilgængelighed:    http://localhost:8085/creditor-portal/was"
echo ""
echo "  Creditor API:      http://localhost:8092/creditor-service/swagger-ui.html"
echo "  Debt API:          http://localhost:8082/debt-service/swagger-ui.html"
echo ""
echo "  Stop with:         ./start-portal-demo.sh stop"
echo "  Logs in:           .demo-logs/"
