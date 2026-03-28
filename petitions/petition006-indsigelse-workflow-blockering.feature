Feature: Indsigelse and workflow blocking during dispute handling

  Scenario: A skyldner registers an objection against a claim
    Given skyldner "S1" is linked to fordring "F1"
    When skyldner "S1" registers an indsigelse against fordring "F1" with reason "claim amount disputed"
    Then an indsigelse record is created for fordring "F1"
    And the indsigelse record identifies skyldner "S1"
    And the indsigelse record contains the stated reason
    And collection progression for fordring "F1" is not automatically blocked

  Scenario: An indsigelse does not automatically suspend inddrivelse
    Given fordring "F2" has an active indsigelse
    When OpenDebt attempts to continue collection progression for fordring "F2"
    Then the system reports that fordring "F2" has an active indsigelse
    But collection progression is not automatically blocked by the indsigelse

  Scenario: A rejected objection allows collection to resume
    Given fordring "F3" has an active indsigelse
    When OpenDebt resolves the indsigelse for fordring "F3" as "rejected"
    Then the indsigelse is no longer active
    And collection progression for fordring "F3" may resume

  Scenario: An upheld objection prevents normal collection continuation
    Given fordring "F4" has an active indsigelse
    When OpenDebt resolves the indsigelse for fordring "F4" as "upheld"
    Then the indsigelse is no longer active
    And fordring "F4" does not resume normal collection progression unchanged
    And the objection history remains available for audit
