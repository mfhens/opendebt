# start-portal-demo.ps1 — Start the creditor portal with backend services (no Docker)
# Requires: Java 21, Maven, PostgreSQL (installed locally or via winget/choco/scoop)
#
# Usage:  .\start-portal-demo.ps1          # start everything
#         .\start-portal-demo.ps1 -Stop    # stop Java services (leaves PostgreSQL running)

param(
    [switch]$Stop
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$PidDir  = Join-Path $ScriptDir ".demo-pids"
$LogDir  = Join-Path $ScriptDir ".demo-logs"
$PgDataDir = Join-Path $ScriptDir ".demo-pgdata"

$PgPort = 5432
$PgUser = "opendebt"
$PgPass = "opendebt"
$Databases = @("opendebt_creditor", "opendebt_debt")

function Write-Status($msg) { Write-Host $msg -ForegroundColor Yellow }
function Write-Ok($msg)     { Write-Host $msg -ForegroundColor Green }
function Write-Err($msg)    { Write-Host $msg -ForegroundColor Red }

# ---------------------------------------------------------------------------
# Locate PostgreSQL binaries
# ---------------------------------------------------------------------------
function Find-PgBin {
    # Check PATH first
    $psqlPath = Get-Command psql -ErrorAction SilentlyContinue
    if ($psqlPath) { return Split-Path $psqlPath.Source }

    # Common Windows install locations
    $candidates = @(
        "$env:ProgramFiles\PostgreSQL\*\bin",
        "$env:ProgramFiles(x86)\PostgreSQL\*\bin",
        "C:\PostgreSQL\*\bin",
        "$env:LOCALAPPDATA\Programs\PostgreSQL\*\bin",
        # Scoop
        "$env:USERPROFILE\scoop\apps\postgresql\current\bin",
        # Chocolatey
        "C:\tools\postgresql-*\bin",
        # winget typical
        "$env:ProgramFiles\PostgreSQL\16\bin",
        "$env:ProgramFiles\PostgreSQL\15\bin",
        "$env:ProgramFiles\PostgreSQL\14\bin"
    )
    foreach ($pattern in $candidates) {
        $resolved = Resolve-Path $pattern -ErrorAction SilentlyContinue | Sort-Object -Descending | Select-Object -First 1
        if ($resolved -and (Test-Path (Join-Path $resolved.Path "psql.exe"))) {
            return $resolved.Path
        }
    }
    return $null
}

# ---------------------------------------------------------------------------
# Wait for a URL to return HTTP 200
# ---------------------------------------------------------------------------
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

# ---------------------------------------------------------------------------
# Wait for PostgreSQL to accept connections
# ---------------------------------------------------------------------------
function Wait-ForPg {
    param([string]$PgBin, [int]$TimeoutSec = 30)
    $pgIsReady = Join-Path $PgBin "pg_isready.exe"
    if (-not (Test-Path $pgIsReady)) { $pgIsReady = Join-Path $PgBin "pg_isready" }
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $result = & $pgIsReady -p $PgPort 2>$null
        if ($LASTEXITCODE -eq 0) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

# ---------------------------------------------------------------------------
# Run psql command
# ---------------------------------------------------------------------------
function Invoke-Psql {
    param([string]$PgBin, [string]$Database, [string]$Command)
    $psql = Join-Path $PgBin "psql"
    $env:PGPASSWORD = $PgPass
    & $psql -h localhost -p $PgPort -U $PgUser -d $Database -tc $Command 2>$null
}

# ---------------------------------------------------------------------------
# Stop
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
# Main
# =========================================================================

# Clean up previous Java processes
try { Stop-JavaServices } catch {}
New-Item -ItemType Directory -Path $PidDir -Force | Out-Null
New-Item -ItemType Directory -Path $LogDir  -Force | Out-Null

# --- Step 1: Find PostgreSQL ---
Write-Status "[1/6] Locating PostgreSQL..."
$PgBin = Find-PgBin
if (-not $PgBin) {
    Write-Err "  PostgreSQL not found!"
    Write-Host ""
    Write-Host "  Install PostgreSQL 14+ using one of:"
    Write-Host "    winget install PostgreSQL.PostgreSQL.16"
    Write-Host "    choco install postgresql16"
    Write-Host "    scoop install postgresql"
    Write-Host ""
    Write-Host "  Then re-run this script."
    exit 1
}
Write-Ok "  Found PostgreSQL in: $PgBin"

# --- Step 2: Ensure PostgreSQL is running ---
Write-Status "[2/6] Ensuring PostgreSQL is running on port $PgPort..."

$pgIsReady = Join-Path $PgBin "pg_isready"
$pgRunning = & $pgIsReady -p $PgPort 2>$null; $pgUp = ($LASTEXITCODE -eq 0)

if ($pgUp) {
    Write-Ok "  PostgreSQL already running on port $PgPort."
} else {
    Write-Host "  PostgreSQL not running. Initializing a local instance..."
    $pgCtl   = Join-Path $PgBin "pg_ctl"
    $initdb  = Join-Path $PgBin "initdb"

    if (-not (Test-Path (Join-Path $PgDataDir "PG_VERSION"))) {
        Write-Host "  Initializing data directory: $PgDataDir"
        New-Item -ItemType Directory -Path $PgDataDir -Force | Out-Null
        $env:PGPASSWORD = $PgPass
        & $initdb -D $PgDataDir -U $PgUser -A md5 --pwfile=(New-TemporaryFile | ForEach-Object { Set-Content $_.FullName $PgPass; $_.FullName }) 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Err "  Failed to initialize PostgreSQL data directory."
            exit 1
        }
        # Set port in postgresql.conf
        $pgConf = Join-Path $PgDataDir "postgresql.conf"
        (Get-Content $pgConf) -replace "^#?port\s*=.*", "port = $PgPort" | Set-Content $pgConf
    }

    Write-Host "  Starting PostgreSQL..."
    & $pgCtl -D $PgDataDir -l (Join-Path $LogDir "postgresql.log") start 2>&1 | Out-Null
    $ok = Wait-ForPg $PgBin 30
    if (-not $ok) {
        Write-Err "  PostgreSQL failed to start. Check .demo-logs\postgresql.log"
        exit 1
    }
    Write-Ok "  PostgreSQL started."
}

# --- Step 3: Create databases and role ---
Write-Status "[3/6] Ensuring databases exist..."
$env:PGPASSWORD = $PgPass
$psql = Join-Path $PgBin "psql"

# Check if the opendebt role exists (may not if using system Postgres)
$roleExists = & $psql -h localhost -p $PgPort -U $PgUser -d postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='$PgUser'" 2>$null
if ($LASTEXITCODE -ne 0) {
    # Can't connect as opendebt user -- try the current OS user or postgres superuser
    Write-Host "  Cannot connect as '$PgUser'. Trying 'postgres' superuser..."
    $env:PGPASSWORD = ""
    $roleCheck = & $psql -h localhost -p $PgPort -U postgres -d postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='$PgUser'" 2>$null
    if ($roleCheck -notmatch "1") {
        Write-Host "  Creating role '$PgUser'..."
        & $psql -h localhost -p $PgPort -U postgres -d postgres -c "CREATE ROLE $PgUser WITH LOGIN PASSWORD '$PgPass' CREATEDB;" 2>$null
    }
    $env:PGPASSWORD = $PgPass
}

foreach ($db in $Databases) {
    $exists = & $psql -h localhost -p $PgPort -U $PgUser -d postgres -tc "SELECT 1 FROM pg_database WHERE datname='$db'" 2>$null
    if ($exists -notmatch "1") {
        Write-Host "  Creating database: $db"
        & $psql -h localhost -p $PgPort -U $PgUser -d postgres -c "CREATE DATABASE $db OWNER $PgUser;" 2>$null
    } else {
        Write-Host "  Database exists: $db"
    }
}
Write-Ok "  Databases ready."

# --- Activate mise environment (if available) for Java + Maven ---
$miseCmd = Get-Command mise -ErrorAction SilentlyContinue
if ($miseCmd) {
    Write-Host "  Activating mise environment..."
    $miseEnv = & $miseCmd.Source env --shell pwsh 2>$null
    if ($miseEnv) { $miseEnv | Invoke-Expression }
}

# --- Step 4: Build JARs ---
Write-Status "[4/6] Building service JARs..."
mvn package -pl opendebt-creditor-service,opendebt-debt-service,opendebt-creditor-portal -am -B -DskipTests -q
if ($LASTEXITCODE -ne 0) { Write-Err "Build failed!"; exit 1 }
Write-Ok "  Build complete."

# --- Step 5: Start backend services ---
Write-Status "[5/6] Starting backend services..."

$DbUrl = "jdbc:postgresql://localhost:${PgPort}"

# creditor-service
$creditorJar = Get-ChildItem "opendebt-creditor-service\target\opendebt-creditor-service-*.jar" -Exclude "*.original" | Select-Object -First 1
$creditorProc = Start-Process -FilePath java -ArgumentList @(
    "-jar", $creditorJar.FullName,
    "--spring.profiles.active=local",
    "--spring.datasource.url=${DbUrl}/opendebt_creditor",
    "--spring.datasource.username=$PgUser",
    "--spring.datasource.password=$PgPass"
) -RedirectStandardOutput "$LogDir\creditor-service.log" `
  -RedirectStandardError "$LogDir\creditor-service-err.log" `
  -PassThru -WindowStyle Hidden
Set-Content -Path "$PidDir\creditor-service.pid" -Value $creditorProc.Id
Write-Host "  creditor-service  PID $($creditorProc.Id)"

# debt-service
$debtJar = Get-ChildItem "opendebt-debt-service\target\opendebt-debt-service-*.jar" -Exclude "*.original" | Select-Object -First 1
$debtProc = Start-Process -FilePath java -ArgumentList @(
    "-jar", $debtJar.FullName,
    "--spring.profiles.active=local",
    "--spring.datasource.url=${DbUrl}/opendebt_debt",
    "--spring.datasource.username=$PgUser",
    "--spring.datasource.password=$PgPass"
) -RedirectStandardOutput "$LogDir\debt-service.log" `
  -RedirectStandardError "$LogDir\debt-service-err.log" `
  -PassThru -WindowStyle Hidden
Set-Content -Path "$PidDir\debt-service.pid" -Value $debtProc.Id
Write-Host "  debt-service      PID $($debtProc.Id)"

Write-Host "  Waiting for backend services..."
$ok = Wait-ForUrl "http://localhost:8092/creditor-service/actuator/health"
if (-not $ok) { Write-Err "  creditor-service failed! Check .demo-logs\creditor-service-err.log"; exit 1 }
$ok = Wait-ForUrl "http://localhost:8082/debt-service/actuator/health"
if (-not $ok) { Write-Err "  debt-service failed! Check .demo-logs\debt-service-err.log"; exit 1 }
Write-Ok "  Backend services ready."

# --- Step 6: Start portal ---
Write-Status "[6/6] Starting creditor-portal..."
$portalJar = Get-ChildItem "opendebt-creditor-portal\target\opendebt-creditor-portal-*.jar" -Exclude "*.original" | Select-Object -First 1
$portalProc = Start-Process -FilePath java -ArgumentList @(
    "-jar", $portalJar.FullName,
    "--spring.profiles.active=dev"
) -RedirectStandardOutput "$LogDir\creditor-portal.log" `
  -RedirectStandardError "$LogDir\creditor-portal-err.log" `
  -PassThru -WindowStyle Hidden
Set-Content -Path "$PidDir\creditor-portal.pid" -Value $portalProc.Id
Write-Host "  creditor-portal   PID $($portalProc.Id)"

$ok = Wait-ForUrl "http://localhost:8085/creditor-portal/" 30
if (-not $ok) { Write-Err "  Portal failed! Check .demo-logs\creditor-portal-err.log"; exit 1 }

Write-Host ""
Write-Ok "============================================="
Write-Ok "  Portal demo is running!"
Write-Ok "============================================="
Write-Host ""
Write-Host "  Portal:           http://localhost:8085/creditor-portal/"
Write-Host "  Fordringer:        http://localhost:8085/creditor-portal/fordringer"
Write-Host "  Opret fordring:    http://localhost:8085/creditor-portal/fordring/ny"
Write-Host "  Tilgaengelighed:   http://localhost:8085/creditor-portal/was"
Write-Host ""
Write-Host "  Creditor API:      http://localhost:8092/creditor-service/swagger-ui.html"
Write-Host "  Debt API:          http://localhost:8082/debt-service/swagger-ui.html"
Write-Host ""
Write-Host "  Stop with:         .\start-portal-demo.ps1 -Stop"
Write-Host "  Logs in:           .demo-logs\"
Write-Host ""
Write-Host "  Note: PostgreSQL keeps running after -Stop."
Write-Host "  Data in:           .demo-pgdata\ (if started by this script)"
