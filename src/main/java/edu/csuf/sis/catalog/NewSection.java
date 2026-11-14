package edu.csuf.sis.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/** Days use catalog codes (M, Tu, W, Th, F, Sa, Su); leave days and times empty for "to be arranged". */
public record NewSection(@NotNull Long termId,
                         @NotBlank String courseCode,
                         @NotBlank @Size(max = 4) String sectionNumber,
                         Long professorId,
                         @Min(1) @Max(500) int capacity,
                         @Size(max = 10) String days,
                         LocalTime startTime,
                         LocalTime endTime,
                         @Size(max = 20) String room) {
}
