#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Seeds petition059 E2E test data and runs the Playwright harness.

.DESCRIPTION
    1. Applies petition059-case-seed.sql to opendebt_case DB
    2. Applies petition059-debt-seed.sql to opendebt_debt DB
    3. Runs the @backlog-tagged petition059 spec (red→green pass)

.PARAMETER Stack
    Root directory of the opendebt repository (defaults to two levels above this script).

.PARAMETER SeedOnly
    If set, only applies seed data without running tests.

.PARAMETER TestOnly
    If set, only runs tests without re-seeding.

.EXAMPLE
    .\apply-petition059-seed.ps1
    .\apply-petition059-seed.ps1 -SeedOnly
    .\apply-petition059-seed.ps1 -TestOnly
#>
param(
    [string]$Stack = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path,
    [switch]$SeedOnly,
    [switch]$TestOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$seedDir = $PSScriptRoot
$e2eDir  = Join-Path $Stack 'opendebt-e2e'

function Write-Step([string]$msg) {
    Write-Host ""
    Write-Host "── $msg" -ForegroundColor Cyan
}

# ── 1. Verify docker compose is running ───────────────────────────────────────

Write-Step "Checking docker compose stack"
$running = docker compose -f (Join-Path $Stack 'docker-compose.yml') ps --services --filter status=running 2>$null
if ($running -notmatch 'postgres') {
    Write-Host "ERROR: postgres container is not running. Start the stack first:" -ForegroundColor Red
    Write-Host "  docker compose up -d" -ForegroundColor Yellow
    exit 1
}
Write-Host "postgres is running ✓"

if (-not $TestOnly) {
    # ── 2. Apply case seed ────────────────────────────────────────────────────

    Write-Step "Seeding opendebt_case (cases + case_debts)"
    $caseSeed = Get-Content (Join-Path $seedDir 'petition059-case-seed.sql') -Raw
    $caseSeed | docker compose -f (Join-Path $Stack 'docker-compose.yml') exec -T postgres `
        psql -U opendebt -d opendebt_case
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: case seed failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "Case seed applied ✓"

    # ── 3. Apply debt seed ────────────────────────────────────────────────────

    Write-Step "Seeding opendebt_debt (foraeldelse_record)"
    $debtSeed = Get-Content (Join-Path $seedDir 'petition059-debt-seed.sql') -Raw
    $debtSeed | docker compose -f (Join-Path $Stack 'docker-compose.yml') exec -T postgres `
        psql -U opendebt -d opendebt_debt
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: debt seed failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "Debt seed applied ✓"

    Write-Host ""
    Write-Host "Seed UUIDs:" -ForegroundColor Gray
    Write-Host "  Case A / Fordring A  (pairs[0]): 05900000-e2e0-0059-fd00-000000000001" -ForegroundColor Gray
    Write-Host "  Case B / Fordring B  (pairs[1]): 05900000-e2e0-0059-fd00-000000000002" -ForegroundColor Gray
    Write-Host "  Case C / Fordring C  (pairs[2]): 05900000-e2e0-0059-fd00-000000000003" -ForegroundColor Gray
}

if ($SeedOnly) {
    Write-Host ""
    Write-Host "Seed complete. Run tests with: .\apply-petition059-seed.ps1 -TestOnly" -ForegroundColor Green
    exit 0
}

# ── 4. Run Playwright tests ───────────────────────────────────────────────────

Write-Step "Running petition059 E2E harness (red→green pass)"
Push-Location $e2eDir
try {
    # @backlog is excluded only in CI (process.env.CI). Local runs include it.
    # Run the specific spec to get a clean isolated result.
    npx playwright test tests/caseworker-portal/petition059-limitation-panel.spec.ts `
        --reporter=list
    $exitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "All petition059 E2E tests passed ✓" -ForegroundColor Green
} else {
    Write-Host "Some tests failed (exit code $exitCode) — check output above." -ForegroundColor Yellow
    Write-Host "State-dependent tests (FR-7.2–FR-7.6) require the seed data to be" -ForegroundColor Yellow
    Write-Host "discoverable via case-service. Verify:" -ForegroundColor Yellow
    Write-Host "  GET http://localhost:8081/case-service/api/v1/cases?size=30" -ForegroundColor Gray
    Write-Host "  GET http://localhost:8082/debt-service/api/v1/foraeldelse/05900000-e2e0-0059-fd00-000000000001" -ForegroundColor Gray
}

exit $exitCode
