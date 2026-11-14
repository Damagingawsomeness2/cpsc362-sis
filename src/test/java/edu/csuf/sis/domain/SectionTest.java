package edu.csuf.sis.domain;

import edu.csuf.sis.common.RuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectionTest {

    private final Course cpsc362 = new Course("CPSC 362", "Foundations of Software Engineering", "", 3);
    private final Term fall2026 = new Term("Fall 2026", LocalDate.of(2026, 8, 24), LocalDate.of(2026, 12, 18));

    private Section sectionWithCapacity(int capacity) {
        return new Section(cpsc362, fall2026, "01", null, capacity, MeetingTime.TBA, "CS-104");
    }

    @Test
    void reservingASeatTakesOneOpening() {
        Section section = sectionWithCapacity(40);

        section.reserveSeat();

        assertThat(section.getEnrolled()).isEqualTo(1);
        assertThat(section.openSeats()).isEqualTo(39);
    }

    @Test
    void fullSectionTurnsAwayTheNextStudent() {
        Section section = sectionWithCapacity(1);
        section.reserveSeat();

        assertThatThrownBy(section::reserveSeat).isInstanceOf(RuleViolationException.class);
        assertThat(section.getEnrolled()).isEqualTo(1);
        assertThat(section.isFull()).isTrue();
    }

    @Test
    void releasingASeatReopensIt() {
        Section section = sectionWithCapacity(2);
        section.reserveSeat();
        section.reserveSeat();

        section.releaseSeat();

        assertThat(section.openSeats()).isEqualTo(1);
        assertThat(section.isFull()).isFalse();
    }

    @Test
    void releasingASeatNobodyHoldsIsABug() {
        Section section = sectionWithCapacity(5);

        assertThatIllegalStateException().isThrownBy(section::releaseSeat);
    }

    @Test
    void capacityMustBePositive() {
        assertThatIllegalArgumentException().isThrownBy(() -> sectionWithCapacity(0));
    }
}
