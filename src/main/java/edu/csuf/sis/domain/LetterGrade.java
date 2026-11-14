package edu.csuf.sis.domain;

import java.math.BigDecimal;
import java.util.Locale;

/** CSUF letter grades and the grade points each one is worth. */
public enum LetterGrade {
    A_PLUS("A+", "4.0"),
    A("A", "4.0"),
    A_MINUS("A-", "3.7"),
    B_PLUS("B+", "3.3"),
    B("B", "3.0"),
    B_MINUS("B-", "2.7"),
    C_PLUS("C+", "2.3"),
    C("C", "2.0"),
    C_MINUS("C-", "1.7"),
    D_PLUS("D+", "1.3"),
    D("D", "1.0"),
    D_MINUS("D-", "0.7"),
    F("F", "0.0");

    private final String label;
    private final BigDecimal points;

    LetterGrade(String label, String points) {
        this.label = label;
        this.points = new BigDecimal(points);
    }

    /** Parses what a professor types, e.g. "a-" or " B+ ". */
    public static LetterGrade fromLabel(String label) {
        if (label != null) {
            String wanted = label.trim().toUpperCase(Locale.ROOT);
            for (LetterGrade grade : values()) {
                if (grade.label.equals(wanted)) {
                    return grade;
                }
            }
        }
        throw new IllegalArgumentException("'" + label + "' is not a letter grade (A+ through F).");
    }

    /** The grade as printed on schedules and transcripts, e.g. "A-". */
    public String label() {
        return label;
    }

    public BigDecimal points() {
        return points;
    }

    /** Whether the class counts as successfully completed (units earned, prerequisite met). */
    public boolean isPassing() {
        return this != F;
    }
}
