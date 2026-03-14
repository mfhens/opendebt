Feature: Fordring Claimant Authorization Rules

  Background:
    Given a fordringhaveraftale "FA001" exists for fordringshaver "FH001"
    And the agreement "FA001" is active

  # System Reporter Validation (Rule 400)

  Scenario: System reporter authorized for fordringshaver passes validation
    Given system reporter "SYS001" is authorized for fordringshaver "FH001"
    And system reporter "SYS001" submits an OPRETFORDRING action for "FH001"
    When the authorization rules are evaluated
    Then the action passes system reporter validation

  Scenario: System reporter not authorized for fordringshaver is rejected
    Given system reporter "SYS002" is not authorized for fordringshaver "FH001"
    And system reporter "SYS002" submits an OPRETFORDRING action for "FH001"
    When the authorization rules are evaluated
    Then the action is rejected with error code 400
    And the error message contains "Systemleverandør der indberetter kan ikke indberette for den angivne fordringshaver"

  # INDR Permission (Rule 416)

  Scenario: Fordringshaver with INDR permission can submit INDR claims
    Given fordringshaver "FH001" has permission to submit INDR claims
    And fordringshaver "FH001" submits an OPRETFORDRING action with ArtType "INDR"
    When the authorization rules are evaluated
    Then the action passes INDR permission validation

  Scenario: Fordringshaver without INDR permission cannot submit INDR claims
    Given fordringshaver "FH001" does not have permission to submit INDR claims
    And fordringshaver "FH001" submits an OPRETFORDRING action with ArtType "INDR"
    When the authorization rules are evaluated
    Then the action is rejected with error code 416
    And the error message contains "fordringshaver må ikke indberette inddrivelsesfordringer"

  Scenario: Fordringshaver without INDR permission cannot resubmit INDR claims
    Given fordringshaver "FH001" does not have permission to submit INDR claims
    And fordringshaver "FH001" submits a GENINDSENDFORDRING action with ArtType "INDR"
    When the authorization rules are evaluated
    Then the action is rejected with error code 416

  # MODR Permission (Rule 419)

  Scenario: Fordringshaver with MODR permission can submit MODR claims
    Given fordringshaver "FH001" has permission to submit MODR claims
    And fordringshaver "FH001" submits an OPRETFORDRING action with ArtType "MODR"
    When the authorization rules are evaluated
    Then the action passes MODR permission validation

  Scenario: Fordringshaver without MODR permission cannot submit MODR claims
    Given fordringshaver "FH001" does not have permission to submit MODR claims
    And fordringshaver "FH001" submits an OPRETFORDRING action with ArtType "MODR"
    When the authorization rules are evaluated
    Then the action is rejected with error code 419
    And the error message contains "fordringshaver må ikke indberette modregningsfordringer"

  Scenario: Fordringshaver without MODR permission cannot resubmit MODR claims
    Given fordringshaver "FH001" does not have permission to submit MODR claims
    And fordringshaver "FH001" submits a GENINDSENDFORDRING action with ArtType "MODR"
    When the authorization rules are evaluated
    Then the action is rejected with error code 419

  # Nedskriv Permission (Rule 420)

  Scenario: Fordringshaver with nedskriv permission can perform write-downs
    Given fordringshaver "FH001" has permission to perform nedskriv
    And fordringshaver "FH001" submits a NEDSKRIV action
    When the authorization rules are evaluated
    Then the action passes nedskriv permission validation

  Scenario: Fordringshaver without nedskriv permission cannot perform write-downs
    Given fordringshaver "FH001" does not have permission to perform nedskriv
    And fordringshaver "FH001" submits a NEDSKRIV action
    When the authorization rules are evaluated
    Then the action is rejected with error code 420
    And the error message contains "fordringshaver må ikke indberette nedskrivninger"

  # Tilbagekald Permission (Rule 421)

  Scenario: Fordringshaver with tilbagekald permission can withdraw claims
    Given fordringshaver "FH001" has permission to perform tilbagekald
    And fordringshaver "FH001" submits a TILBAGEKALD action
    When the authorization rules are evaluated
    Then the action passes tilbagekald permission validation

  Scenario: Fordringshaver without tilbagekald permission cannot withdraw claims
    Given fordringshaver "FH001" does not have permission to perform tilbagekald
    And fordringshaver "FH001" submits a TILBAGEKALD action
    When the authorization rules are evaluated
    Then the action is rejected with error code 421
    And the error message contains "fordringshaver må ikke indberette tilbagekald"

  # Portal Permission (Rule 437)

  Scenario: Fordringshaver with portal agreement can submit via portal
    Given fordringshaver "FH001" has a portal submission agreement
    And fordringshaver "FH001" submits an action via the fordringshaverportal
    When the authorization rules are evaluated
    Then the action passes portal permission validation

  Scenario: Fordringshaver without portal agreement cannot submit via portal
    Given fordringshaver "FH001" does not have a portal submission agreement
    And fordringshaver "FH001" submits an action via the fordringshaverportal
    When the authorization rules are evaluated
    Then the action is rejected with error code 437
    And the error message contains "ikke oprettet en aftale om indberetning via portal"

  # Complex Correction Action Permissions (Rules 465, 466, 497, 501, 508)

  Scenario: Fordringshaver without OANI permission is rejected
    Given fordringshaver "FH001" does not have permission for OANI actions
    And fordringshaver "FH001" submits an OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING action
    When the authorization rules are evaluated
    Then the action is rejected with error code 465
    And the error message contains "må ikke indberette OpskrivningAnnulleretNedskrivningIndbetaling"

  Scenario: Fordringshaver without opskrivning regulering permission is rejected
    Given fordringshaver "FH001" does not have permission for opskrivning regulering
    And fordringshaver "FH001" submits an OPSKRIVNINGREGULERING action
    When the authorization rules are evaluated
    Then the action is rejected with error code 466
    And the error message contains "må ikke indberette Opskrivninger med årsag opskrivningRegulering"

  Scenario: Fordringshaver without OONR permission is rejected
    Given fordringshaver "FH001" does not have permission for OONR actions
    And fordringshaver "FH001" submits an OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING action
    When the authorization rules are evaluated
    Then the action is rejected with error code 497
    And the error message contains "må ikke indberette OpskrivningOmgjortNedskrivningRegulering"

  Scenario: Fordringshaver without NAOR permission is rejected
    Given fordringshaver "FH001" does not have permission for NAOR actions
    And fordringshaver "FH001" submits a NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING action
    When the authorization rules are evaluated
    Then the action is rejected with error code 501
    And the error message contains "må ikke indberette NedskrivningAnnulleretOpskrivningRegulering"

  Scenario: Fordringshaver without NAOI permission is rejected
    Given fordringshaver "FH001" does not have permission for NAOI actions
    And fordringshaver "FH001" submits a NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING action
    When the authorization rules are evaluated
    Then the action is rejected with error code 508
    And the error message contains "må ikke indberette NedskrivningAnnulleretOpskrivningIndbetaling"

  Scenario Outline: Fordringshaver with correct permission passes complex action validation
    Given fordringshaver "FH001" has permission for "<permission>"
    And fordringshaver "FH001" submits a "<action_type>" action
    When the authorization rules are evaluated
    Then the action passes authorization validation

    Examples:
      | permission            | action_type                                  |
      | OANI                  | OPSKRIVNINGANNULLERETNEDSKRIVNINGINDBETALING |
      | opskrivning_regulering| OPSKRIVNINGREGULERING                        |
      | OONR                  | OPSKRIVNINGOMGJORTNEDSKRIVNINGREGULERING     |
      | NAOR                  | NEDSKRIVNINGANNULLERETOPSKRIVNINGREGULERING  |
      | NAOI                  | NEDSKRIVNINGANNULLERETOPSKRIVNINGINDBETALING |

  # Hovedstol Permission (Rule 511)

  Scenario: Fordringshaver with hovedstol permission can modify principal
    Given fordringshaver "FH001" has permission to change hovedstol
    And fordringshaver "FH001" submits an AENDRFORDRING action modifying hovedstol
    When the authorization rules are evaluated
    Then the action passes hovedstol permission validation

  Scenario: Fordringshaver without hovedstol permission cannot modify principal
    Given fordringshaver "FH001" does not have permission to change hovedstol
    And fordringshaver "FH001" submits an AENDRFORDRING action modifying hovedstol
    When the authorization rules are evaluated
    Then the action is rejected with error code 511
    And the error message contains "fordringshaver må ikke indberette hovedstolændringer"

  # Genindsend Permission (Rule 543)

  Scenario: Fordringshaver with resubmit permission can resubmit claims
    Given fordringshaver "FH001" has permission to resubmit claims
    And fordringshaver "FH001" submits a GENINDSENDFORDRING action
    When the authorization rules are evaluated
    Then the action passes resubmit permission validation

  Scenario: Fordringshaver without resubmit permission cannot resubmit claims
    Given fordringshaver "FH001" does not have permission to resubmit claims
    And fordringshaver "FH001" submits a GENINDSENDFORDRING action
    When the authorization rules are evaluated
    Then the action is rejected with error code 543
    And the error message contains "fordringshaver må ikke indberette genindsend aktioner"

  # SSO Access Validation (Rule 480)

  Scenario: Portal user with valid SSO access passes validation
    Given portal user "User1" has valid SSO access for fordringshaver "FH001"
    And portal user "User1" submits an action via the portal
    When the authorization rules are evaluated
    Then the action passes SSO access validation

  Scenario: Portal user with invalid SSO access is rejected
    Given portal user "User2" has invalid SSO access
    And portal user "User2" submits an action via the portal
    When the authorization rules are evaluated
    Then the action is rejected with error code 480
    And the error message contains "Adgang nægtet. Ugyldig sagsbehandler- eller fordringshaver adgang"

  # Combined Authorization

  Scenario: Fully authorized OPRETFORDRING passes all authorization checks
    Given system reporter "SYS001" is authorized for fordringshaver "FH001"
    And fordringshaver "FH001" has permission to submit INDR claims
    And system reporter "SYS001" submits an OPRETFORDRING action for "FH001" with ArtType "INDR"
    When the authorization rules are evaluated
    Then the action passes all authorization rules
