@petition053
Feature: P053 backend enforcement independent of portal — FR-9 and audit log (AC-16)

  # Covers: FR-9 (ClaimAdjustmentService validation rules enforced at API boundary),
  #          AC-16 (all adjustment operations logged to CLS audit trail).
  # Legal basis: G.A.1.4.3, G.A.1.4.4, G.A.2.3.4.4, Gæld.bekendtg. § 7 stk. 1–2, GIL § 18 k.
  # Portal scenarios (FR-1, FR-4, FR-5, FR-6, FR-7) are in opendebt-creditor-portal petition053.
  # Endpoint: POST /api/v1/debts/{id}/adjustments (ClaimAdjustmentController — SPEC-P053 §9.2).

  # --- AC-16: Audit log — every adjustment operation is logged regardless of outcome ---

  Scenario: Vellykket justering logges til revisionssporet
    Given a claim "FDR-90091" is under inddrivelse
    When a valid nedskrivning is submitted via a direct API call for claim "FDR-90091" with reasonCode "NED_INDBETALING"
    Then debt-service returns HTTP status 201
    And an audit log entry is created in CLS for claim "FDR-90091" with outcome "SUCCESS"

  Scenario: Fejlende justering logges til revisionssporet
    Given a claim "FDR-90092" is under inddrivelse
    When a WriteDownDto is submitted via a direct API call for claim "FDR-90092" without a reasonCode
    Then debt-service returns HTTP status 422
    And an audit log entry is created in CLS for claim "FDR-90092" with outcome "FAILURE"

  # --- FR-9: Backend enforcement independent of portal ---

  Scenario: debt-service afviser nedskrivning uden årsagskode med HTTP 422
    Given a direct API call is made to the debt-service adjustment endpoint
    When a WriteDownDto is submitted for claim "FDR-90070" without a reasonCode
    Then debt-service returns HTTP status 422
    And the response body contains a problem-detail describing the validation failure

  Scenario: debt-service afviser nedskrivning med ukendt årsagskode med HTTP 422
    Given a direct API call is made to the debt-service adjustment endpoint
    When a WriteDownDto is submitted for claim "FDR-90071" with reasonCode "UNKNOWN_CODE"
    Then debt-service returns HTTP status 422
    And the response body contains a problem-detail describing the validation failure

  Scenario: debt-service afviser opskrivning på rentefordring med HTTP 422
    Given a direct API call is made to the debt-service adjustment endpoint
    And claim "FDR-90072" has claim category "RENTE"
    When a write-up of type "OPSKRIVNING_REGULERING" is submitted for claim "FDR-90072"
    Then debt-service returns HTTP status 422

  Scenario Outline: debt-service afviser opskrivning med RIM-intern kode <code> med HTTP 422
    Given a direct API call is made to the debt-service adjustment endpoint
    When a write-up is submitted for claim "FDR-90073" with reasonCode "<code>"
    Then debt-service returns HTTP status 422

    Examples:
      | code  |
      | DINDB |
      | OMPL  |
      | AFSK  |

  Scenario: debt-service accepterer gyldig nedskrivning med korrekt årsagskode
    Given a direct API call is made to the debt-service adjustment endpoint
    When a WriteDownDto is submitted for claim "FDR-90076" with reasonCode "NED_GRUNDLAG_AENDRET"
    Then debt-service returns HTTP status 201

  Scenario: debt-service anvender høring-tidspunktsregel uafhængigt af portal
    Given a direct API call is made to the debt-service adjustment endpoint
    And claim "FDR-90080" has lifecycleState "HOERING"
    When an opskrivning is submitted for claim "FDR-90080"
    Then debt-service records the opskrivningsfordring receipt timestamp as the høring resolution time
    And debt-service does not use the portal submission timestamp as the receipt time

  Scenario: debt-service logger retroaktiv nedskrivning separat til driftsovervågning
    Given a direct API call is made to the debt-service adjustment endpoint
    When a WriteDownDto is submitted for claim "FDR-90090" with:
      | field         | value           |
      | reasonCode    | NED_INDBETALING |
      | virkningsdato | 60 days ago     |
    Then debt-service returns HTTP status 201
    And a retroactive nedskrivning log marker is emitted for claim "FDR-90090"
