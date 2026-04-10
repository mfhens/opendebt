# ADR-0037: Performance SLA

**Status**: proposed  
**Date**: 2026-04-10  
**Deciders**: Architecture team

## Context

OpenDebt is a public-sector debt collection system that must meet SLA commitments
defined in OLA agreements with UFST's Horizontale Driftsplatform. No explicit
performance targets have been codified in a single ADR, creating a gap in the
NFR register (NFR-PERF-001 through NFR-PERF-003).

## Decision

The following performance targets apply to all services:

### Response time
- Synchronous API endpoints: **p95 < 500 ms** under normal load (≤ 50 concurrent
  users per service instance).
- Endpoints expected to exceed this threshold must be explicitly flagged in the
  petition specification and reviewed for caching, async offloading, or query
  optimisation before the petition is closed.

### Database access
- All new queries must use indexed columns in `WHERE` and `JOIN` clauses.
- Collection-returning queries must include `LIMIT`/pagination.
- N+1 query patterns are a blocking code-review finding.
- Query plans for new queries on tables expected to grow beyond 100 K rows must
  be reviewed before delivery.

### Asynchronous processing
- Operations estimated at > 1 second must execute asynchronously (Flowable
  process instance, Spring `@Async`, or scheduled job) and must not block
  the HTTP request thread.
- The petition spec must define the async mechanism and the status-feedback
  pattern (polling endpoint or event notification).

### Load testing
- The load-testing suite in `load-testing/` covers the nominal debt lifecycle
  flow. New high-traffic endpoints must add or extend a k6 scenario before
  delivery.

## Consequences

- NFR-PERF-001 through NFR-PERF-003 in `compliance/nfr-register.yaml` reference
  this ADR as their normative source.
- Petition specs that introduce new endpoints must include a performance
  classification (sync < 500 ms / async with status feedback).
- The `code-reviewer-strict` agent checks for the database access constraints
  (indexed columns, pagination) as a blocking finding.
- NFR-PERF-001 (p95 < 500 ms) has `test_hook: ~` — manual review against k6
  results is required until automated latency gating is added to CI.

## Related ADRs

- ADR-0011 PostgreSQL Database
- ADR-0016 Flowable Workflow Engine
- ADR-0019 Orchestration over Event-Driven
