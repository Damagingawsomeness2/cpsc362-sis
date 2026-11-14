package edu.csuf.sis.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CourseTest {

    private static Course course(String code) {
        return new Course(code, "Title of " + code, "", 3);
    }

    @ParameterizedTest
    @CsvSource({"cpsc362, CPSC 362", "' Math 150a ', MATH 150A", "'CPSC   131', CPSC 131", "phys 225L, PHYS 225L"})
    void courseCodesAreNormalizedToCatalogForm(String raw, String expected) {
        assertThat(Course.normalizeCode(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"362", "CPSC", "CPSC 36", "C 362", "CPSC 3620"})
    void malformedCourseCodesAreRejected(String raw) {
        assertThatIllegalArgumentException().isThrownBy(() -> Course.normalizeCode(raw));
    }

    @Test
    void newCoursesStoreTheNormalizedCode() {
        assertThat(course("cpsc 362").getCode()).isEqualTo("CPSC 362");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void unitsMustBeBetweenOneAndSix(int units) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Course("CPSC 999", "Bad units", "", units));
    }

    @Test
    void reportsOnlyThePrerequisitesTheStudentHasNotPassed() {
        Course algorithms = course("CPSC 335");
        algorithms.addPrerequisite(course("CPSC 131"));
        algorithms.addPrerequisite(course("MATH 170B"));

        assertThat(algorithms.missingPrerequisites(Set.of("CPSC 131", "ENGL 101"))).containsExactly("MATH 170B");
    }

    @Test
    void nothingIsMissingOnceEveryPrerequisiteIsPassed() {
        Course softwareEngineering = course("CPSC 362");
        softwareEngineering.addPrerequisite(course("CPSC 131"));

        assertThat(softwareEngineering.missingPrerequisites(Set.of("CPSC 131"))).isEmpty();
    }

    @Test
    void prerequisitesAreListedInCodeOrder() {
        Course capstone = course("CPSC 491");
        capstone.addPrerequisite(course("CPSC 490"));
        capstone.addPrerequisite(course("CPSC 362"));

        assertThat(capstone.prerequisiteCodes()).containsExactly("CPSC 362", "CPSC 490");
    }

    @Test
    void aCourseCannotBeItsOwnPrerequisite() {
        Course course = course("CPSC 362");

        assertThatIllegalArgumentException().isThrownBy(() -> course.addPrerequisite(course));
    }
}
