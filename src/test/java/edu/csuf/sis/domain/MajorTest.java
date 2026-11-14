package edu.csuf.sis.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MajorTest {

    private final Major computerScience = new Major("BSCS", "Computer Science", "B.S.", 120, List.of(
            new Course("CPSC 362", "Foundations of Software Engineering", "", 3),
            new Course("CPSC 131", "Data Structures", "", 3)));

    @Test
    void eligibleOnceEveryRequiredCourseIsPassedAndUnitsAreMet() {
        DegreeProgress progress = computerScience.evaluate(Set.of("CPSC 131", "CPSC 362", "ENGL 101"), 120);

        assertThat(progress.eligible()).isTrue();
        assertThat(progress.completedCourses()).containsExactly("CPSC 131", "CPSC 362");
        assertThat(progress.missingCourses()).isEmpty();
    }

    @Test
    void listsRequiredCoursesStillMissing() {
        // Maria Lopez in the object diagram: CPSC 131 done, CPSC 362 in progress, 64 units.
        DegreeProgress progress = computerScience.evaluate(Set.of("CPSC 131"), 64);

        assertThat(progress.eligible()).isFalse();
        assertThat(progress.missingCourses()).containsExactly("CPSC 362");
        assertThat(progress.unitsRemaining()).isEqualTo(56);
    }

    @Test
    void notEligibleWithEveryCourseButTooFewUnits() {
        DegreeProgress progress = computerScience.evaluate(Set.of("CPSC 131", "CPSC 362"), 119);

        assertThat(progress.eligible()).isFalse();
        assertThat(progress.missingCourses()).isEmpty();
        assertThat(progress.unitsRemaining()).isEqualTo(1);
    }

    @Test
    void extraUnitsNeverCountAsNegativeRemaining() {
        assertThat(computerScience.evaluate(Set.of(), 130).unitsRemaining()).isZero();
    }
}
