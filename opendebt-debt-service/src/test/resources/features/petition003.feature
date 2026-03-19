Feature: Formalization of fordring, restance, and transfer to collection

  Scenario: An unpaid fordring becomes a restance after the betalingsfrist
    Given a fordring "F1" has a betalingsfrist on 2026-03-01
    And fordring "F1" is not fully paid by 2026-03-01
    When OpenDebt evaluates the claim state on 2026-03-02
    Then fordring "F1" is classified as a restance

  Scenario: A timely paid fordring does not become a restance
    Given a fordring "F2" has a betalingsfrist on 2026-03-01
    And fordring "F2" is fully paid by 2026-03-01
    When OpenDebt evaluates the claim state on 2026-03-02
    Then fordring "F2" is not classified as a restance

  Scenario: Only a restance can be transferred to collection
    Given a fordring "F3" is not classified as a restance
    When fordringshaver "K1" attempts overdragelse til inddrivelse for fordring "F3"
    Then the transfer is rejected
    And no transfer record is created

  Scenario: A transferred restance records the required audit trail
    Given restance "R1" belongs to fordringshaver "K1"
    And restance "R1" is eligible for transfer to collection
    When fordringshaver "K1" transfers restance "R1" to restanceinddrivelsesmyndighed "Gaeldsstyrelsen"
    Then a transfer record is created for restance "R1"
    And the transfer record identifies fordringshaver "K1"
    And the transfer record identifies restanceinddrivelsesmyndighed "Gaeldsstyrelsen"
    And the transfer record contains the transfer timestamp
    And restance "R1" becomes eligible for further collection handling
