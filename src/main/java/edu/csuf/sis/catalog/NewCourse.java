package edu.csuf.sis.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NewCourse(@NotBlank String code,
                        @NotBlank @Size(max = 120) String title,
                        @Size(max = 2000) String description,
                        @Min(1) @Max(6) int units,
                        List<String> prerequisites) {
}
