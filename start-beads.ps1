# start-beads.ps1 — Start Dolt SQL server for Beads issue tracking.
# Run at the start of each session before using bd commands.
#
# Usage:
#   .\start-beads.ps1              # Start server (default port 3307)
#   .\start-beads.ps1 -Stop        # Stop the running server
#   .\start-beads.ps1 -Status      # Check if server is running

param(
    [string]$DataDir = "$env:USERPROFILE\.beads-dolt\opendebt",
    [int]$Port = 3307,
    [string]$Host = "127.0.0.1",
    [switch]$Stop,
    [switch]$Status
)

$ErrorActionPreference = "Stop"

function Test-DoltRunning {
    $tcpClient = $null
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $connect = $tcpClient.BeginConnect($Host, $Port, $null, $null)
        $wait = $connect.AsyncWaitHandle.WaitOne(500, $false)
        return $wait -and $tcpClient.Connected
    } catch {
        return $false
    } finally {
        if ($tcpClient) { $tcpClient.Close() }
    }
}

if ($Status) {
    if (Test-DoltRunning) {
        Write-Host "✓ Dolt SQL server is running on ${Host}:${Port}" -ForegroundColor Green
    } else {
        Write-Host "✗ Dolt SQL server is NOT running on ${Host}:${Port}" -ForegroundColor Red
    }
    exit 0
}

if ($Stop) {
    $procs = Get-Process dolt -ErrorAction SilentlyContinue
    if ($procs) {
        $procs | ForEach-Object { Stop-Process -Id $_.Id -Force }
        Write-Host "✓ Dolt SQL server stopped" -ForegroundColor Yellow
    } else {
        Write-Host "No Dolt process found" -ForegroundColor Gray
    }
    exit 0
}

# --- Start ---

if (Test-DoltRunning) {
    Write-Host "✓ Dolt SQL server already running on ${Host}:${Port}" -ForegroundColor Green
    exit 0
}

if (-not (Test-Path $DataDir)) {
    Write-Error "Dolt data directory not found: $DataDir`nRun: dolt init in that directory first"
    exit 1
}

if (-not (Get-Command dolt -ErrorAction SilentlyContinue)) {
    Write-Error "dolt not found in PATH. Install from https://github.com/dolthub/dolt/releases"
    exit 1
}

Write-Host "Starting Dolt SQL server (${Host}:${Port})..." -ForegroundColor Cyan

$proc = Start-Process dolt `
    -ArgumentList "sql-server", "--port", $Port, "--host", $Host `
    -WorkingDirectory $DataDir `
    -WindowStyle Hidden `
    -PassThru

# Wait up to 5 seconds for it to respond
$started = $false
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Milliseconds 500
    if (Test-DoltRunning) { $started = $true; break }
}

if ($started) {
    Write-Host "✓ Dolt SQL server started (PID $($proc.Id), port $Port)" -ForegroundColor Green
    Write-Host "  Run 'bd ready' to see available work" -ForegroundColor Gray
    Write-Host "  Run '.\start-beads.ps1 -Stop' to stop the server" -ForegroundColor Gray
} else {
    Write-Error "Dolt server did not respond on port $Port within 5 seconds"
    exit 1
}
