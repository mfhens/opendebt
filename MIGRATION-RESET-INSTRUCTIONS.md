# Database Migration Reset Instructions

**Date:** 2026-03-17  
**Reason:** Consolidation of V1-V7 migrations into single baseline for cleaner rapid development

## What Changed?

The debt-service migrations have been consolidated from 7 separate files (V1-V7) into a single `V1__baseline.sql` file. Old migrations are archived in `.archive/` folder for reference.

## Reset Your Local Database

### Option 1: Using the Startup Script (Recommended)

```powershell
# Stop all services
.\stop-portal-demo.ps1

# Remove the PostgreSQL data directory
Remove-Item -Recurse -Force .demo-pgdata

# Start fresh (will recreate DB and run V1__baseline.sql)
.\start-portal-demo.ps1
```

### Option 2: Manual Reset

```powershell
# Stop the services
.\stop-portal-demo.ps1

# Connect to PostgreSQL and drop the database
# (Adjust if your PostgreSQL is running differently)

# Start services again
.\start-portal-demo.ps1
```

## For CI/CD and Test Environments

If you have existing Flyway migration history in test/staging environments:

1. **Drop and recreate the database** - cleanest approach for non-production
2. The new `V1__baseline.sql` will create the complete schema in one pass

## What About Production?

We are **not in production yet**. This consolidation is only appropriate during rapid development phase. Once we go live, all migrations must be append-only (V8, V9, etc.) and never consolidated.

## Verification

After reset, verify the schema:

```sql
SELECT version, description, installed_on 
FROM flyway_schema_history 
ORDER BY installed_rank;
```

You should see only:
- `1` | `baseline` | `<timestamp>`

## Questions?

If you encounter issues, check:
- `.demo-logs/postgres.log` for database errors
- `.demo-logs/debt-service.log` for Flyway errors
