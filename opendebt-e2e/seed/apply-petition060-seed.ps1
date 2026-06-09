#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Seeds petition060 E2E data and runs the Playwright harness.
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

Write-Step "Checking docker compose stack"
$running = docker compose -f (Join-Path $Stack 'docker-compose.yml') ps --services --filter status=running 2>$null
if ($running -notmatch 'postgres') {
    Write-Host "ERROR: postgres container is not running. Start the stack first:" -ForegroundColor Red
    Write-Host "  docker compose up -d" -ForegroundColor Yellow
    exit 1
}
Write-Host "postgres is running ✓"

if (-not $TestOnly) {
    Write-Step "Seeding opendebt_debt (section50 candidate items)"
    $debtSeed = Get-Content (Join-Path $seedDir 'petition060-debt-seed.sql') -Raw
    $debtSeed | docker compose -f (Join-Path $Stack 'docker-compose.yml') exec -T postgres `
        psql -U opendebt -d opendebt_debt
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: debt seed failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "Debt seed applied ✓"
}

if ($SeedOnly) {
    Write-Host ""
    Write-Host "Seed complete. Run tests with: .\apply-petition060-seed.ps1 -TestOnly" -ForegroundColor Green
    exit 0
}

Write-Step "Running petition060 E2E harness"
Push-Location $e2eDir
try {
    npx playwright test tests/caseworker-portal/petition060-retskraft-worklist.spec.ts `
        --reporter=list
    $exitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "All petition060 E2E tests passed ✓" -ForegroundColor Green
} else {
    Write-Host "Some tests failed (exit code $exitCode) — check output above." -ForegroundColor Yellow
}

exit $exitCode
