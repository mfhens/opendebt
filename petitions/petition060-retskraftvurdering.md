---
id: petition060
title: "Formalize retskraft evaluation ordering under section 50"
delivery_track: governed
status: draft
created: 2026-05-26
author: "GitHub Copilot"
---

## Context

OpenDebt already formalizes payment application order (petition057) and automated set-off and correction-pool flows (petition058), but it does not yet define the legally required order in which doubtful claims are retskraft-evaluated under gaeldsinddrivelsesbekendtgoerelsens section 50. The PSRM guidance in G.A.2.5 makes that ordering explicit and separate from both daekningsraekkefolge in GIL section 4 and modregningsraekkefolge in GIL section 7.

The topic is compliance-sensitive because section 50 governs which claims may be evaluated first when voluntary payments or modregning produce surplus amounts and some claims still have doubtful retskraft or suspected data error. Without an explicit section-50 model, OpenDebt cannot provide a defensible worklist or caseworker-visible explanation for why one claim was retskraft-evaluated before another.

## Problem Statement

The system lacks a formal, auditable rule set for deciding which claims are retskraft-evaluated first in normal, data-error, voluntary-payment, and modregning situations. Without that rule set, caseworkers and downstream services cannot rely on a consistent ordering model for section-50 decisions.

## Functional Requirements

- FR-01: The system must rank claims with no suspected data error for retskraft evaluation in this default order: fines first, private maintenance claims second, and all other claims last.
- FR-02: The system must allow the default order to be overridden only when special circumstances justify it, and it must record the justification together with the legal basis for the override.
- FR-03: When a claim is marked with suspected data error that may affect collection, set-off, or payment coverage, the system must produce a discretionary ordering based on operational factors such as claim amount, error type, investigation complexity, and payment opportunity, and it must record the factors used.
- FR-04: The system must only retskraft-evaluate interest, fees, and similar accessory amounts after the related principal claim has been established as retskraftig; if such accessory amounts are written off because evaluation would be disproportionate, they must be excluded from evaluation ordering.
- FR-05: When a voluntary payment has covered already confirmed retskraftige claims and leaves a surplus, the system must select additional doubtful claims for retskraft evaluation only up to the surplus amount window, using the payment-application order from GIL section 4, subsection 2-3 for the principal claims and placing interest and similar accessory amounts last.
- FR-06: When the normal voluntary-payment ordering would delay use of a surplus amount, the system must support an expedited deviation that prioritizes the claims that can fastest be applied to payment, and it must record why the deviation was used.
- FR-07: When modregning is being considered for an overskydende beloeb, the system must first use claims already established as retskraftige and free of suspected data error, then select additional doubtful claims for retskraft evaluation only up to the remaining amount window, with interest and similar accessory amounts evaluated last.
- FR-08: In the modregning path, the system must support a decision to fully or partially abstain from modregning when operational timing, payout deadlines, claim amounts, error characteristics, or investigation complexity make that appropriate, and it must record the reason.
- FR-09: The system must present the resulting retskraft evaluation worklist and its basis to caseworkers and internal consumers, including the applied ordering mode, claim category, amount window, suspected data-error flag, and any deviation reason.
- FR-10: The system must keep a traceable log for each ordering decision showing when the ranking was produced, which rule path was used, and which claim or accessory amount was selected next for evaluation.

## Non-Functional Requirements

- NFR-01: Given identical input state and no manual override, the system must produce the same retskraft ordering every time.
- NFR-02: Every override or discretionary ordering decision must be auditable with legal reference, timestamp, and actor or system origin.
- NFR-03: Debtors and creditors must continue to be referenced by technical identifiers only; no PII may be introduced outside the Person Registry.

## Constraints and Assumptions

- The petition governs ordering of retskraft evaluations, not the substantive legal test for whether a claim is retskraftig.
- The ordering must follow gaeldsinddrivelsesbekendtgoerelsens section 50 and must not reuse GIL section 4 or section 7 ordering outside the specific surplus-payment and modregning cases allowed by section 50, subsection 4-5.
- Petition057 remains the authoritative source for payment-application ordering used by FR-05.
- Petition058 remains the authoritative source for modregning execution used by FR-07 and FR-08.
- The system can distinguish claims already confirmed as retskraftige from claims with doubtful retskraft and from claims marked with suspected data error.
- Legal scope follows the current section-50 and G.A.2.5 rules for claims transferred for collection through 2024-12-31.

## Out of Scope

- Substantive per-claim legal analysis rules for deciding whether a claim is retskraftig.
- Creation of new debt, interest, or claim-type rules unrelated to retskraft evaluation ordering.
- New creditor-facing portal functionality.
- Automatic correction of underlying data errors.
