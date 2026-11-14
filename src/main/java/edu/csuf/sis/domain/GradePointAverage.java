package edu.csuf.sis.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Unit-weighted grade point average, rounded to two decimals. */
public final class GradePointAverage {

    private GradePointAverage() {
    }

    public static BigDecimal of(List<GradedUnits> gradedClasses) {
        int units = 0;
        BigDecimal gradePoints = BigDecimal.ZERO;
        for (GradedUnits graded : gradedClasses) {
            units += graded.units();
            gradePoints = gradePoints.add(graded.grade().points().multiply(BigDecimal.valueOf(graded.units())));
        }
        if (units == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return gradePoints.divide(BigDecimal.valueOf(units), 2, RoundingMode.HALF_UP);
    }
}
