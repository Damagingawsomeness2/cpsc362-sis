package edu.csuf.sis.registration;

import edu.csuf.sis.records.EnrollmentView;

/** Registration component: enroll and drop, enforcing prerequisites, seats, conflicts and deadlines. */
public interface IRegistration {

    EnrollmentView enroll(long studentId, long sectionId);

    /** A student dropping their own class; only allowed through the first week of the term. */
    void drop(long studentId, long sectionId);

    /** A professor removing a student from a section they teach; allowed at any time. */
    void dropByProfessor(long professorId, long sectionId, long studentId);
}
