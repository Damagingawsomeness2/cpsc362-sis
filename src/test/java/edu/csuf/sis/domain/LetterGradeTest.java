package edu.csuf.sis.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LetterGradeTest {

    @ParameterizedTest
    @CsvSource({"A+, A_PLUS", "' a- ', A_MINUS", "b+, B_PLUS", "D-, D_MINUS", "f, F"})
    void parsesLabelsIgnoringCaseAndSurroundingSpaces(String label, LetterGrade expected) {
        assertThat(LetterGrade.fromLabel(label)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"E", "A++", "", "4.0"})
    void rejectsTextThatIsNotALetterGrade(String label) {
        assertThatIllegalArgumentException().isThrownBy(() -> LetterGrade.fromLabel(label));
    }

    @Test
    void rejectsMissingGrade() {
        assertThatIllegalArgumentException().isThrownBy(() -> LetterGrade.fromLabel(null));
    }

    @ParameterizedTest
    @EnumSource(LetterGrade.class)
    void everyLabelParsesBackToItsOwnGrade(LetterGrade grade) {
        assertThat(LetterGrade.fromLabel(grade.label())).isEqualTo(grade);
    }

    @Test
    void dMinusIsTheLowestPassingGrade() {
        assertThat(LetterGrade.D_MINUS.isPassing()).isTrue();
        assertThat(LetterGrade.F.isPassing()).isFalse();
    }
}
