$env:PGPASSWORD = "opendebt"
& psql -h localhost -p 5432 -U opendebt -d postgres -c "DROP DATABASE IF EXISTS opendebt_payment;"
& psql -h localhost -p 5432 -U opendebt -d postgres -c "CREATE DATABASE opendebt_payment OWNER opendebt;"
Write-Host "opendebt_payment recreated."
