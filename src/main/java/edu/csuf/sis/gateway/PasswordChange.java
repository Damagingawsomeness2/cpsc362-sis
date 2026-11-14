package edu.csuf.sis.gateway;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChange(@NotBlank @Size(min = 8, max = 72) String password) {
}
