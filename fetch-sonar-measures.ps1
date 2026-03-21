param(
    [string]$ProjectKey = "mfhens_opendebt",
    [string]$OutputPath = "sonar-measures.json",
    [string]$BaseUrl = "https://sonarcloud.io",
    [string]$Token = $env:SONAR_TOKEN
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Token)) {
    throw "Set SONAR_TOKEN or pass -Token."
}

$authBytes = [System.Text.Encoding]::ASCII.GetBytes("${Token}:")
$headers = @{ Authorization = "Basic " + [Convert]::ToBase64String($authBytes) }

$metricKeys = @(
    # Coverage
    "coverage",
    "line_coverage",
    "branch_coverage",
    "uncovered_lines",
    "uncovered_conditions",
    "lines_to_cover",
    # Duplication
    "duplicated_lines_density",
    "duplicated_lines",
    "duplicated_blocks",
    "duplicated_files",
    # Size
    "ncloc",
    "lines",
    "files",
    "functions",
    "classes",
    # Reliability / Maintainability
    "bugs",
    "code_smells",
    "vulnerabilities",
    "security_hotspots",
    "sqale_debt_ratio",
    "reliability_rating",
    "security_rating",
    "sqale_rating"
) -join ","

# Overall project metrics
$uri = "$BaseUrl/api/measures/component?component=$ProjectKey&metricKeys=$metricKeys"
$response = Invoke-RestMethod -Method Get -Uri $uri -Headers $headers

# Per-module breakdown (coverage + duplication per module)
$modulesUri = "$BaseUrl/api/measures/component_tree?component=$ProjectKey&metricKeys=coverage,duplicated_lines_density,ncloc&strategy=children&ps=50"
$modulesResponse = Invoke-RestMethod -Method Get -Uri $modulesUri -Headers $headers

$payload = [ordered]@{
    fetchedAt  = (Get-Date).ToString("o")
    projectKey = $ProjectKey
    overall    = $response.component.measures
    modules    = $modulesResponse.components
}

$payload | ConvertTo-Json -Depth 10 | Set-Content -Path $OutputPath -Encoding utf8

# Pretty-print to console
Write-Host "`n=== Overall Project Metrics ===" -ForegroundColor Cyan

$measures = @{}
foreach ($m in $response.component.measures) {
    $measures[$m.metric] = $m.value
}

Write-Host "`n--- Test Coverage ---"
Write-Host ("  Coverage (overall):  {0,8}%" -f $measures["coverage"])
Write-Host ("  Line coverage:       {0,8}%" -f $measures["line_coverage"])
Write-Host ("  Branch coverage:     {0,8}%" -f $measures["branch_coverage"])
Write-Host ("  Uncovered lines:     {0,8}"  -f $measures["uncovered_lines"])
Write-Host ("  Lines to cover:      {0,8}"  -f $measures["lines_to_cover"])

Write-Host "`n--- Duplication ---"
Write-Host ("  Duplication density: {0,8}%" -f $measures["duplicated_lines_density"])
Write-Host ("  Duplicated lines:    {0,8}"  -f $measures["duplicated_lines"])
Write-Host ("  Duplicated blocks:   {0,8}"  -f $measures["duplicated_blocks"])
Write-Host ("  Duplicated files:    {0,8}"  -f $measures["duplicated_files"])

Write-Host "`n--- Size ---"
Write-Host ("  Lines of code:       {0,8}"  -f $measures["ncloc"])
Write-Host ("  Files:               {0,8}"  -f $measures["files"])

Write-Host "`n--- Quality Gates ---"
Write-Host ("  Bugs:                {0,8}"  -f $measures["bugs"])
Write-Host ("  Code smells:         {0,8}"  -f $measures["code_smells"])
Write-Host ("  Vulnerabilities:     {0,8}"  -f $measures["vulnerabilities"])
Write-Host ("  Tech debt ratio:     {0,8}%" -f $measures["sqale_debt_ratio"])

Write-Host "`n=== Per-Module Breakdown ===" -ForegroundColor Cyan
$modulesResponse.components |
    Sort-Object name |
    ForEach-Object {
        $mod = $_
        $covMeasure = $mod.measures | Where-Object { $_.metric -eq "coverage" } | Select-Object -First 1
        $dupMeasure = $mod.measures | Where-Object { $_.metric -eq "duplicated_lines_density" } | Select-Object -First 1
        $locMeasure = $mod.measures | Where-Object { $_.metric -eq "ncloc" } | Select-Object -First 1
        $covStr = if ($covMeasure -and $covMeasure.PSObject.Properties["value"]) { "$($covMeasure.value)%" } else { "N/A" }
        $dupStr = if ($dupMeasure -and $dupMeasure.PSObject.Properties["value"]) { "$($dupMeasure.value)%" } else { "0%" }
        $loc    = if ($locMeasure -and $locMeasure.PSObject.Properties["value"]) { $locMeasure.value } else { "0" }
        Write-Host ("  {0,-45} coverage={1,6}  dup={2,6}  loc={3}" -f $mod.name, $covStr, $dupStr, $loc)
    }

Write-Host "`nSaved to $OutputPath"
