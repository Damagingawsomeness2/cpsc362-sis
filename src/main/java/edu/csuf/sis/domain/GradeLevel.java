package edu.csuf.sis.domain;

/** Class standing, derived from the units a student has successfully completed. */
public enum GradeLevel {
    FRESHMAN,
    SOPHOMORE,
    JUNIOR,
    SENIOR;

    public static GradeLevel forUnits(int unitsCompleted) {
        if (unitsCompleted < 0) {
            throw new IllegalArgumentException("Units completed cannot be negative: " + unitsCompleted);
        }
        if (unitsCompleted >= 90) {
            return SENIOR;
        }
        if (unitsCompleted >= 60) {
            return JUNIOR;
        }
        if (unitsCompleted >= 30) {
            return SOPHOMORE;
        }
        return FRESHMAN;
    }
}
