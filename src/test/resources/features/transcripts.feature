Feature: Academic history and transcripts
  The registrar produces transcripts: courses taken and grades earned, by term, with GPA.
  Students and professors see the same history.

  Scenario: The transcript lists terms in order with the cumulative GPA
    Given student "Maria Lopez" with CWID "885104273"
    And "Maria Lopez" took "CPSC 121" in "Spring 2026" and earned "B"
    And "Maria Lopez" took "CPSC 120" in "Fall 2025" and earned "A"
    And "Maria Lopez" took "MATH 150A" in "Spring 2026" and earned "F"
    When the registrar generates a transcript for CWID "885104273"
    Then the transcript lists the terms "Fall 2025, Spring 2026" in order
    And the cumulative GPA is "2.33"
    And the transcript shows 6 units earned

  Scenario: Classes still in progress are not on the transcript
    Given today is 2026-08-01
    And the term "Fall 2026" runs from 2026-08-24 to 2026-12-18
    And the course "CPSC 362" "Foundations of Software Engineering" is in the catalog
    And professor "Alicia Kim" teaches section "01" of "CPSC 362" in "Fall 2026" with 40 seats on "MW" from 10:00 to 11:15
    And student "Maria Lopez" with CWID "885104273" has passed "CPSC 131" with grade "A-"
    And "Maria Lopez" is registered for section "01" of "CPSC 362" in "Fall 2026"
    When the registrar generates a transcript for CWID "885104273"
    Then the transcript lists the terms "Fall 2025" in order
