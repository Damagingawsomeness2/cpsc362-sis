package edu.csuf.sis.records;

import edu.csuf.sis.domain.GradeLevel;

public record StudentProfile(long id, String cwid, String name, String email, String majorCode, String majorName,
                             int unitsCompleted, GradeLevel gradeLevel, boolean accessEnabled) {
}
