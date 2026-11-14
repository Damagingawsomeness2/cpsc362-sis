package edu.csuf.sis.audit;

import edu.csuf.sis.records.StudentProfile;

import java.time.LocalDate;
import java.util.List;

public record GraduationCheck(StudentProfile student, String majorCode, String majorName, String degree,
                              boolean eligible, List<String> completedCourses, List<String> missingCourses,
                              int unitsCompleted, int unitsRequired, int unitsRemaining, LocalDate checkedOn) {
}
