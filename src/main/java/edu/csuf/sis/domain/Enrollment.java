package edu.csuf.sis.domain;

import edu.csuf.sis.common.RuleViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Locale;

/** A student's place in a section and, once posted, the grade earned (data store D4). */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "section_id"}))
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EnrollmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private LetterGrade grade;

    @Column(nullable = false)
    private Instant enrolledAt;

    protected Enrollment() {
    }

    public Enrollment(Student student, Section section, Instant enrolledAt) {
        this.student = student;
        this.section = section;
        this.enrolledAt = enrolledAt;
        this.status = EnrollmentStatus.ENROLLED;
    }

    public void drop() {
        if (status != EnrollmentStatus.ENROLLED) {
            throw new RuleViolationException("Only classes in progress can be dropped; this one is "
                    + status.name().toLowerCase(Locale.ROOT) + ".");
        }
        status = EnrollmentStatus.DROPPED;
    }

    public void reenroll() {
        if (status != EnrollmentStatus.DROPPED) {
            throw new IllegalStateException("Only a dropped class can be rejoined.");
        }
        status = EnrollmentStatus.ENROLLED;
    }

    /** Posts (or corrects) the final grade, which completes the class. */
    public void assignGrade(LetterGrade grade) {
        if (status == EnrollmentStatus.DROPPED) {
            throw new RuleViolationException(student.getName() + " dropped this class, so it cannot be graded.");
        }
        this.grade = grade;
        this.status = EnrollmentStatus.COMPLETED;
    }

    /** Completed with a passing grade: the units count and the course satisfies prerequisites. */
    public boolean isPassed() {
        return status == EnrollmentStatus.COMPLETED && grade.isPassing();
    }

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public Section getSection() {
        return section;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public LetterGrade getGrade() {
        return grade;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}
