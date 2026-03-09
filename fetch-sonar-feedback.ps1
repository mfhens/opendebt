param(
    [string]$ProjectKey = "mfhens_opendebt",
    [string]$OutputPath = "sonar-feedback.json",
    [string[]]$Severities,
    [string]$BaseUrl = "https://sonarcloud.io",
    [int]$PageSize = 500,
    [string]$Token = $env:SONAR_TOKEN
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Token)) {
    throw "Set SONAR_TOKEN or pass -Token."
}

if ($PageSize -lt 1 -or $PageSize -gt 500) {
    throw "PageSize must be between 1 and 500."
}

$authBytes = [System.Text.Encoding]::ASCII.GetBytes("${Token}:")
$headers = @{ Authorization = "Basic " + [Convert]::ToBase64String($authBytes) }

$query = @{
    componentKeys = $ProjectKey
    resolved      = "false"
    ps            = $PageSize
}

if ($Severities -and $Severities.Count -gt 0) {
    $query.severities = ($Severities -join ",")
}

$issues = New-Object System.Collections.Generic.List[object]
$pageIndex = 1
$total = 0

do {
    $query.p = $pageIndex
    $queryString = ($query.GetEnumerator() | ForEach-Object {
            "{0}={1}" -f [uri]::EscapeDataString([string]$_.Key), [uri]::EscapeDataString([string]$_.Value)
        }) -join "&"

    $response = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/issues/search?$queryString" -Headers $headers

    if ($pageIndex -eq 1) {
        $total = [int]$response.total
    }

    foreach ($issue in $response.issues) {
        [void]$issues.Add($issue)
    }

    $pageIndex++
}
while ($issues.Count -lt $total)

$payload = [ordered]@{
    fetchedAt  = (Get-Date).ToString("o")
    baseUrl    = $BaseUrl
    projectKey = $ProjectKey
    total      = $total
    severities = $Severities
    issues     = $issues
}

$payload | ConvertTo-Json -Depth 100 | Set-Content -Path $OutputPath -Encoding utf8

Write-Host "Saved $total issues to $OutputPath"
$issues |
    Group-Object severity |
    Sort-Object Count -Descending |
    ForEach-Object { Write-Host ("{0}: {1}" -f $_.Name, $_.Count) }
