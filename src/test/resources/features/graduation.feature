Feature: Graduation check
  A student may graduate after passing every course their major requires
  and completing the major's total units.

  Background:
    Given the courses "CPSC 131, CPSC 362" are in the catalog
    And the major "BSCS" "Computer Science" requires 120 units including "CPSC 131, CPSC 362"

  Scenario: A student who finished everything is eligible
    Given student "Ethan Brooks" with CWID "885500004" majors in "BSCS"
    And "Ethan Brooks" has passed "CPSC 131, CPSC 362" and 120 units in all
    When the registrar runs a graduation check for CWID "885500004"
    Then the student is eligible to graduate

  Scenario: Missing required courses are listed
    Given student "Maria Lopez" with CWID "885104273" majors in "BSCS"
    And "Maria Lopez" has passed "CPSC 131" and 64 units in all
    When the registrar runs a graduation check for CWID "885104273"
    Then the student is not eligible to graduate
    And the missing courses are "CPSC 362"

  Scenario: Too few units blocks graduation
    Given student "Ethan Brooks" with CWID "885500004" majors in "BSCS"
    And "Ethan Brooks" has passed "CPSC 131, CPSC 362" and 117 units in all
    When the registrar runs a graduation check for CWID "885500004"
    Then the student is not eligible to graduate
    And 3 units remain

  Scenario: Changing a major's requirements changes the outcome
    Given the course "CPSC 491" "Senior Capstone Project in Computer Science" is in the catalog
    And student "Ethan Brooks" with CWID "885500004" majors in "BSCS"
    And "Ethan Brooks" has passed "CPSC 131, CPSC 362" and 120 units in all
    When the registrar changes major "BSCS" to require "CPSC 131, CPSC 362, CPSC 491"
    And the registrar runs a graduation check for CWID "885500004"
    Then the student is not eligible to graduate
    And the missing courses are "CPSC 491"

  Scenario: A student without a declared major cannot be checked
    Given student "Sam Ortiz" with CWID "885200001" has not taken any classes
    When the registrar tries a graduation check for CWID "885200001"
    Then the request is refused with a message mentioning "major"
