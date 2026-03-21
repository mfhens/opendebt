# start-demo.ps1 — Start the full OpenDebt demo (creditor portal + caseworker portal + all backend services)
# Requires: Java 21, Maven, PostgreSQL (installed locally or via winget/choco/scoop)
#
# Usage:  .\start-demo.ps1                        # start everything
#         .\start-demo.ps1 -Stop                   # stop all Java services (leaves PostgreSQL running)
#         .\start-demo.ps1 -Only caseworker         # start only caseworker portal + its backends
#         .\start-demo.ps1 -Only creditor           # start only creditor portal + its backends

param(
    [switch]$Stop,
    [ValidateSet("all", "caseworker", "creditor")]
    [string]$Only = "all"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$PidDir  = Join-Path $ScriptDir ".demo-pids"
$LogDir  = Join-Path $ScriptDir ".demo-logs"

$PgPort = 5432
$PgUser = "opendebt"
$PgPass = "opendebt"

# All databases needed for the full demo
$AllDatabases = @("opendebt_case", "opendebt_debt", "opendebt_payment", "opendebt_creditor", "opendebt_person")

function Write-Status($msg) { Write-Host $msg -ForegroundColor Yellow }
function Write-Ok($msg)     { Write-Host $msg -ForegroundColor Green }
function Write-Err($msg)    { Write-Host $msg -ForegroundColor Red }

# ---------------------------------------------------------------------------
# Locate PostgreSQL binaries
# ---------------------------------------------------------------------------
function Find-PgBin {
    $psqlPath = Get-Command psql -ErrorAction SilentlyContinue
    if ($psqlPath) { return Split-Path $psqlPath.Source }

    $candidates = @(
        "$env:ProgramFiles\PostgreSQL\*\bin",
        "$env:ProgramFiles(x86)\PostgreSQL\*\bin",
        "C:\PostgreSQL\*\bin",
        "$env:LOCALAPPDATA\Programs\PostgreSQL\*\bin",
        "$env:USERPROFILE\scoop\apps\postgresql\current\bin",
        "C:\tools\postgresql-*\bin",
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
$startCreditor  = ($Only -eq "all" -or $Only -eq "creditor")

# Build the list of Maven modules to compile
$modules = [System.Collections.ArrayList]::new()
$databases = [System.Collections.ArrayList]::new()

# Shared backend services
[void]$modules.Add("opendebt-debt-service")
[void]$databases.Add("opendebt_debt")

if ($startCaseworker) {
    [void]$modules.Add("opendebt-case-service")
    [void]$modules.Add("opendebt-payment-service")
    [void]$modules.Add("opendebt-caseworker-portal")
    [void]$databases.Add("opendebt_case")
    [void]$databases.Add("opendebt_payment")
}
if ($startCreditor) {
    [void]$modules.Add("opendebt-creditor-service")
    [void]$modules.Add("opendebt-creditor-portal")
    [void]$databases.Add("opendebt_creditor")
}

# AIDEV-TODO: Add person-registry once PersonServiceImpl exists (currently skeleton only)
# [void]$modules.Add("opendebt-person-registry")
# [void]$databases.Add("opendebt_person")

$totalSteps = 7
$step = 0

# =========================================================================
# Main
# =========================================================================

# Clean up previous Java processes
try { Stop-JavaServices } catch {}
New-Item -ItemType Directory -Path $PidDir -Force | Out-Null
New-Item -ItemType Directory -Path $LogDir  -Force | Out-Null

# --- Step 1: Find PostgreSQL ---
$step++
Write-Status "[$step/$totalSteps] Locating PostgreSQL..."
$PgBin = Find-PgBin
if (-not $PgBin) {
    Write-Err "  PostgreSQL not found!"
    Write-Host ""
    Write-Host "  Install PostgreSQL 14+ using one of:"
    Write-Host "    winget install PostgreSQL.PostgreSQL.16"
    Write-Host "    choco install postgresql16"
    Write-Host "    scoop install postgresql"
    Write-Host ""
    exit 1
}
Write-Ok "  Found PostgreSQL in: $PgBin"

# --- Step 2: Ensure PostgreSQL is running ---
$step++
Write-Status "[$step/$totalSteps] Ensuring PostgreSQL is running on port $PgPort..."

$pgIsReady = Join-Path $PgBin "pg_isready"
$pgRunning = & $pgIsReady -p $PgPort 2>$null; $pgUp = ($LASTEXITCODE -eq 0)

if ($pgUp) {
    Write-Ok "  PostgreSQL already running on port $PgPort."
} else {
    Write-Host "  PostgreSQL not running. Starting with pg_ctl..."
    $pgCtl = Join-Path $PgBin "pg_ctl"
    & $pgCtl start -l (Join-Path $LogDir "postgresql.log") 2>&1 | Out-Null
    $ok = Wait-ForPg $PgBin 30
    if (-not $ok) {
        Write-Err "  PostgreSQL failed to start. Check .demo-logs\postgresql.log"
        exit 1
    }
    Write-Ok "  PostgreSQL started."
}

# --- Step 3: Create databases and role ---
$step++
Write-Status "[$step/$totalSteps] Ensuring databases exist..."
$env:PGPASSWORD = $PgPass
$env:PGCLIENTENCODING = 'UTF8'
$psql = Join-Path $PgBin "psql"

$roleExists = & $psql -h localhost -p $PgPort -U $PgUser -d postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='$PgUser'" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Cannot connect as '$PgUser'. Trying 'postgres' superuser..."
    $env:PGPASSWORD = ""
    $roleCheck = & $psql -h localhost -p $PgPort -U postgres -d postgres -tc "SELECT 1 FROM pg_roles WHERE rolname='$PgUser'" 2>$null
    if ($roleCheck -notmatch "1") {
        Write-Host "  Creating role '$PgUser'..."
        & $psql -h localhost -p $PgPort -U postgres -d postgres -c "CREATE ROLE $PgUser WITH LOGIN PASSWORD '$PgPass' CREATEDB;" 2>$null
    }
    $env:PGPASSWORD = $PgPass
}

foreach ($db in $databases) {
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
$step++
$moduleList = $modules -join ","
Write-Status "[$step/$totalSteps] Building service JARs ($moduleList)..."
mvn package -pl $moduleList -am -B -DskipTests -q
if ($LASTEXITCODE -ne 0) { Write-Err "Build failed!"; exit 1 }
Write-Ok "  Build complete."

# --- Step 5: Start backend services ---
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
    -Profile "local" -DbName "opendebt_debt"

# AIDEV-TODO: Start person-registry here once PersonServiceImpl exists
# Start-Service -Name "person-registry" ...


if ($startCaseworker) {
    Start-Service -Name "case-service" `
        -JarPattern "opendebt-case-service\target\opendebt-case-service-*.jar" `
        -Profile "local" -DbName "opendebt_case"

    Start-Service -Name "payment-service" `
        -JarPattern "opendebt-payment-service\target\opendebt-payment-service-*.jar" `
        -Profile "dev" -DbName "opendebt_payment"
}

if ($startCreditor) {
    Start-Service -Name "creditor-service" `
        -JarPattern "opendebt-creditor-service\target\opendebt-creditor-service-*.jar" `
        -Profile "local" -DbName "opendebt_creditor"
}

# --- Step 6: Wait for backend services ---
$step++
Write-Status "[$step/$totalSteps] Waiting for backend services..."

$ok = Wait-ForUrl "http://localhost:8082/debt-service/actuator/health"
if (-not $ok) { Write-Err "  debt-service failed! Check .demo-logs\debt-service-err.log"; exit 1 }
Write-Ok "  debt-service ready."

if ($startCaseworker) {
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

# --- Step 7: Start portal(s) ---
$step++
Write-Status "[$step/$totalSteps] Starting portal(s)..."

if ($startCaseworker) {
    Start-Service -Name "caseworker-portal" `
        -JarPattern "opendebt-caseworker-portal\target\opendebt-caseworker-portal-*.jar" `
        -Profile "dev" -DbName $null

    $ok = Wait-ForUrl "http://localhost:8087/caseworker-portal/actuator/health" 30
    if (-not $ok) { Write-Err "  caseworker-portal failed! Check .demo-logs\caseworker-portal-err.log"; exit 1 }
    Write-Ok "  caseworker-portal ready."
}

if ($startCreditor) {
    Start-Service -Name "creditor-portal" `
        -JarPattern "opendebt-creditor-portal\target\opendebt-creditor-portal-*.jar" `
        -Profile "dev" -DbName $null

    $ok = Wait-ForUrl "http://localhost:8085/creditor-portal/" 30
    if (-not $ok) { Write-Err "  creditor-portal failed! Check .demo-logs\creditor-portal-err.log"; exit 1 }
    Write-Ok "  creditor-portal ready."
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
    Write-Host "  Caseworker Portal:  http://localhost:8087/caseworker-portal/demo-login"
}
if ($startCreditor) {
    Write-Host "  Creditor Portal:    http://localhost:8085/creditor-portal/"
    Write-Host "    Fordringer:       http://localhost:8085/creditor-portal/fordringer"
    Write-Host "    Opret fordring:   http://localhost:8085/creditor-portal/fordring/ny"
}

Write-Host ""
Write-Host "  Backend APIs:"
Write-Host "    Debt API:         http://localhost:8082/debt-service/swagger-ui.html"

if ($startCaseworker) {
    Write-Host "    Case API:         http://localhost:8081/case-service/swagger-ui.html"
    Write-Host "    Payment API:      http://localhost:8083/payment-service/swagger-ui.html"
}
if ($startCreditor) {
    Write-Host "    Creditor API:     http://localhost:8092/creditor-service/swagger-ui.html"
}

Write-Host ""
Write-Host "  Stop with:          .\start-demo.ps1 -Stop"
Write-Host "  Logs in:            .demo-logs\"
Write-Host ""
Write-Host "  Note: PostgreSQL keeps running after -Stop."
