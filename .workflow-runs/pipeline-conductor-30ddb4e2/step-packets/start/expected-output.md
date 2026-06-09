# Expected Output for start

## Required Fields
- status: (string) One of: success
- summary: (string) Brief description of result
- artifacts: (list) Output files or artifacts
- evidence_checked: (list) One or more evidence entries with claim, proof, freshness, and result

## Example
```yaml
status: success
summary: "Claim identified and processed"
artifacts:
  - output.json
evidence_checked:
  - claim: "Workflow contract was read"
    proof: "Reviewed workflow-contract.md in the step packet"
    freshness: "current-run"
    result: pass
```
