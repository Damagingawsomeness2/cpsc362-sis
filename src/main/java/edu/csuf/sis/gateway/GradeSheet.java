package edu.csuf.sis.gateway;

import edu.csuf.sis.records.GradeEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GradeSheet(@NotEmpty List<@Valid GradeEntry> grades) {
}
