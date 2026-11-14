package edu.csuf.sis.domain;

import java.util.List;

/** How far a student is toward a degree: required courses passed and missing, and units. */
public record DegreeProgress(List<String> completedCourses, List<String> missingCourses,
                             int unitsCompleted, int unitsRequired) {

    public DegreeProgress {
        completedCourses = List.copyOf(completedCourses);
        missingCourses = List.copyOf(missingCourses);
    }

    public boolean eligible() {
        return missingCourses.isEmpty() && unitsCompleted >= unitsRequired;
    }

    public int unitsRemaining() {
        return Math.max(0, unitsRequired - unitsCompleted);
    }
}
