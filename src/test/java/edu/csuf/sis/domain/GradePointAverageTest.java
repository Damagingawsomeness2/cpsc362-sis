package edu.csuf.sis.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GradePointAverageTest {

    @Test
    void averageIsWeightedByUnits() {
        // (3.7 * 3 + 2.0 * 4) / 7 = 19.1 / 7 = 2.728...
        BigDecimal gpa = GradePointAverage.of(List.of(
                new GradedUnits(3, LetterGrade.A_MINUS),
                new GradedUnits(4, LetterGrade.C)));

        assertThat(gpa).isEqualTo(new BigDecimal("2.73"));
    }

    @Test
    void failingGradesCountAgainstTheAverage() {
        // (4.0 * 3 + 0.0 * 3) / 6
        BigDecimal gpa = GradePointAverage.of(List.of(
                new GradedUnits(3, LetterGrade.A),
                new GradedUnits(3, LetterGrade.F)));

        assertThat(gpa).isEqualTo(new BigDecimal("2.00"));
    }

    @Test
    void noGradedCoursesMeansAZeroAverage() {
        assertThat(GradePointAverage.of(List.of())).isEqualTo(new BigDecimal("0.00"));
    }
}
