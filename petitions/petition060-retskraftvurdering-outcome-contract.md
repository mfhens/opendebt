---
petition_id: petition060
status: draft
---

## Acceptance Criteria

- AC-01: Given a debtor with fines, private maintenance claims, and other claims and no suspected data error, when the default retskraft worklist is generated, then fines are ranked before private maintenance claims and private maintenance claims before other claims.
- AC-02: Given a special-circumstances override, when the retskraft worklist is generated, then the overridden ranking is returned together with the override justification and the legal basis for deviating from the default order.
- AC-03: Given claims with suspected data error, when the discretionary retskraft worklist is generated, then the ranking is based on operational factors such as amount, error type, investigation complexity, and payment opportunity, and the factors used are visible in the result.
- AC-04: Given accessory amounts linked to a principal claim, when the principal claim is not yet established as retskraftig, then the accessory amounts are not ranked ahead of that principal claim; if the accessory amounts are written off because evaluation would be disproportionate, they are excluded from the worklist.
- AC-05: Given a voluntary payment leaves a surplus after already confirmed retskraftige claims have been covered, when the system selects additional doubtful claims for retskraft evaluation, then selection is limited to the remaining amount window, follows the applicable principal-claim ordering from GIL section 4, subsection 2-3, and places interest and similar accessory amounts last.
- AC-06: Given the normal voluntary-payment ordering would delay use of a surplus amount, when expedited deviation is used, then the system prioritizes quicker-to-apply claims and records the deviation reason.
- AC-07: Given modregning is being considered for an overskydende beloeb, when the system selects claims for section-50 evaluation, then already confirmed retskraftige claims without suspected data error are used first and additional doubtful claims are evaluated only within the remaining amount window, with interest and similar accessory amounts ranked last.
- AC-08: Given operational timing, payout deadlines, claim amounts, error characteristics, or investigation complexity make modregning impractical, when the system decides on partial or no modregning, then that decision and its reason are recorded and visible to the caseworker.
- AC-09: Given a caseworker or internal consumer opens the retskraft worklist, when the ranked result is returned, then it includes the applied ordering mode, claim category, amount window when relevant, suspected data-error indicator, and any deviation reason.
- AC-10: Given any ordering decision is inspected after the fact, when audit data is reviewed, then the result shows timestamp, rule path, legal reference, actor or system origin, selected next item, and no PII beyond technical identifiers.

## Definition of Done

- [ ] All acceptance criteria pass
- [ ] Petition057 and petition058 references remain consistent with the section-50 ordering rules in this petition
- [ ] The petition, outcome contract, and Gherkin scenarios describe the same bounded scope

## Success Conditions

Caseworkers and internal services can retrieve a section-50 retskraft worklist that clearly explains why each claim is next in line for evaluation in normal, data-error, voluntary-payment, and modregning contexts. The returned order is reproducible, deviations are explicitly justified, and accessory amounts never outrank a principal claim that has not yet been established as retskraftig.

## Failure Conditions

The delivery fails if the default section-50 order is replaced by GIL section 4 or section 7 ordering outside the legally allowed surplus cases, if overrides or discretionary orderings are not justified and logged, if accessory amounts are evaluated ahead of a non-retskraftig principal claim, or if caseworkers cannot see which rule path produced the ranking.
