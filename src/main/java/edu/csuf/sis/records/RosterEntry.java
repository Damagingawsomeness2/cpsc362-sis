package edu.csuf.sis.records;

import edu.csuf.sis.domain.EnrollmentStatus;
import edu.csuf.sis.domain.GradeLevel;

/** One line of a class roll. */
public record RosterEntry(long studentId, String cwid, String name, GradeLevel gradeLevel,
                          EnrollmentStatus status, String grade) {
}
