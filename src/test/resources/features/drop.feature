Feature: Drop a class
  Students may drop a class only before the second week of classes.
  Professors may drop a student from a section they teach at any time.

  Background:
    Given today is 2026-11-02
    And the term "Spring 2027" runs from 2027-01-25 to 2027-05-21
    And the course "CPSC 131" "Data Structures" is in the catalog
    And the course "CPSC 362" "Foundations of Software Engineering" requires "CPSC 131"
    And professor "Alicia Kim" teaches section "01" of "CPSC 362" in "Spring 2027" with 40 seats on "MW" from 10:00 to 11:15
    And student "Maria Lopez" with CWID "885104273" has passed "CPSC 131" with grade "A-"
    And "Maria Lopez" is registered for section "01" of "CPSC 362" in "Spring 2027"

  Scenario: Dropping during the first week frees the seat
    Given today is 2027-01-29
    When "Maria Lopez" drops section "01" of "CPSC 362" in "Spring 2027"
    Then the request succeeds
    And section "01" of "CPSC 362" in "Spring 2027" has 40 open seats
    And the schedule of "Maria Lopez" does not list "CPSC 362"

  Scenario: Dropping in the second week is refused
    Given today is 2027-02-01
    When "Maria Lopez" drops section "01" of "CPSC 362" in "Spring 2027"
    Then the request is refused with a message mentioning "Jan 31, 2027"
    And the schedule of "Maria Lopez" lists "CPSC 362"

  Scenario: A dropped class can be added back while add/drop is open
    Given "Maria Lopez" drops section "01" of "CPSC 362" in "Spring 2027"
    When "Maria Lopez" registers for section "01" of "CPSC 362" in "Spring 2027"
    Then the request succeeds
    And section "01" of "CPSC 362" in "Spring 2027" has 39 open seats

  Scenario: A professor can drop a student from their own section after the deadline
    Given today is 2027-03-15
    When professor "Alicia Kim" drops "Maria Lopez" from section "01" of "CPSC 362" in "Spring 2027"
    Then the request succeeds
    And section "01" of "CPSC 362" in "Spring 2027" has 40 open seats
    And the schedule of "Maria Lopez" does not list "CPSC 362"

  Scenario: A professor cannot drop students from another professor's section
    Given professor "Ben Carter" teaches section "01" of "CPSC 131" in "Spring 2027" with 30 seats on "TuTh" from 08:30 to 09:45
    When professor "Ben Carter" drops "Maria Lopez" from section "01" of "CPSC 362" in "Spring 2027"
    Then the request is refused because the section belongs to another professor
    And the schedule of "Maria Lopez" lists "CPSC 362"
