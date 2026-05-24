package hr.tvz.experimate.experimate.security.google.dto;

import hr.tvz.experimate.experimate.shared.Constraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GoogleRegistrationRequest(
        @NotBlank String idToken,
        @NotBlank @Size(min = Constraints.UserConstraints.USERNAME_MIN, max = Constraints.UserConstraints.USERNAME_MAX)
        String username,
        @NotBlank @Size(min = Constraints.UserConstraints.PASSWORD_MIN, max = Constraints.UserConstraints.PASSWORD_MAX)
        String password,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank String idNumber
) {}
