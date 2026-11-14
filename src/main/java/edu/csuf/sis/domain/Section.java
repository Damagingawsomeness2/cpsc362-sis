package edu.csuf.sis.domain;

import edu.csuf.sis.common.RuleViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalTime;

/** One offering of a course in a term (data store D3). Keeps its own seat count, as in the DFD. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "term_id", "section_number"}))
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "section_number", nullable = false, length = 4)
    private String sectionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    private Professor professor;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int enrolled;

    @Column(length = 10)
    private String days;

    private LocalTime startTime;

    private LocalTime endTime;

    @Column(length = 20)
    private String room;

    protected Section() {
    }

    public Section(Course course, Term term, String sectionNumber, Professor professor,
                   int capacity, MeetingTime meetingTime, String room) {
        if (capacity < 1) {
            throw new IllegalArgumentException("A section needs room for at least one student.");
        }
        if (sectionNumber == null || sectionNumber.isBlank()) {
            throw new IllegalArgumentException("A section needs a section number such as 01.");
        }
        this.course = course;
        this.term = term;
        this.sectionNumber = sectionNumber.trim();
        this.professor = professor;
        this.capacity = capacity;
        this.days = meetingTime.isTba() ? null : meetingTime.dayCodes();
        this.startTime = meetingTime.start();
        this.endTime = meetingTime.end();
        this.room = room;
    }

    public void reserveSeat() {
        if (isFull()) {
            throw new RuleViolationException(
                    course.getCode() + " section " + sectionNumber + " has no open seats.");
        }
        enrolled++;
    }

    public void releaseSeat() {
        if (enrolled == 0) {
            throw new IllegalStateException(
                    course.getCode() + " section " + sectionNumber + " has no taken seats to release.");
        }
        enrolled--;
    }

    public int openSeats() {
        return Math.max(0, capacity - enrolled);
    }

    public boolean isFull() {
        return enrolled >= capacity;
    }

    public MeetingTime meetingTime() {
        return MeetingTime.of(days, startTime, endTime);
    }

    public boolean isTaughtBy(Long professorId) {
        return professor != null && professor.getId().equals(professorId);
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public Term getTerm() {
        return term;
    }

    public String getSectionNumber() {
        return sectionNumber;
    }

    public Professor getProfessor() {
        return professor;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getEnrolled() {
        return enrolled;
    }

    public String getRoom() {
        return room;
    }
}
