package edu.csuf.sis.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class GradeLevelTest {

    @ParameterizedTest(name = "{0} units -> {1}")
    @CsvSource({
            "0, FRESHMAN",
            "29, FRESHMAN",
            "30, SOPHOMORE",
            "59, SOPHOMORE",
            "60, JUNIOR",
            "64, JUNIOR", // Maria Lopez in the object diagram
            "89, JUNIOR",
            "90, SENIOR",
            "132, SENIOR"
    })
    void gradeLevelDependsOnUnitsCompleted(int units, GradeLevel expected) {
        assertThat(GradeLevel.forUnits(units)).isEqualTo(expected);
    }

    @Test
    void negativeUnitsAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> GradeLevel.forUnits(-3));
    }
}
