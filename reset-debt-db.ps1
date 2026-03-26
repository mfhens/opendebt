$env:PGPASSWORD = "opendebt"
& psql -h localhost -p 5432 -U opendebt -d postgres -c "DROP DATABASE IF EXISTS opendebt_debt;"
& psql -h localhost -p 5432 -U opendebt -d postgres -c "CREATE DATABASE opendebt_debt OWNER opendebt;"
Write-Host "opendebt_debt recreated."
