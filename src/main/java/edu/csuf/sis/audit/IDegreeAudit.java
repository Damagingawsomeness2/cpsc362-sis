package edu.csuf.sis.audit;

import java.util.List;

/** Degree Audit component: major requirements and graduation checks. Reads grades through IRecords. */
public interface IDegreeAudit {

    List<MajorView> majors();

    /** Admin: create a major, or replace the course list of an existing one. */
    MajorView defineMajor(String code, MajorRequest request);

    GraduationCheck checkGraduation(String cwid);
}
