package edu.csuf.sis.records;

import java.util.List;
import java.util.Set;

/**
 * Academic Records component: enrollments, grades, class rolls, history/transcripts and grade levels.
 * Used by the API gateway, Registration and Degree Audit.
 */
public interface IRecords {

    StudentProfile student(long studentId);

    StudentProfile studentByCwid(String cwid);

    /** Classes the student is taking now or is registered for in an upcoming term. */
    List<EnrollmentView> schedule(long studentId);

    /** Completed classes and grades by term, with GPA. Also printed as the official transcript. */
    Transcript history(long studentId);

    /** Codes of courses the student passed; used for prerequisite and graduation checks. */
    Set<String> passedCourseCodes(long studentId);

    /** Everyone enrolled in (or graded for) a section the professor teaches. */
    List<RosterEntry> classRoll(long professorId, long sectionId);

    /** Posts final grades for a section the professor teaches, then updates each student's grade level. */
    List<RosterEntry> enterGrades(long professorId, long sectionId, List<GradeEntry> grades);

    /** Recounts the units a student has passed and updates their grade level (DFD process 5.0). */
    StudentProfile updateGradeLevel(long studentId);

    /** Called by Registration after a seat has been reserved. */
    EnrollmentView recordEnrollment(long studentId, long sectionId);

    /** Called by Registration once a drop is allowed. */
    void recordDrop(long studentId, long sectionId);
}
