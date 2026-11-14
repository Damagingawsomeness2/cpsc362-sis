package edu.csuf.sis.gateway;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(@NotNull Long sectionId) {
}
