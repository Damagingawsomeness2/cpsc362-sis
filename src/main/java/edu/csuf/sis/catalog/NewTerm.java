package edu.csuf.sis.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record NewTerm(@NotBlank @Size(max = 40) String name,
                      @NotNull LocalDate startDate,
                      @NotNull LocalDate endDate) {
}
