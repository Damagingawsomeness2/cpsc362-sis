package edu.csuf.sis.domain;

import edu.csuf.sis.common.RuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrollmentTest {

    private final Enrollment enrollment = new Enrollment(
            new Student("885104273", "Maria Lopez", "mlopez@csu.fullerton.edu", null,
                    new UserAccount("mlopez", "hash", Role.STUDENT, "Maria Lopez")),
            new Section(new Course("CPSC 362", "Foundations of Software Engineering", "", 3),
                    new Term("Fall 2026", LocalDate.of(2026, 8, 24), LocalDate.of(2026, 12, 18)),
                    "01", null, 40, MeetingTime.TBA, "CS-104"),
            Instant.parse("2026-08-20T17:00:00Z"));

    @Test
    void gradingCompletesTheClass() {
        enrollment.assignGrade(LetterGrade.A_MINUS);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getGrade()).isEqualTo(LetterGrade.A_MINUS);
        assertThat(enrollment.isPassed()).isTrue();
    }

    @Test
    void anFCompletesTheClassWithoutPassingIt() {
        enrollment.assignGrade(LetterGrade.F);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.isPassed()).isFalse();
    }

    @Test
    void aClassInProgressIsNotPassedYet() {
        assertThat(enrollment.isPassed()).isFalse();
    }

    @Test
    void gradesCanBeCorrectedAfterTheyArePosted() {
        enrollment.assignGrade(LetterGrade.C);

        enrollment.assignGrade(LetterGrade.B);

        assertThat(enrollment.getGrade()).isEqualTo(LetterGrade.B);
    }

    @Test
    void droppedClassesCannotBeGraded() {
        enrollment.drop();

        assertThatThrownBy(() -> enrollment.assignGrade(LetterGrade.B)).isInstanceOf(RuleViolationException.class);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
    }

    @Test
    void completedClassesCannotBeDropped() {
        enrollment.assignGrade(LetterGrade.B);

        assertThatThrownBy(enrollment::drop).isInstanceOf(RuleViolationException.class);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void aDroppedClassCanBeRejoined() {
        enrollment.drop();

        enrollment.reenroll();

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
    }
}
