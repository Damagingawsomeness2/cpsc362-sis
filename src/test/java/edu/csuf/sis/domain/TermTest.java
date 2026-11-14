package edu.csuf.sis.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TermTest {

    // Classes start Monday, Aug 24, so the first week runs through Sunday, Aug 30.
    private final Term fall2026 = new Term("Fall 2026", LocalDate.of(2026, 8, 24), LocalDate.of(2026, 12, 18));

    @Test
    void addDropIsOpenBeforeClassesStart() {
        assertThat(fall2026.isAddDropOpen(LocalDate.of(2026, 4, 1))).isTrue();
    }

    @Test
    void addDropStaysOpenOnTheLastDayOfTheFirstWeek() {
        assertThat(fall2026.isAddDropOpen(LocalDate.of(2026, 8, 30))).isTrue();
    }

    @Test
    void addDropClosesWhenTheSecondWeekStarts() {
        assertThat(fall2026.isAddDropOpen(LocalDate.of(2026, 8, 31))).isFalse();
    }

    @Test
    void deadlineIsTheLastDayOfTheFirstWeek() {
        assertThat(fall2026.addDropDeadline()).isEqualTo(LocalDate.of(2026, 8, 30));
    }

    @Test
    void termMustEndAfterItStarts() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new Term("Backwards", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 1, 20)));
    }
}
