package edu.csuf.sis.records;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A grade a professor is posting, e.g. studentId 7 gets "A-". */
public record GradeEntry(@NotNull Long studentId, @NotBlank String grade) {
}
