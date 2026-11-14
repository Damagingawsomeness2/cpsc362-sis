Feature: Register for a class section
  Students choose an open section of a course. Registration checks prerequisites,
  open seats, time conflicts, and the add/drop deadline (the end of the first week of classes).

  Background:
    Given today is 2026-11-02
    And the term "Spring 2027" runs from 2027-01-25 to 2027-05-21
    And the course "CPSC 131" "Data Structures" is in the catalog
    And the course "CPSC 362" "Foundations of Software Engineering" requires "CPSC 131"
    And professor "Alicia Kim" teaches section "01" of "CPSC 362" in "Spring 2027" with 40 seats on "MW" from 10:00 to 11:15
    And student "Maria Lopez" with CWID "885104273" has passed "CPSC 131" with grade "A-"

  Scenario: A student who passed the prerequisite gets a seat
    When "Maria Lopez" registers for section "01" of "CPSC 362" in "Spring 2027"
    Then the request succeeds
    And section "01" of "CPSC 362" in "Spring 2027" has 39 open seats
    And the schedule of "Maria Lopez" lists "CPSC 362"

  Scenario: A missing prerequisite blocks registration
    Given student "Sam Ortiz" with CWID "885200001" has not taken any classes
    When "Sam Ortiz" registers for section "01" of "CPSC 362" in "Spring 2027"
    Then the request is refused with a message mentioning "CPSC 131"
    And section "01" of "CPSC 362" in "Spring 2027" has 40 open seats

  Scenario: A full section turns the next student away
    Given professor "Alicia Kim" teaches section "02" of "CPSC 362" in "Spring 2027" with 1 seat on "TuTh" from 13:00 to 14:15
    And student "Jordan Lee" with CWID "885300002" has passed "CPSC 131" with grade "B"
    And "Jordan Lee" is registered for section "02" of "CPSC 362" in "Spring 2027"
    When "Maria Lopez" registers for section "02" of "CPSC 362" in "Spring 2027"
    Then the request is refused with a message mentioning "no open seats"

  Scenario: A student takes only one section of a course per term
    Given professor "Alicia Kim" teaches section "02" of "CPSC 362" in "Spring 2027" with 40 seats on "TuTh" from 13:00 to 14:15
    And "Maria Lopez" is registered for section "01" of "CPSC 362" in "Spring 2027"
    When "Maria Lopez" registers for section "02" of "CPSC 362" in "Spring 2027"
    Then the request is refused with a message mentioning "already registered"

  Scenario: Classes that meet at the same time conflict
    Given the course "CPSC 332" "File Structures and Database Systems" requires "CPSC 131"
    And professor "Alicia Kim" teaches section "01" of "CPSC 332" in "Spring 2027" with 40 seats on "W" from 11:00 to 12:15
    And "Maria Lopez" is registered for section "01" of "CPSC 362" in "Spring 2027"
    When "Maria Lopez" registers for section "01" of "CPSC 332" in "Spring 2027"
    Then the request is refused with a message mentioning "CPSC 362"

  Scenario: A course already passed cannot be taken again
    Given professor "Alicia Kim" teaches section "01" of "CPSC 131" in "Spring 2027" with 40 seats on "F" from 09:00 to 11:45
    When "Maria Lopez" registers for section "01" of "CPSC 131" in "Spring 2027"
    Then the request is refused with a message mentioning "already passed"

  Scenario: Registration closes after the first week of classes
    Given today is 2027-02-01
    When "Maria Lopez" registers for section "01" of "CPSC 362" in "Spring 2027"
    Then the request is refused with a message mentioning "Jan 31, 2027"
