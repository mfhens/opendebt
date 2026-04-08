Feature: Fordringshaverportal — Opskrivning og nedskrivning (P053)

  # Legal basis: G.A.1.4.3, G.A.1.4.4, G.A.2.3.4.4, Gæld.bekendtg. § 7 stk. 1-2, GIL § 18 k
  # Out of scope: TB-038 (timeline replay), TB-039 (rentegodtgørelse), lønindeholdelse wording.
  # FR-2 and FR-3 are baseline-implemented (compliance-fixes sprint); scenarios included for
  # completeness and regression coverage.
  # FR-8 is baseline-implemented (petition 034); not re-tested here.
  # AC-10 coverage: see petition034-fordringshaverportal-opskrivning-nedskrivning.feature
  # (grantedAdjustmentPermissions filter scenarios).
  # AC-14 (i18n bundle coverage) is verified by CI bundle-lint tests, not by Gherkin scenarios.
  # AC-16 (audit log) is covered by the two CLS scenarios below.

  # --- FR-1: Nedskrivning — kontrolleret årsagsvalg (Gæld.bekendtg. § 7, stk. 2) ---

  Scenario: Nedskrivningsformular viser præcis tre lovlige årsagskoder
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission from the creditor agreement
    And user "U2" has nedskrivning permission from the creditor agreement
    And a claim "FDR-90001" is under inddrivelse for fordringshaver "K1"
    When user "U2" navigates to the nedskrivning adjustment form for claim "FDR-90001"
    Then the form displays a reason dropdown with exactly three options:
      | code                  | label                                         |
      | NED_INDBETALING       | Direkte indbetaling til fordringshaver        |
      | NED_FEJL_OVERSENDELSE | Fejlagtig oversendelse til inddrivelse        |
      | NED_GRUNDLAG_AENDRET  | Opkrævningsgrundlaget har ændret sig          |
    And no free-text reason input field is present on the form

  Scenario: Nedskrivning med gyldig årsagskode sendes korrekt til debt-service
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and nedskrivning permissions from the creditor agreement
    And a claim "FDR-90001" is under inddrivelse
    When user "U2" submits a nedskrivning for claim "FDR-90001" with:
      | field          | value            |
      | reasonCode     | NED_INDBETALING  |
      | beloeb         | 500.00           |
      | virkningsdato  | today            |
    Then the BFF forwards a WriteDownDto to debt-service containing reasonCode "NED_INDBETALING"
    And debt-service accepts the request and returns a success receipt

  Scenario Outline: Nedskrivning med gyldig årsagskode accepteres for alle tre koder
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and nedskrivning permissions from the creditor agreement
    And a claim "FDR-90001" is under inddrivelse
    When user "U2" submits a nedskrivning for claim "FDR-90001" with reasonCode "<code>"
    Then debt-service accepts the request

    Examples:
      | code                  |
      | NED_FEJL_OVERSENDELSE |
      | NED_GRUNDLAG_AENDRET  |

  Scenario: Nedskrivning uden valgt årsagskode afvises af portalen inden BFF-kald
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and nedskrivning permissions from the creditor agreement
    And a claim "FDR-90001" is under inddrivelse
    When user "U2" submits the nedskrivning form for claim "FDR-90001" without selecting a reason code
    Then the form displays a validation error using message key "adjustment.validation.reason.required"
    And the BFF does not forward any request to debt-service

  Scenario: Nedskrivning med ugyldig årsagskode afvises af portalen
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and nedskrivning permissions from the creditor agreement
    And a claim "FDR-90002" is under inddrivelse
    When user "U2" submits the nedskrivning form for claim "FDR-90002" with an unrecognised reason code "UNKNOWN_CODE"
    Then the form displays a validation error
    And the BFF does not forward any request to debt-service

  # --- FR-2: Opskrivning — rentefordring-undtagelse (G.A.1.4.3, 3. pkt.) ---
  # NOTE: Baseline implemented in compliance-fixes sprint. Scenarios retained for regression.

  Scenario: Opskrivning på rentefordring afvises med vejledningsbesked
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and opskrivning permissions from the creditor agreement
    And a claim "FDR-90010" has claim category "RENTE"
    When user "U2" navigates to the opskrivning adjustment form for claim "FDR-90010"
    And user "U2" selects adjustment type "OPSKRIVNING_REGULERING"
    Then the form displays a rejection message using message key "adjustment.validation.type.rentefordring"
    And the message instructs the user to submit a ny rentefordring via the claim creation flow
    And the adjustment form is not submitted to the BFF

  Scenario: Opskrivning på ikke-rentefordring tillades
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and opskrivning permissions from the creditor agreement
    And a claim "FDR-90011" does not have claim category "RENTE"
    When user "U2" navigates to the opskrivning adjustment form for claim "FDR-90011"
    And user "U2" selects adjustment type "OPSKRIVNING_REGULERING"
    Then the adjustment form is displayed without a rejection message

  # --- FR-3: Opskrivning — høring-tidspunktsbanner (Gæld.bekendtg. § 7 stk. 1, 4. pkt.) ---
  # NOTE: Baseline implemented in compliance-fixes sprint. Scenarios retained for regression.

  Scenario: Høringsbanner vises på justeringsformular når fordring er i høring
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission from the creditor agreement
    And a claim "FDR-90020" has lifecycleState "HOERING"
    When user "U2" navigates to the adjustment form for claim "FDR-90020"
    Then the portal displays a persistent informational banner before the form fields
    And the banner uses message key "adjustment.info.hoering"
    And the banner element carries attribute role="status"
    And the form is still submittable

  Scenario: Høringsbanner vises ved både opskrivning og nedskrivning
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" permission from the creditor agreement
    And a claim "FDR-90021" has lifecycleState "HOERING"
    When user "U2" navigates to the write-up adjustment form for claim "FDR-90021"
    Then the portal displays the hoering informational banner
    When user "U2" navigates to the write-down adjustment form for claim "FDR-90021"
    Then the portal displays the hoering informational banner

  Scenario: Høringsbanner vises ikke når fordring ikke er i høring
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And a claim "FDR-90022" has lifecycleState "UNDER_INDDRIVELSE"
    When user "U2" navigates to the adjustment form for claim "FDR-90022"
    Then the portal does not display the hoering informational banner

  # --- FR-4: Retroaktiv nedskrivning — brugervejledning (G.A.1.4.4) ---

  Scenario: Retroaktiv advarsel vises når virkningsdato er i fortiden
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and nedskrivning permissions from the creditor agreement
    And a claim "FDR-90030" is under inddrivelse
    And user "U2" is on the nedskrivning adjustment form for claim "FDR-90030"
    When user "U2" enters a virkningsdato that is 30 days in the past
    Then the form displays an inline advisory below the virkningsdato field using message key "adjustment.info.retroaktiv.virkningsdato"
    And the advisory element carries attribute aria-live="polite"
    And the form submission is not blocked

  Scenario Outline: Retroaktiv advarsel vises ikke når virkningsdato ikke er i fortiden
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And a claim "FDR-90022" is under inddrivelse
    When user "U2" enters virkningsdato "<date_description>" on the nedskrivning form for claim "FDR-90022"
    Then no retroactive nedskrivning advisory is displayed

    Examples:
      | date_description |
      | today            |
      | a future date    |

  Scenario: Retroaktiv nedskrivning kan stadig indsendes trods advarsel
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and nedskrivning permissions from the creditor agreement
    And a claim "FDR-90031" is under inddrivelse
    When user "U2" submits a nedskrivning for claim "FDR-90031" with:
      | field         | value            |
      | reasonCode    | NED_INDBETALING  |
      | beloeb        | 250.00           |
      | virkningsdato | 60 days ago      |
    Then the portal forwards the submission to the BFF
    And debt-service accepts the request and returns a success receipt

  # --- FR-5: Annulleret nedskrivning — tilbagedateret opskrivning (Gæld.bekendtg. § 7 stk. 1, 5. pkt.) ---

  Scenario: Tilbagedateringsbeskrivelse vises for OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and opskrivning permissions from the creditor agreement
    And a claim "FDR-90040" is under inddrivelse
    When user "U2" navigates to the opskrivning adjustment form for claim "FDR-90040"
    And user "U2" selects adjustment type "OPSKRIVNING_OMGJORT_NEDSKRIVNING_REGULERING"
    Then the form displays a type description using message key "adjustment.type.description.omgjort_nedskrivning_regulering"

  Scenario: Tilbagedateringsbeskrivelse vises ikke for andre opskrivningstyper
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and opskrivning permissions from the creditor agreement
    And a claim "FDR-90041" is under inddrivelse
    When user "U2" navigates to the opskrivning adjustment form for claim "FDR-90041"
    And user "U2" selects adjustment type "OPSKRIVNING_REGULERING"
    Then the form does not display the backdating type description

  # --- FR-6: Krydssystem retroaktiv nedskrivning — suspensionsadvisory (GIL § 18 k) ---

  Scenario: Suspensionsadvisory vises på kvitteringssiden ved krydssystem retroaktiv nedskrivning
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and nedskrivning permissions from the creditor agreement
    And a claim "FDR-90050" has a PSRM registration date of "2023-01-15"
    When user "U2" submits a nedskrivning for claim "FDR-90050" with virkningsdato "2022-12-01"
    Then the submission is accepted by debt-service
    And the receipt page displays a cross-system suspension advisory using message key "adjustment.info.suspension.krydssystem"
    And the advisory references GIL § 18 k
    And the standard receipt confirmation is also displayed

  Scenario: Suspensionsadvisory vises ikke når virkningsdato er efter PSRM-registreringsdato
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and nedskrivning permissions from the creditor agreement
    And a claim "FDR-90051" has a PSRM registration date of "2023-01-15"
    When user "U2" submits a nedskrivning for claim "FDR-90051" with virkningsdato "2023-06-01"
    Then the submission is accepted by debt-service
    And the receipt page does not display the cross-system suspension advisory

  # --- FR-7: Interne opskrivningskoder fjernet fra portalen (G.A.2.3.4.4) ---

  Scenario: RIM-interne årsagskoder er ikke tilgængelige i opskrivningsformularen
    Given portal user "U2" is authenticated with role "CREDITOR_EDITOR"
    And user "U2" has "allow_portal_actions" and opskrivning permissions from the creditor agreement
    And a claim "FDR-90060" is under inddrivelse
    When user "U2" navigates to the opskrivning adjustment form for claim "FDR-90060"
    Then the form does not contain any selectable option with code "DINDB"
    And the form does not contain any selectable option with code "OMPL"
    And the form does not contain any selectable option with code "AFSK"

  # AC-16: Audit log (CLS) — every adjustment submission is logged regardless of outcome

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

  # --- FR-9: Backend-håndhævelse uafhængig af portalen ---

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
    And an adjustment record is created in debt-service

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