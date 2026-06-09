# Validation Contract — petition060

## VAL-P060-001: Default ordering without suspected data error

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Default ordering without suspected data error"  
**Description**: Generating a default section-50 worklist returns fines before private maintenance claims and those before other claims.  
**Pass criteria**:
- The generated worklist shows the three claims in the scenario order `C-06011`, `C-06012`, `C-06013`.
- The visible ordering mode indicates the default section-50 path.
**Fail criteria**: Any different ranking or any non-default ordering mode is observed.  
**Required evidence**: network

## VAL-P060-002: Special circumstances override records why default order was changed

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Special circumstances override records why default order was changed"  
**Description**: Applying an override shows the changed ordering together with the override reason and legal basis.  
**Pass criteria**:
- Claim `C-06022` can appear ahead of the default section-50 order for this worklist.
- The visible result includes override reason `Urgent court deadline on C-06022`.
- The visible result includes a legal basis for the override.
**Fail criteria**: Any missing override reason, missing legal basis, or unchanged default ordering is observed.  
**Required evidence**: network, screenshots, console_errors

## VAL-P060-003: Suspected data error uses discretionary ordering instead of default ranking

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Suspected data error uses discretionary ordering instead of default ranking"  
**Description**: A worklist generated for suspected data error exposes a discretionary path and the prioritisation factors used.  
**Pass criteria**:
- The visible result indicates a discretionary data-error ordering path.
- Each ranked item exposes the factors used for prioritisation.
- The result does not claim that the default section-50 order was applied.
**Fail criteria**: Any default-order claim or any missing prioritisation-factor evidence is observed.  
**Required evidence**: network

## VAL-P060-004: Accessory amounts stay behind a principal claim until the principal is retskraftig

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Accessory amounts stay behind a principal claim until the principal is retskraftig"  
**Description**: The visible worklist never ranks an accessory amount ahead of a non-retskraftig principal claim, and only includes it after the principal becomes retskraftig.  
**Pass criteria**:
- Accessory amount `A-06041` is not ranked ahead of principal claim `C-06041` while the principal is not retskraftig.
- After the principal becomes retskraftig, `A-06041` may appear only after `C-06041`.
**Fail criteria**: Any ranking that places `A-06041` ahead of `C-06041` before principal confirmation is observed.  
**Required evidence**: network

## VAL-P060-005: Disproportionate accessory evaluation removes the accessory amount from the worklist

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Disproportionate accessory evaluation removes the accessory amount from the worklist"  
**Description**: An accessory amount written off as disproportionate is not returned in the ranked worklist.  
**Pass criteria**:
- Accessory amount `A-06042` is absent from the returned worklist.
**Fail criteria**: Any visible ranking entry for `A-06042` is observed.  
**Required evidence**: network

## VAL-P060-006: Voluntary payment surplus limits which doubtful claims are selected for evaluation

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Voluntary payment surplus limits which doubtful claims are selected for evaluation"  
**Description**: A surplus-payment worklist limits selection to the remaining amount window, follows petition057 ordering for principal claims, and keeps accessory items last.  
**Pass criteria**:
- The visible remaining amount window is `400 DKK`.
- Selected principal items follow the applicable GIL section 4 ordering.
- Accessory item `A-06052` is ranked after the principal items.
- The selected doubtful amount does not exceed `400 DKK`.
**Fail criteria**: Any window overflow, accessory-first ranking, or non-petition057 principal ordering is observed.  
**Required evidence**: network

## VAL-P060-007: Expedited voluntary-payment deviation is logged when normal ordering would delay coverage

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Expedited voluntary-payment deviation is logged when normal ordering would delay coverage"  
**Description**: An expedited deviation makes the faster-to-apply path visible and records why the normal ordering was bypassed.  
**Pass criteria**:
- The visible worklist records that an expedited deviation was used.
- The result explains why quicker-to-apply claims were prioritised.
**Fail criteria**: Any expedited worklist with no visible deviation marker or no visible reason is observed.  
**Required evidence**: network, screenshots, console_errors

## VAL-P060-008: Modregning uses confirmed claims first and evaluates doubtful claims within the remaining amount window

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Modregning uses confirmed claims first and evaluates doubtful claims within the remaining amount window"  
**Description**: A modregning worklist uses confirmed retskraftige claims before doubtful items and limits the doubtful branch to the remaining amount window.  
**Pass criteria**:
- Confirmed retskraftige claims are visibly applied before doubtful items.
- The remaining amount window for doubtful items is `500 DKK`.
- Accessory item `A-06072` is ranked after the principal items.
**Fail criteria**: Any doubtful-first ranking, wrong remaining window, or accessory-first ordering is observed.  
**Required evidence**: network

## VAL-P060-009: Partial or no modregning can be chosen for operational reasons and must be visible

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Partial or no modregning can be chosen for operational reasons and must be visible"  
**Description**: Recording a no-modregning outcome makes the decision and its operational reason visible to the caseworker.  
**Pass criteria**:
- The caseworker-visible result shows that no modregning was chosen for the current payout.
- The visible reason references timing or complexity constraints.
**Fail criteria**: Any missing decision visibility or any missing reason is observed.  
**Required evidence**: screenshots, console_errors, network

## VAL-P060-010: Caseworkers can inspect the rule path and audit details for an ordering decision

**Source**: `petitions/petition060-retskraftvurdering.feature` — Scenario: "Caseworkers can inspect the rule path and audit details for an ordering decision"  
**Description**: Inspecting an existing worklist exposes the applied rule path and audit metadata without leaking PII.  
**Pass criteria**:
- The visible result shows the ordering mode used.
- The visible result shows the legal reference for the rule path.
- The visible result shows actor or system origin and timestamp.
- Only technical identifiers are visible.
**Fail criteria**: Any missing rule-path metadata or any visible PII is observed.  
**Required evidence**: screenshots, console_errors, network
