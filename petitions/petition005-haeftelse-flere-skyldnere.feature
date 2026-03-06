Feature: Explicit hæftelse and support for multiple skyldnere

  Scenario: A fordring with one liable party is registered as enehæftelse
    Given fordring "F1" is linked to skyldner "S1"
    When OpenDebt records the liability structure for fordring "F1"
    Then a hæftelse record is created between fordring "F1" and skyldner "S1"
    And the hæftelse type is "enehæftelse"

  Scenario: A fordring can have multiple solidarisk hæftende skyldnere
    Given fordring "F2" is linked to skyldner "S1" and skyldner "S2"
    And both skyldnere are jointly liable for the same fordring
    When OpenDebt records the liability structure for fordring "F2"
    Then a hæftelse record exists for fordring "F2" and skyldner "S1"
    And a hæftelse record exists for fordring "F2" and skyldner "S2"
    And the liability type for both relations is "solidarisk hæftelse"

  Scenario: A fordring can have delt hæftelse with defined shares
    Given fordring "F3" is linked to skyldner "S1" with share 60 percent
    And fordring "F3" is linked to skyldner "S2" with share 40 percent
    When OpenDebt records the liability structure for fordring "F3"
    Then the hæftelse for skyldner "S1" preserves the 60 percent share
    And the hæftelse for skyldner "S2" preserves the 40 percent share
    And the liability type for both relations is "delt hæftelse"

  Scenario: Communication can resolve all liable skyldnere from hæftelse
    Given fordring "F4" has registered hæftelser for skyldner "S1" and skyldner "S2"
    When OpenDebt prepares a formal underretning for fordring "F4"
    Then OpenDebt can identify skyldner "S1" as a liable recipient
    And OpenDebt can identify skyldner "S2" as a liable recipient
