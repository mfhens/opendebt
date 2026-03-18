Add-Type -Assembly System.IO.Compression.FileSystem
$jar = 'C:\Users\AJ849XF\Documents\GitHub\opendebt\opendebt-debt-service\target\opendebt-debt-service-0.1.0-SNAPSHOT.jar'
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$zip.Entries | Where-Object { $_.FullName -like '*migration*' } | ForEach-Object { Write-Host $_.FullName }
$zip.Dispose()
