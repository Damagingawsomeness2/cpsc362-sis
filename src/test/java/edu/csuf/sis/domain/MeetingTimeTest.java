package edu.csuf.sis.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MeetingTimeTest {

    private static MeetingTime meeting(String days, String start, String end) {
        return MeetingTime.of(days, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void parsesOneAndTwoLetterDayCodes() {
        assertThat(meeting("TuTh", "10:00", "11:15").days()).containsExactlyInAnyOrder(TUESDAY, THURSDAY);
        assertThat(meeting("MWF", "09:00", "09:50").days()).containsExactlyInAnyOrder(MONDAY, WEDNESDAY, FRIDAY);
    }

    @Test
    void dayCodesAreCaseInsensitiveAndWrittenBackInCatalogForm() {
        assertThat(meeting("tuth", "10:00", "11:15").dayCodes()).isEqualTo("TuTh");
    }

    @Test
    void rejectsUnknownDayCodes() {
        assertThatIllegalArgumentException().isThrownBy(() -> meeting("MX", "10:00", "11:15"));
    }

    @Test
    void classMustEndAfterItStarts() {
        assertThatIllegalArgumentException().isThrownBy(() -> meeting("M", "11:00", "10:00"));
    }

    @Test
    void overlappingTimesOnASharedDayConflict() {
        assertThat(meeting("MW", "10:00", "11:15").overlaps(meeting("W", "11:00", "12:15"))).isTrue();
    }

    @Test
    void backToBackClassesDoNotConflict() {
        assertThat(meeting("MW", "10:00", "11:15").overlaps(meeting("MW", "11:15", "12:30"))).isFalse();
    }

    @Test
    void sameHoursOnDifferentDaysDoNotConflict() {
        assertThat(meeting("MW", "10:00", "11:15").overlaps(meeting("TuTh", "10:00", "11:15"))).isFalse();
    }

    @Test
    void sectionsWithoutAScheduledTimeNeverConflict() {
        MeetingTime lecture = meeting("MW", "10:00", "11:15");

        assertThat(MeetingTime.TBA.overlaps(lecture)).isFalse();
        assertThat(lecture.overlaps(MeetingTime.TBA)).isFalse();
    }

    @Test
    void blankDaysMeanToBeArranged() {
        assertThat(MeetingTime.of(" ", null, null).isTba()).isTrue();
    }

    @Test
    void describesDaysAndTwelveHourTimes() {
        assertThat(meeting("TuTh", "13:00", "14:15").describe()).isEqualTo("TuTh 1:00 PM–2:15 PM");
        assertThat(MeetingTime.TBA.describe()).isEqualTo("TBA");
    }
}
