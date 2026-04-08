@petition053
Feature: Fordringshaverportal — Opskrivning og nedskrivning — portal delta FRs (P053)

  # Covers: FR-1 (WriteDownReasonCode dropdown), FR-4 (retroactive advisory),
  #          FR-5 (backdated type description), FR-6 (cross-system suspension advisory),
  #          FR-7 (RIM-internal codes removed from portal).
  # Legal basis: G.A.1.4.3, G.A.1.4.4, G.A.2.3.4.4, Gæld.bekendtg. § 7 stk. 1–2, GIL § 18 k.
  # Baseline FRs (FR-2, FR-3, FR-8) are covered by separate regression scenarios.
  # Backend enforcement (FR-9) and audit log (AC-16) are in opendebt-debt-service petition053.

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
