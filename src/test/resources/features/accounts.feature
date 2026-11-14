Feature: Accounts and access
  The registrar creates logins with initial passwords and can revoke access.
  Revoking access never deletes a student's academic history.

  Scenario: A new student signs in with the password the registrar assigned
    When the registrar adds student "Nina Shah" with CWID "885600005", username "nshah" and password "Titans-2027"
    Then the request succeeds
    And "nshah" can sign in with password "Titans-2027"
    And "nshah" cannot sign in with password "wrong-password"

  Scenario: Usernames must be unique
    Given the registrar adds student "Nina Shah" with CWID "885600005", username "nshah" and password "Titans-2027"
    When the registrar adds student "Nick Shah" with CWID "885600006", username "nshah" and password "Titans-2027"
    Then the request is refused with a message mentioning "nshah"

  Scenario: Revoking access keeps the student's history
    Given student "Maria Lopez" with CWID "885104273" has passed "CPSC 131" with grade "A-"
    And "Maria Lopez" is signed in
    When the registrar revokes access for "Maria Lopez"
    Then "Maria Lopez" can no longer sign in
    And the earlier session of "Maria Lopez" no longer works
    And the transcript of "Maria Lopez" still shows "CPSC 131" with grade "A-"

  Scenario: Restoring access lets the student sign in again
    Given student "Maria Lopez" with CWID "885104273" has not taken any classes
    And the registrar revokes access for "Maria Lopez"
    When the registrar restores access for "Maria Lopez"
    Then "Maria Lopez" can sign in again

  Scenario: The registrar cannot lock themselves out
    When the registrar tries to revoke their own access
    Then the request is refused with a message mentioning "your own"

  Scenario: Sessions expire after eight hours
    Given student "Maria Lopez" with CWID "885104273" has not taken any classes
    And "Maria Lopez" is signed in
    When 8 hours pass
    Then the earlier session of "Maria Lopez" no longer works
