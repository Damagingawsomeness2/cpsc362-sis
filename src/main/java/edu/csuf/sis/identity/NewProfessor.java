package edu.csuf.sis.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NewProfessor(@NotBlank @Size(max = 100) String name,
                           @NotBlank @Size(max = 100) String department,
                           @Email @Size(max = 120) String email,
                           @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,40}",
                                   message = "must be 3-40 letters, digits, dots, dashes or underscores")
                           String username,
                           @NotBlank @Size(min = 8, max = 72) String password) {
}
