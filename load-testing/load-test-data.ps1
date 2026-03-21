<#
.SYNOPSIS
    Seeds 1 million fordringer and ~4 million bogføringsposter into a
    locally running OpenDebt PostgreSQL instance for batch interest
    calculation load testing.

.DESCRIPTION
    Runs the following scripts in order:
      01_seed_debts.sql          → opendebt_debt    (~1–3 min)
      02_seed_ledger_entries.sql → opendebt_payment (~3–8 min)

    Both databases must already have Flyway migrations applied
    (i.e. the application services must have started at least once).

.PARAMETER PgHost
    PostgreSQL host. Default: localhost

.PARAMETER PgPort
    PostgreSQL port. Default: 5432

.PARAMETER PgUser
    PostgreSQL user. Default: opendebt

.PARAMETER PgPassword
    PostgreSQL password. Default: opendebt

.PARAMETER DebtDb
    Debt service database name. Default: opendebt_debt

.PARAMETER PaymentDb
    Payment service database name. Default: opendebt_payment

.PARAMETER UseDocker
    If set, runs psql inside the running Docker container instead of
    using a locally installed psql binary.

.PARAMETER DockerContainer
    Docker container name/ID to exec into. Default: opendebt-postgres-1

.PARAMETER SkipDebts
    Skip 01_seed_debts.sql (useful if already seeded).

.PARAMETER SkipLedger
    Skip 02_seed_ledger_entries.sql.

.PARAMETER Cleanup
    Run cleanup scripts first (removes previous load test data).

.EXAMPLE
    # Basic usage — all defaults
    .\load-test-data.ps1

.EXAMPLE
    # Custom credentials
    .\load-test-data.ps1 -PgUser postgres -PgPassword secret

.EXAMPLE
    # PostgreSQL is running in Docker (no local psql required)
    .\load-test-data.ps1 -UseDocker -DockerContainer opendebt-postgres-1

.EXAMPLE
    # Cleanup previous run then re-seed
    .\load-test-data.ps1 -Cleanup

.EXAMPLE
    # Only seed debts, skip ledger (e.g. different DB on different host)
    .\load-test-data.ps1 -SkipLedger
#>
[CmdletBinding()]
param(
    [string] $PgHost          = "localhost",
    [int]    $PgPort          = 5432,
    [string] $PgUser          = "opendebt",
    [string] $PgPassword      = "opendebt",
    [string] $DebtDb          = "opendebt_debt",
    [string] $PaymentDb       = "opendebt_payment",
    [switch] $UseDocker,
    [string] $DockerContainer = "opendebt-postgres-1",
    [switch] $SkipDebts,
    [switch] $SkipLedger,
    [switch] $Cleanup
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ScriptDir = $PSScriptRoot

# ── Colour helpers ────────────────────────────────────────────────────────────
function Write-Step  { param($msg) Write-Host "`n▶  $msg" -ForegroundColor Cyan   }
function Write-Ok    { param($msg) Write-Host "   ✓  $msg" -ForegroundColor Green  }
function Write-Warn  { param($msg) Write-Host "   ⚠  $msg" -ForegroundColor Yellow }
function Write-Fatal { param($msg) Write-Host "   ✗  $msg" -ForegroundColor Red    }

# ── Locate psql ───────────────────────────────────────────────────────────────
function Find-Psql {
    if ($UseDocker) { return $null }   # Docker mode — no local psql needed

    $cmd = Get-Command psql -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    # Common Windows installation paths (newest first)
    $candidates = @(
        "C:\Program Files\PostgreSQL\17\bin\psql.exe",
        "C:\Program Files\PostgreSQL\16\bin\psql.exe",
        "C:\Program Files\PostgreSQL\15\bin\psql.exe",
        "C:\Program Files\PostgreSQL\14\bin\psql.exe",
        "C:\Program Files\PostgreSQL\13\bin\psql.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    return $null
}

# ── Run a single SQL file ─────────────────────────────────────────────────────
function Invoke-SqlFile {
    param(
        [string] $SqlFile,
        [string] $Database
    )

    if (-not (Test-Path $SqlFile)) {
        Write-Fatal "SQL file not found: $SqlFile"
        exit 1
    }

    $fileName = Split-Path $SqlFile -Leaf

    if ($UseDocker) {
        # Copy the file into the container then execute it
        Write-Step "[$fileName] → $Database  (Docker: $DockerContainer)"

        & docker cp $SqlFile "${DockerContainer}:/tmp/$fileName" 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Fatal "docker cp failed — is the container '$DockerContainer' running?"
            exit 1
        }

        & docker exec -e PGPASSWORD=$PgPassword $DockerContainer `
            psql -h localhost -p 5432 -U $PgUser -d $Database `
            -f "/tmp/$fileName" --echo-errors
    }
    else {
        Write-Step "[$fileName] → $Database"
        $env:PGPASSWORD = $PgPassword
        & $psqlPath `
            -h $PgHost -p $PgPort -U $PgUser -d $Database `
            -f $SqlFile --echo-errors
        Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Fatal "psql exited with code $LASTEXITCODE for $fileName"
        exit $LASTEXITCODE
    }
    Write-Ok "Done: $fileName"
}

# ── Measure elapsed time ──────────────────────────────────────────────────────
function Format-Elapsed {
    param([TimeSpan] $ts)
    if ($ts.TotalMinutes -ge 1) {
        return "$([Math]::Floor($ts.TotalMinutes))m $($ts.Seconds)s"
    }
    return "$($ts.Seconds)s"
}

# ── Main ──────────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  OpenDebt — Load Test Data Seeder" -ForegroundColor Magenta
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  Host     : $PgHost`:$PgPort"
Write-Host "  Debt DB  : $DebtDb"
Write-Host "  Payment  : $PaymentDb"
Write-Host "  Mode     : $(if ($UseDocker) { "Docker ($DockerContainer)" } else { "Local psql" })"
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Magenta

# Locate psql (or validate Docker)
$psqlPath = $null
if ($UseDocker) {
    $docker = Get-Command docker -ErrorAction SilentlyContinue
    if (-not $docker) {
        Write-Fatal "docker not found in PATH. Install Docker Desktop or omit -UseDocker."
        exit 1
    }
    $running = & docker ps --filter "name=$DockerContainer" --format "{{.Names}}" 2>&1
    if (-not ($running -match $DockerContainer)) {
        Write-Fatal "Container '$DockerContainer' is not running. Start it with 'docker compose up -d postgres'."
        exit 1
    }
    Write-Ok "Docker container '$DockerContainer' is running."
}
else {
    $psqlPath = Find-Psql
    if (-not $psqlPath) {
        Write-Fatal @"
psql not found.

Options:
  1. Install PostgreSQL client tools and add to PATH:
       winget install PostgreSQL.PostgreSQL
  2. Run with -UseDocker if PostgreSQL is in a Docker container:
       .\load-test-data.ps1 -UseDocker
"@
        exit 1
    }
    Write-Ok "Found psql: $psqlPath"

    # Quick connectivity check
    $env:PGPASSWORD = $PgPassword
    $ping = & $psqlPath -h $PgHost -p $PgPort -U $PgUser -d $DebtDb -c "SELECT 1" -t --no-align 2>&1
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
    if ($LASTEXITCODE -ne 0) {
        Write-Fatal "Cannot connect to $DebtDb on $PgHost`:$PgPort as $PgUser. Check that PostgreSQL is running and Flyway migrations have been applied."
        exit 1
    }
    Write-Ok "Connected to PostgreSQL successfully."
}

$totalStart = Get-Date

# ── Optional cleanup ──────────────────────────────────────────────────────────
if ($Cleanup) {
    Write-Host ""
    Write-Warn "Cleanup mode — removing previous load test data..."

    $cleanDebts  = Join-Path $ScriptDir "00_cleanup_debts.sql"
    $cleanLedger = Join-Path $ScriptDir "00_cleanup_ledger.sql"

    $t = Measure-Command { Invoke-SqlFile -SqlFile $cleanLedger -Database $PaymentDb }
    Write-Ok "Ledger cleanup completed in $(Format-Elapsed $t)"

    $t = Measure-Command { Invoke-SqlFile -SqlFile $cleanDebts -Database $DebtDb }
    Write-Ok "Debt cleanup completed in $(Format-Elapsed $t)"
}

# ── Seed debts ────────────────────────────────────────────────────────────────
if (-not $SkipDebts) {
    Write-Host ""
    Write-Warn "Seeding 1 000 000 fordringer — this takes 1–3 minutes..."
    $debtFile = Join-Path $ScriptDir "01_seed_debts.sql"
    $t = Measure-Command { Invoke-SqlFile -SqlFile $debtFile -Database $DebtDb }
    Write-Ok "Debt seeding completed in $(Format-Elapsed $t)"
}
else {
    Write-Warn "Skipping debt seeding (-SkipDebts)"
}

# ── Seed ledger entries ───────────────────────────────────────────────────────
if (-not $SkipLedger) {
    Write-Host ""
    Write-Warn "Seeding ~4 000 000 bogføringsposter — this takes 3–8 minutes..."
    $ledgerFile = Join-Path $ScriptDir "02_seed_ledger_entries.sql"
    $t = Measure-Command { Invoke-SqlFile -SqlFile $ledgerFile -Database $PaymentDb }
    Write-Ok "Ledger seeding completed in $(Format-Elapsed $t)"
}
else {
    Write-Warn "Skipping ledger seeding (-SkipLedger)"
}

# ── Summary ───────────────────────────────────────────────────────────────────
$totalElapsed = (Get-Date) - $totalStart

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  Seeding complete in $(Format-Elapsed $totalElapsed)" -ForegroundColor Green
Write-Host ""
Write-Host "  Next steps:" -ForegroundColor Cyan
Write-Host "    • Trigger the interest accrual batch via the debt-service"
Write-Host "      actuator endpoint:"
Write-Host "        curl -X POST http://localhost:8082/debt-service/actuator/batch/interest-accrual" -ForegroundColor DarkGray
Write-Host ""
Write-Host "    • Or wait for the scheduled run at 02:30 UTC."
Write-Host ""
Write-Host "    • Monitor progress:"
Write-Host "        SELECT * FROM batch_job_executions ORDER BY started_at DESC LIMIT 5;" -ForegroundColor DarkGray
Write-Host "        SELECT COUNT(*) FROM interest_journal_entries;" -ForegroundColor DarkGray
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""
