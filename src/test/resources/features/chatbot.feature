Feature: Chatbot assistant
  The assistant answers course and policy questions from catalog data.
  It never looks at anyone's grades or other personal records.

  Background:
    Given today is 2026-11-02
    And the term "Spring 2027" runs from 2027-01-25 to 2027-05-21
    And the course "CPSC 131" "Data Structures" is in the catalog
    And the course "CPSC 362" "Foundations of Software Engineering" requires "CPSC 131"
    And professor "Alicia Kim" teaches section "01" of "CPSC 362" in "Spring 2027" with 40 seats on "MW" from 10:00 to 11:15

  Scenario: Asking about prerequisites
    When someone asks the assistant "What do I need before taking cpsc362?"
    Then the reply mentions "CPSC 131"

  Scenario: Asking about open seats
    When someone asks the assistant "Are there open seats in CPSC 362?"
    Then the reply mentions "Spring 2027"
    And the reply mentions "40 of 40"

  Scenario: A full section is reported as full
    Given professor "Alicia Kim" teaches section "02" of "CPSC 362" in "Spring 2027" with 1 seat on "TuTh" from 13:00 to 14:15
    And student "Jordan Lee" with CWID "885300002" has passed "CPSC 131" with grade "B"
    And "Jordan Lee" is registered for section "02" of "CPSC 362" in "Spring 2027"
    When someone asks the assistant "Is CPSC 362 full?"
    Then the reply mentions "section 02 (TuTh 1:00 PM–2:15 PM, Alicia Kim): full (1 seat)"

  Scenario: Asking about the drop deadline
    When someone asks the assistant "When is the last day to drop a class?"
    Then the reply mentions "Jan 31, 2027"

  Scenario: Asking about a course that is not in the catalog
    When someone asks the assistant "Tell me about CPSC 999"
    Then the reply mentions "couldn't find CPSC 999"

  Scenario: Questions it cannot answer get a suggestion
    When someone asks the assistant "What is for lunch?"
    Then the reply mentions "prerequisites"
