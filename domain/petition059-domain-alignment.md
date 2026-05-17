# Domain Alignment Report — Petition 059

**Concept model version:** 1.0  
**Approved:** Not yet approved

## Concepts referenced by this petition

| Concept | Term | English | Status | Notes |
|---|---|---|---|---|
| fordring | Fordring | Claim | partial | Modelled as `DebtEntity`; petition059 depends on richer claim semantics around limitation state. |
| foraeldelse | Forældelse | Limitation | missing | Canonical concept exists, but implementation mapping is still empty in `debt-service`. |
| fordringskompleks | Fordringskompleks | Claim Complex | missing | Petition059 requires explicit membership + propagation behavior; model says this structure is not implemented. |
| indsigelse | Indsigelse | Objection | missing | Petition059 requires a dedicated objection workflow; model maps the concept as entirely missing. |
| modregning | Modregning | Set-off | partial | Present as bookkeeping / strategy behavior, not as a first-class domain object. |
| loenindeholdelse | Lønindeholdelse | Wage Garnishment | partial | Present as strategy / letter behavior, not as a full object with lifecycle and rules. |
| udlaeg | Udlæg | Attachment | missing | Petition059 depends on udlæg as a limitation-interrupting event; model has no service/entity/API yet. |
| underretning | Underretning | Notification | missing | Petition059 relies on underretning timing for lønindeholdelse interruption, but the shared notification concept is still missing. |

## Potential new concepts (not in model)

| Term found in petition | Suggested area | Action needed |
|---|---|---|
| Forældelsesfrist | Krav og status | Add as a distinct concept or explicitly model it as part of `foraeldelse`; petition059 treats it as a first-class business object. |
| Afbrydelse | Krav og status | Add as an event concept; petition059 uses it as the core legal and audit unit. |
| Tillægsfrist | Krav og status | Add as a distinct extension concept tied to internal opskrivning. |
| Udskydelse | Krav og status | Add as a separate statutory postponement concept; petition059 explicitly distinguishes it from afbrydelse. |
| Berostillelse | Myndighedsudøvelse | Add as a dedicated collection-measure subtype or event if the concept model is meant to drive implementation of interruption types. |
| Særligt retsgrundlag | Krav og status | Add as a dedicated classification concept or subtype; petition059 uses it as a legal switch for 10-year frist after udlæg. |

## Naming conflicts

| Petition uses | Concept model uses | Recommendation |
|---|---|---|
| retsgrundlag / særligt retsgrundlag | kravgrundlag | Do not force alignment by wording alone. The petition term behaves like a legal-basis classifier for limitation rules, while `kravgrundlag` in the model is broader. Clarify whether `retsgrundlag` should become a subtype, attribute, or separate concept. |

## Summary

- 8 concepts referenced, 0 exists, 3 partial, 5 missing
- 6 potential new concepts flagged
- 1 naming conflict found

## Recommendation

petition059 is domain-aligned enough to continue the pipeline, but the architecture phase should treat the following as mandatory inputs:

1. `foraeldelse`, `fordringskompleks`, and `indsigelse` are conceptually canonical but implementation-missing.
2. `afbrydelse`, `udskydelse`, `tillægsfrist`, and `særligt retsgrundlag` should be treated as likely concept-model extensions rather than ad hoc implementation terms.
3. The `retsgrundlag` vs `kravgrundlag` distinction needs an explicit architectural decision before code-level naming is frozen.
