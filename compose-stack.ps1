param(
    [ValidateSet("up", "down", "restart", "ps", "logs", "pull", "build")]
    [string]$Action = "up",

    [ValidateSet("all", "app", "obs", "infra")]
    [string]$Stack = "all",

    [switch]$Build,
    [switch]$NoBuild,
    [switch]$RemoveVolumes,
    [string]$Service,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ExtraArgs
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($Build -and $NoBuild) {
    throw "Use either -Build or -NoBuild, not both."
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$appCompose = Join-Path $scriptDir "docker-compose.yml"
$obsCompose = Join-Path $scriptDir "docker-compose.observability.yml"

if (-not (Test-Path $appCompose)) {
    throw "Missing compose file: $appCompose"
}
if (-not (Test-Path $obsCompose)) {
    throw "Missing compose file: $obsCompose"
}

function Get-ComposeFiles {
    param([string]$SelectedStack)

    switch ($SelectedStack) {
        "app" { return @($appCompose) }
        "obs" { return @($obsCompose) }
        "infra" { return @($appCompose, $obsCompose) }
        default { return @($appCompose, $obsCompose) }
    }
}

function Get-InfraServices {
    return @(
        "postgres",
        "keycloak",
        "otel-collector",
        "tempo",
        "loki",
        "promtail",
        "prometheus",
        "grafana"
    )
}

$files = Get-ComposeFiles -SelectedStack $Stack
$infraServices = Get-InfraServices
$useInfraServices = ($Stack -eq "infra" -and -not $Service)
$args = @()

foreach ($file in $files) {
    $args += @("-f", $file)
}

switch ($Action) {
    "up" {
        $args += @("up", "-d")
        if ($Build) { $args += "--build" }
        if ($NoBuild) { $args += "--no-build" }
        if ($Service) { $args += $Service }
        elseif ($useInfraServices) { $args += $infraServices }
    }
    "down" {
        if ($Stack -eq "infra") {
            $args += @("rm", "-s", "-f")
            if ($RemoveVolumes) { $args += "-v" }
            if ($Service) { $args += $Service }
            else { $args += $infraServices }
        } else {
            $args += "down"
            if ($RemoveVolumes) { $args += "-v" }
        }
    }
    "restart" {
        $args += "restart"
        if ($Service) { $args += $Service }
        elseif ($useInfraServices) { $args += $infraServices }
    }
    "ps" {
        $args += "ps"
        if ($useInfraServices) { $args += $infraServices }
    }
    "logs" {
        $args += @("logs", "-f", "--tail", "200")
        if ($Service) { $args += $Service }
        elseif ($useInfraServices) { $args += $infraServices }
    }
    "pull" {
        $args += "pull"
        if ($Service) { $args += $Service }
        elseif ($useInfraServices) { $args += $infraServices }
    }
    "build" {
        $args += "build"
        if ($Service) { $args += $Service }
        elseif ($useInfraServices) { $args += $infraServices }
    }
}

if ($ExtraArgs) {
    $args += $ExtraArgs
}

Write-Host ("Running: docker compose " + ($args -join " ")) -ForegroundColor Yellow
& docker compose @args
exit $LASTEXITCODE
