Feature: Enter grades
  Professors post final grades for sections they teach. Posting a grade
  updates the student's units completed and grade level.

  Background:
    Given today is 2026-08-01
    And the term "Fall 2026" runs from 2026-08-24 to 2026-12-18
    And the course "CPSC 362" "Foundations of Software Engineering" is in the catalog
    And professor "Alicia Kim" teaches section "01" of "CPSC 362" in "Fall 2026" with 40 seats on "MW" from 10:00 to 11:15

  Scenario Outline: The grade level follows units completed
    Given student "Chris Park" with CWID "885400003" has completed <before> units
    And "Chris Park" is registered for section "01" of "CPSC 362" in "Fall 2026"
    And today is 2026-12-18
    When professor "Alicia Kim" posts grade "<grade>" for "Chris Park" in section "01" of "CPSC 362" in "Fall 2026"
    Then the request succeeds
    And "Chris Park" has <after> units completed and is a <level>

    Examples:
      | before | grade | after | level     |
      | 27     | B     | 30    | SOPHOMORE |
      | 57     | A-    | 60    | JUNIOR    |
      | 87     | C     | 90    | SENIOR    |
      | 27     | F     | 27    | FRESHMAN  |

  Scenario: The class roll lists the students in a section
    Given student "Chris Park" with CWID "885400003" has completed 27 units
    And "Chris Park" is registered for section "01" of "CPSC 362" in "Fall 2026"
    When professor "Alicia Kim" views the roll for section "01" of "CPSC 362" in "Fall 2026"
    Then the roll lists "Chris Park"

  Scenario: Professors can only grade their own sections
    Given professor "Ben Carter" teaches section "02" of "CPSC 362" in "Fall 2026" with 40 seats on "TuTh" from 10:00 to 11:15
    And student "Chris Park" with CWID "885400003" has completed 27 units
    And "Chris Park" is registered for section "01" of "CPSC 362" in "Fall 2026"
    When professor "Ben Carter" posts grade "A" for "Chris Park" in section "01" of "CPSC 362" in "Fall 2026"
    Then the request is refused because the section belongs to another professor
    And "Chris Park" has 27 units completed and is a FRESHMAN

  Scenario: A grade must be a real letter grade
    Given student "Chris Park" with CWID "885400003" has completed 27 units
    And "Chris Park" is registered for section "01" of "CPSC 362" in "Fall 2026"
    When professor "Alicia Kim" posts grade "E" for "Chris Park" in section "01" of "CPSC 362" in "Fall 2026"
    Then the request is refused with a message mentioning "'E' is not a letter grade"
