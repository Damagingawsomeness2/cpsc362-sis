package edu.csuf.sis.records;

import java.math.BigDecimal;
import java.util.List;

/** Courses taken and grades earned, grouped by term (oldest first), with the cumulative GPA. */
public record Transcript(StudentProfile student, List<TranscriptTerm> terms,
                         int unitsAttempted, int unitsEarned, BigDecimal gpa) {
}
