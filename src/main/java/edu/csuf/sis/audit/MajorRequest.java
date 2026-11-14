package edu.csuf.sis.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MajorRequest(@NotBlank @Size(max = 100) String name,
                           @NotBlank @Size(max = 20) String degree,
                           @Min(1) @Max(300) int totalUnits,
                           @NotNull List<String> requiredCourses) {
}
