package edu.csuf.sis.domain;

public enum EnrollmentStatus {
    /** Taking the class now (or registered for an upcoming term). */
    ENROLLED,
    /** Dropped by the student or removed by the professor. */
    DROPPED,
    /** A final grade has been posted. */
    COMPLETED
}
