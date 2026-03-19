Feature: Citizen Portal MitID/TastSelv OAuth2 Authentication

  As a citizen (skyldner)
  I want to log in with MitID via TastSelv
  So that I can view my debts securely

  Background:
    Given the citizen portal is running with OAuth2 authentication enabled

  Scenario: Public pages are accessible without login
    When I visit the landing page "/"
    Then I should see the page without being redirected to login
    And the page should contain a MitID login button

  Scenario: Accessibility statement is public
    When I visit the accessibility statement "/was"
    Then I should see the page without being redirected to login

  Scenario: Authenticated pages redirect to OAuth2 login
    When I visit the dashboard "/dashboard" without being logged in
    Then I should be redirected to the OAuth2 authorization endpoint

  Scenario: Successful MitID login resolves person_id
    Given a citizen authenticates via MitID with a valid CPR claim
    When the OAuth2 callback is processed
    Then the citizen portal calls person-registry to resolve person_id
    And the resolved person_id UUID is stored on the authentication principal
    And the CPR number is NOT stored in the session or logged

  Scenario: Missing CPR claim in token causes login failure
    Given a citizen authenticates via MitID but the token has no CPR claim
    When the OAuth2 callback is processed
    Then the citizen is redirected to the login error page "/error/login-failed"
    And an error is logged about the missing CPR claim

  Scenario: Person registry unavailable causes graceful failure
    Given a citizen authenticates via MitID with a valid CPR claim
    And the person-registry service is unavailable
    When the OAuth2 callback is processed
    Then the citizen is redirected to the login error page "/error/login-failed"
    And an error is logged about the person resolution failure

  Scenario: Citizen logs out and session is cleared
    Given a citizen is logged in with person_id resolved
    When the citizen clicks the logout button
    Then the session is invalidated
    And the citizen is redirected to the landing page "/"

  Scenario: CPR is never exposed in responses or logs
    Given a citizen has authenticated via MitID
    When the citizen navigates any page in the portal
    Then the HTTP response must not contain any CPR number
    And the application logs must not contain any CPR number

  Scenario: Dev mode uses local Keycloak as TastSelv mock
    Given the application is running with the "dev" profile
    Then the OAuth2 provider points to local Keycloak
    And the CPR claim name is configurable via "opendebt.citizen.auth.cpr-claim-name"

  Scenario: ROLE_CITIZEN authority is granted after login
    Given a citizen authenticates successfully via MitID
    When the OAuth2 callback is processed
    Then the authentication principal has the ROLE_CITIZEN authority
