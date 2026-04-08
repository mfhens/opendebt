Feature: Creation of a new fordring by a fordringshaver

  Scenario: API creation with a valid OCES3 certificate creates a debt post
    Given fordringshaver "K1" authenticates to the API with a valid OCES3 certificate
    And fordringshaver "K1" submits a new fordring for debtor "P1"
    And the rules evaluate the fordring as inddrivelsesparat
    When OpenDebt processes the submission
    Then a new debt post is created for debtor "P1"
    And bookkeeping is updated for the new debt post

  Scenario: API creation without a valid OCES3 certificate is rejected
    Given a fordringshaver submits a new fordring to the API without a valid OCES3 certificate
    When OpenDebt receives the submission
    Then the submission is rejected
    And no debt post is created

  Scenario: Portal creation with MitID Erhverv succeeds for the linked fordringshaver
    Given user "U1" is logged into the fordringshaverportal with MitID Erhverv
    And user "U1" is linked to fordringshaver "K1"
    And user "U1" submits a new fordring for fordringshaver "K1"
    And the rules evaluate the fordring as inddrivelsesparat
    When OpenDebt processes the submission
    Then a new debt post is created for the submitted debtor
    And bookkeeping is updated for the new debt post

  Scenario: Portal creation for another fordringshaver is rejected
    Given user "U1" is logged into the fordringshaverportal with MitID Erhverv
    And user "U1" is linked to fordringshaver "K1"
    When user "U1" submits a new fordring for fordringshaver "K2"
    Then the submission is rejected
    And no debt post is created

  Scenario: Portal creation requires MitID Erhverv login
    Given a user is not logged into the fordringshaverportal with MitID Erhverv
    When the user attempts to create a new fordring in the portal
    Then the user is not allowed to create the fordring
    And no debt post is created

  Scenario Outline: A non-inddrivelsesparat fordring returns the reason and is not created
    Given <channel> submits a new fordring using valid authentication
    And the rules evaluate the fordring as not inddrivelsesparat with reason "<reason>"
    When OpenDebt processes the submission
    Then the submission is rejected
    And the fordringshaver receives an error message that includes "<reason>"
    And no debt post is created
    And bookkeeping is not updated for debt creation

    Examples:
      | channel                           | reason                           |
      | the API for fordringshaver "K1"   | missing mandatory claim data     |
      | the portal user for fordringshaver "K1" | debt is not ready for collection |

  Scenario: A successful fordring submission returns a kvittering with fordringId and slutstatus UDFOERT
    Given fordringshaver "K1" authenticates to the API with a valid OCES3 certificate
    And fordringshaver "K1" submits a new fordring for debtor "P1"
    And the rules evaluate the fordring as inddrivelsesparat
    When OpenDebt processes the submission
    Then OpenDebt returns a kvittering with a fordringId
    And the kvittering slutstatus is "UDFOERT"

  Scenario: A fordring with stamdata deviations from indgangsfilter enters the HOERING workflow
    Given fordringshaver "K1" authenticates to the API with a valid OCES3 certificate
    And fordringshaver "K1" submits a fordring with stamdata that deviates from indgangsfilter rules
    When OpenDebt processes the submission
    Then OpenDebt returns a kvittering with slutstatus "HOERING"
    And the fordring is not received for inddrivelse while in HOERING

  Scenario: The Beskrivelse field does not store personal data
    Given fordringshaver "K1" authenticates to the API with a valid OCES3 certificate
    And fordringshaver "K1" submits a fordring with a Beskrivelse containing personal data
    When OpenDebt processes the submission
    Then the stored fordring Beskrivelse does not contain the submitted personal data
