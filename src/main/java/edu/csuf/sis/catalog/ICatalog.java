package edu.csuf.sis.catalog;

import java.util.List;
import java.util.Set;

/**
 * Course Catalog &amp; Scheduling component: courses, prerequisites, terms, sections and open seats.
 * Used by the API gateway, Registration and the Chatbot Assistant.
 */
public interface ICatalog {

    List<CourseView> courses();

    CourseView course(String code);

    /** Admin: describe a new course, including its prerequisites. */
    CourseView addCourse(NewCourse course);

    /** Prerequisites of {@code courseCode} that are not among {@code passedCourseCodes}, in code order. */
    List<String> missingPrerequisites(String courseCode, Set<String> passedCourseCodes);

    /** All terms, oldest first. */
    List<TermView> terms();

    TermView term(long termId);

    TermView addTerm(NewTerm term);

    /** Sections offered in a term, optionally narrowed to courses whose code or title contains {@code search}. */
    List<SectionView> sections(long termId, String search);

    List<SectionView> sectionsTaughtBy(long professorId);

    SectionView section(long sectionId);

    /** Admin: add a section to a term's class schedule. */
    SectionView addSection(NewSection section);

    /** Takes one seat in the section, or fails if it is full. Called by Registration. */
    void reserveSeat(long sectionId);

    /** Gives a seat back after a drop. Called by Registration. */
    void releaseSeat(long sectionId);
}
