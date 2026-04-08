# start-demo.ps1 — Start the full OpenDebt demo (citizen portal + creditor portal + caseworker portal + all backend services)
# Requires: Java 21, Maven, Docker (postgres/keycloak/observability via compose)
#
# Usage:  .\start-demo.ps1                         # start everything (fast dev mode, no auth)
#         .\start-demo.ps1 -SecurityDemo           # start with Keycloak auth enabled
#         .\start-demo.ps1 -Stop                    # stop all Java services (leaves Docker infra running)
#         .\start-demo.ps1 -Only caseworker         # start only caseworker portal + its backends
#         .\start-demo.ps1 -Only creditor           # start only creditor portal + its backends
#         .\start-demo.ps1 -Only citizen            # start only citizen portal + its backends
#         .\start-demo.ps1 -SecurityDemo -Only creditor

param(
    [switch]$Stop,
    [ValidateSet("all", "caseworker", "creditor", "citizen")]
    [string]$Only = "all",
    [switch]$SecurityDemo
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$PidDir  = Join-Path $ScriptDir ".demo-pids"
$LogDir  = Join-Path $ScriptDir ".demo-logs"

$PgPort = 5432
$PgUser = "opendebt"
$PgPass = "opendebt"

# AES-256 key for person-registry (Base64 of 32 bytes). Same default as docker-compose.yml — dev/local only.
# Inherited by java child processes (person-registry reads ENCRYPTION_KEY via application.yml).
if (-not $env:ENCRYPTION_KEY) {
    $env:ENCRYPTION_KEY = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="
}

$DockerInfraServices = @("postgres", "keycloak", "otel-collector", "tempo", "loki", "promtail", "prometheus", "grafana", "immudb")
$KeycloakIssuerUri = "http://localhost:8080/realms/opendebt"
$CaseworkerPortalClientSecret = "caseworker-portal-dev-secret"
$CreditorPortalClientSecret = "creditor-portal-dev-secret"
$CitizenPortalClientSecret = "citizen-dev-secret"

function Write-Status($msg) { Write-Host $msg -ForegroundColor Yellow }
function Write-Ok($msg)     { Write-Host $msg -ForegroundColor Green }
function Write-Err($msg)    { Write-Host $msg -ForegroundColor Red }

function Wait-ForUrl {
    param([string]$Url, [int]$TimeoutSec = 60)
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $r = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3 -ErrorAction SilentlyContinue
            if ($r.StatusCode -eq 200) { return $true }
        } catch {}
        Start-Sleep -Seconds 2
    }
    return $false
}

function Get-RunningInfraServices {
    $appCompose = Join-Path $ScriptDir "docker-compose.yml"
    $obsCompose = Join-Path $ScriptDir "docker-compose.observability.yml"
    $demoCompose = Join-Path $ScriptDir "docker-compose.demo.yml"

    $running = & docker compose -f $appCompose -f $obsCompose -f $demoCompose ps --status running --services 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $running) {
        return @()
    }

    return @($running)
}

function Ensure-DockerInfra {
    Write-Status "Ensuring Docker infra is running (postgres, keycloak, observability)..."

    $dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $dockerCmd) {
        Write-Err "  Docker CLI not found. Install/start Docker Desktop first."
        exit 1
    }

    $runningServices = Get-RunningInfraServices
    $missingServices = @($DockerInfraServices | Where-Object { $_ -notin $runningServices })

    if ($missingServices.Count -eq 0) {
        Write-Ok "  Docker infra already running."
        return
    }

    Write-Host "  Missing infra services: $($missingServices -join ', ')"

    $appCompose  = Join-Path $ScriptDir "docker-compose.yml"
    $obsCompose  = Join-Path $ScriptDir "docker-compose.observability.yml"
    $demoCompose = Join-Path $ScriptDir "docker-compose.demo.yml"

    # Start infra with the demo override: prometheus and promtail reach host services
    # via host.docker.internal instead of Docker container hostnames.
    $composeArgs = @("-f", $appCompose, "-f", $obsCompose, "-f", $demoCompose, "up", "-d") + $DockerInfraServices
    & docker compose @composeArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Err "  Failed to start Docker infra services."
        exit 1
    }

    $pgOk = Wait-ForUrl "http://localhost:5432" 5
    if (-not $pgOk) {
        # Port check via HTTP is expected to fail for postgres; use tcp probe below.
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $async = $tcp.BeginConnect("localhost", $PgPort, $null, $null)
            $connected = $async.AsyncWaitHandle.WaitOne(15000, $false)
            if (-not $connected) {
                Write-Err "  PostgreSQL container did not open port $PgPort in time."
                exit 1
            }
            $tcp.EndConnect($async)
            $tcp.Close()
        } catch {
            Write-Err "  PostgreSQL container is not reachable on port $PgPort."
            exit 1
        }
    }

    $keycloakOk = Wait-ForUrl "http://localhost:8080" 90
    if (-not $keycloakOk) {
        Write-Err "  Keycloak did not become ready in time."
        exit 1
    }

    $immudbOk = Wait-ForUrl "http://localhost:9497/metrics" 30
    if (-not $immudbOk) {
        Write-Err "  immudb did not become ready on port 9497 in time."
        exit 1
    }

    Write-Ok "  Docker infra ready."
}

# ---------------------------------------------------------------------------
# Stop all Java services
# ---------------------------------------------------------------------------
function Stop-JavaServices {
    Write-Status "Stopping Java services..."
    if (Test-Path $PidDir) {
        Get-ChildItem "$PidDir\*.pid" -ErrorAction SilentlyContinue | ForEach-Object {
            $procId = [int](Get-Content $_.FullName)
            $name = $_.BaseName
            try {
                $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
                if ($proc) {
                    Write-Host "  Stopping $name (PID $procId)"
                    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                }
            } catch {}
            Remove-Item $_.FullName -Force
        }
    }
    Write-Ok "Java services stopped."
}

if ($Stop) {
    Stop-JavaServices
    exit 0
}

# =========================================================================
# Determine which services to start based on -Only parameter
# =========================================================================
$startCaseworker = ($Only -eq "all" -or $Only -eq "caseworker")
$startCreditor   = ($Only -eq "all" -or $Only -eq "creditor")
$startCitizen    = ($Only -eq "all" -or $Only -eq "citizen")

$backendProfile = if ($SecurityDemo) { "demo-auth" } else { "dev" }
$portalProfile = if ($SecurityDemo) { "local" } else { "dev" }

if ($SecurityDemo) {
    Write-Status "Security demo mode enabled: Keycloak/OIDC login required for portal and backend access."
}

# Build the list of Maven modules to compile
$modules = [System.Collections.ArrayList]::new()

# Shared backend services
[void]$modules.Add("opendebt-debt-service")

if ($startCaseworker -or $startCitizen) {
    [void]$modules.Add("opendebt-case-service")
    [void]$modules.Add("opendebt-payment-service")
}
if ($startCaseworker) {
    [void]$modules.Add("opendebt-caseworker-portal")
}
if ($startCitizen) {
    [void]$modules.Add("opendebt-citizen-portal")
}
if ($startCreditor) {
    [void]$modules.Add("opendebt-creditor-service")
    [void]$modules.Add("opendebt-creditor-portal")
}

# Shared backend: person-registry (PII store, needed by all portals)
[void]$modules.Add("opendebt-person-registry")

$totalSteps = 5
$step = 0

# =========================================================================
# Main
# =========================================================================

# Clean up previous Java processes
try { Stop-JavaServices } catch {}
New-Item -ItemType Directory -Path $PidDir -Force | Out-Null
New-Item -ItemType Directory -Path $LogDir  -Force | Out-Null

# --- Step 1: Ensure Docker infra is running ---
$step++
Write-Status "[$step/$totalSteps] Checking/starting Docker infra..."
Ensure-DockerInfra

# --- Activate mise environment (if available) for Java + Maven ---
$miseCmd = Get-Command mise -ErrorAction SilentlyContinue
if ($miseCmd) {
    Write-Host "  Activating mise environment..."
    $miseEnv = & $miseCmd.Source env --shell pwsh 2>$null
    if ($miseEnv) { $miseEnv | Invoke-Expression }
}

# --- Step 2: Build JARs ---
$step++
$moduleList = $modules -join ","
Write-Status "[$step/$totalSteps] Building service JARs ($moduleList)..."
mvn package -pl $moduleList -am -B -DskipTests -q
if ($LASTEXITCODE -ne 0) { Write-Err "Build failed!"; exit 1 }
Write-Ok "  Build complete."

# --- Step 3: Start backend services ---
$step++
Write-Status "[$step/$totalSteps] Starting backend services..."

$DbUrl = "jdbc:postgresql://localhost:${PgPort}"

# Helper to start a Java service
function Start-Service {
    param(
        [string]$Name,
        [string]$JarPattern,
        [string]$Profile,
        [string]$DbName,
        [hashtable]$ExtraArgs = @{}
    )
    $jar = Get-ChildItem $JarPattern -Exclude "*.original" | Select-Object -First 1
    if (-not $jar) {
        Write-Err "  JAR not found for $Name ($JarPattern)"
        return $null
    }
    $args = @("-jar", $jar.FullName, "--spring.profiles.active=$Profile")
    if ($DbName) {
        $args += "--spring.datasource.url=${DbUrl}/$DbName"
        $args += "--spring.datasource.username=$PgUser"
        $args += "--spring.datasource.password=$PgPass"
    }
    foreach ($key in $ExtraArgs.Keys) {
        $args += "--${key}=$($ExtraArgs[$key])"
    }
    $proc = Start-Process -FilePath java -ArgumentList $args `
        -RedirectStandardOutput "$LogDir\$Name.log" `
        -RedirectStandardError "$LogDir\$Name-err.log" `
        -PassThru -WindowStyle Hidden
    Set-Content -Path "$PidDir\$Name.pid" -Value $proc.Id
    Write-Host ("  {0,-24} PID {1}" -f $Name, $proc.Id)
    return $proc
}

# -- debt-service (shared by both portals) --
Start-Service -Name "debt-service" `
    -JarPattern "opendebt-debt-service\target\opendebt-debt-service-*.jar" `
    -Profile $backendProfile -DbName "opendebt_debt" `
    -ExtraArgs @{
        "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
        "KEYCLOAK_JWK_URI" = "$KeycloakIssuerUri/protocol/openid-connect/certs"
    }

# -- person-registry (shared PII store, needed by all portals) --
Start-Service -Name "person-registry" `
    -JarPattern "opendebt-person-registry\target\opendebt-person-registry-*.jar" `
    -Profile $backendProfile -DbName "opendebt_person" `
    -ExtraArgs @{
        "KEYCLOAK_ISSUER_URI"     = $KeycloakIssuerUri
        "KEYCLOAK_JWK_URI"        = "$KeycloakIssuerUri/protocol/openid-connect/certs"
        "opendebt.encryption.key" = $env:ENCRYPTION_KEY
    }


if ($startCaseworker -or $startCitizen) {
    Start-Service -Name "case-service" `
        -JarPattern "opendebt-case-service\target\opendebt-case-service-*.jar" `
        -Profile $backendProfile -DbName "opendebt_case" `
        -ExtraArgs @{
            "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
            "KEYCLOAK_JWK_URI" = "$KeycloakIssuerUri/protocol/openid-connect/certs"
        }

    Start-Service -Name "payment-service" `
        -JarPattern "opendebt-payment-service\target\opendebt-payment-service-*.jar" `
        -Profile $backendProfile -DbName "opendebt_payment" `
        -ExtraArgs @{
            "KEYCLOAK_ISSUER_URI"          = $KeycloakIssuerUri
            "KEYCLOAK_JWK_URI"             = "$KeycloakIssuerUri/protocol/openid-connect/certs"
            "opendebt.immudb.enabled"      = "true"
            "opendebt.immudb.host"         = "localhost"
            "opendebt.immudb.port"         = "3322"
            "opendebt.immudb.username"     = "immudb"
            "opendebt.immudb.password"     = "immudb"
            "opendebt.immudb.database"     = "defaultdb"
        }
}

if ($startCreditor) {
    Start-Service -Name "creditor-service" `
        -JarPattern "opendebt-creditor-service\target\opendebt-creditor-service-*.jar" `
        -Profile $backendProfile -DbName "opendebt_creditor" `
        -ExtraArgs @{
            "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
            "KEYCLOAK_JWK_URI" = "$KeycloakIssuerUri/protocol/openid-connect/certs"
        }
}

# --- Step 4: Wait for backend services ---
$step++
Write-Status "[$step/$totalSteps] Waiting for backend services..."

$ok = Wait-ForUrl "http://localhost:8082/debt-service/actuator/health"
if (-not $ok) { Write-Err "  debt-service failed! Check .demo-logs\debt-service-err.log"; exit 1 }
Write-Ok "  debt-service ready."

$ok = Wait-ForUrl "http://localhost:8090/person-registry/actuator/health"
if (-not $ok) { Write-Err "  person-registry failed! Check .demo-logs\person-registry-err.log"; exit 1 }
Write-Ok "  person-registry ready."

if ($startCaseworker -or $startCitizen) {
    $ok = Wait-ForUrl "http://localhost:8081/case-service/actuator/health"
    if (-not $ok) { Write-Err "  case-service failed! Check .demo-logs\case-service-err.log"; exit 1 }
    Write-Ok "  case-service ready."

    $ok = Wait-ForUrl "http://localhost:8083/payment-service/actuator/health"
    if (-not $ok) { Write-Err "  payment-service failed! Check .demo-logs\payment-service-err.log"; exit 1 }
    Write-Ok "  payment-service ready."
}

if ($startCreditor) {
    $ok = Wait-ForUrl "http://localhost:8092/creditor-service/actuator/health"
    if (-not $ok) { Write-Err "  creditor-service failed! Check .demo-logs\creditor-service-err.log"; exit 1 }
    Write-Ok "  creditor-service ready."
}

# --- Step 5: Start portal(s) ---
$step++
Write-Status "[$step/$totalSteps] Starting portal(s)..."

if ($startCaseworker) {
    Start-Service -Name "caseworker-portal" `
        -JarPattern "opendebt-caseworker-portal\target\opendebt-caseworker-portal-*.jar" `
        -Profile $portalProfile -DbName $null `
        -ExtraArgs @{
            "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
            "KEYCLOAK_CLIENT_SECRET" = $CaseworkerPortalClientSecret
        }

    $ok = Wait-ForUrl "http://localhost:8087/caseworker-portal/actuator/health" 30
    if (-not $ok) { Write-Err "  caseworker-portal failed! Check .demo-logs\caseworker-portal-err.log"; exit 1 }
    Write-Ok "  caseworker-portal ready."
}

if ($startCreditor) {
    Start-Service -Name "creditor-portal" `
        -JarPattern "opendebt-creditor-portal\target\opendebt-creditor-portal-*.jar" `
        -Profile $portalProfile -DbName $null `
        -ExtraArgs @{
            "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
            "KEYCLOAK_CLIENT_SECRET" = $CreditorPortalClientSecret
        }

    $ok = Wait-ForUrl "http://localhost:8085/creditor-portal/" 30
    if (-not $ok) { Write-Err "  creditor-portal failed! Check .demo-logs\creditor-portal-err.log"; exit 1 }
    Write-Ok "  creditor-portal ready."
}

if ($startCitizen) {
    Start-Service -Name "citizen-portal" `
        -JarPattern "opendebt-citizen-portal\target\opendebt-citizen-portal-*.jar" `
        -Profile $portalProfile -DbName $null `
        -ExtraArgs @{
            "KEYCLOAK_ISSUER_URI" = $KeycloakIssuerUri
            "TASTSELV_CLIENT_SECRET" = $CitizenPortalClientSecret
        }

    $ok = Wait-ForUrl "http://localhost:8086/borger/actuator/health" 30
    if (-not $ok) { Write-Err "  citizen-portal failed! Check .demo-logs\citizen-portal-err.log"; exit 1 }
    Write-Ok "  citizen-portal ready."
}

# =========================================================================
# Summary
# =========================================================================
Write-Host ""
Write-Ok "============================================="
Write-Ok "  OpenDebt demo is running!"
Write-Ok "============================================="
Write-Host ""

if ($startCaseworker) {
    if ($SecurityDemo) {
        Write-Host "  Caseworker Portal:  http://localhost:8087/caseworker-portal/"
    } else {
        Write-Host "  Caseworker Portal:  http://localhost:8087/caseworker-portal/demo-login"
    }
}
if ($startCreditor) {
    Write-Host "  Creditor Portal:    http://localhost:8085/creditor-portal/"
    Write-Host "    Fordringer:       http://localhost:8085/creditor-portal/fordringer"
    Write-Host "    Opret fordring:   http://localhost:8085/creditor-portal/fordring/opret"
}
if ($startCitizen) {
    Write-Host "  Citizen Portal:     http://localhost:8086/borger/"
}

Write-Host ""
Write-Host "  Backend APIs:"
Write-Host "    Debt API:         http://localhost:8082/debt-service/swagger-ui.html"
Write-Host "    Person Registry:  http://localhost:8090/person-registry/swagger-ui.html"

if ($startCaseworker -or $startCitizen) {
    Write-Host "    Case API:         http://localhost:8081/case-service/swagger-ui.html"
    Write-Host "    Payment API:      http://localhost:8083/payment-service/swagger-ui.html"
}
if ($startCreditor) {
    Write-Host "    Creditor API:     http://localhost:8092/creditor-service/swagger-ui.html"
}

Write-Host ""
if ($SecurityDemo) {
    Write-Host "  Keycloak demo users:"
    Write-Host "    caseworker / caseworker123   (role: CASEWORKER)"
    Write-Host "    creditor   / creditor123     (role: CREDITOR)"
    Write-Host "    citizen    / citizen123      (role: CITIZEN)"
    Write-Host "    admin      / admin123        (role: ADMIN)"
    Write-Host "  Keycloak admin:"
    Write-Host "    admin / admin  -> http://localhost:8080/admin/"
    Write-Host ""
}
Write-Host "  Stop with:          .\start-demo.ps1 -Stop"
Write-Host "  Logs in:            .demo-logs\"
Write-Host ""
Write-Host "  Observability:"
Write-Host "    Grafana:          http://localhost:3000"
Write-Host "    Prometheus:       http://localhost:9090"
Write-Host "    (Traces via OTLP to localhost:4317/4318 -> Tempo)"
Write-Host ""
Write-Host "  Note: Docker infra keeps running after -Stop."
