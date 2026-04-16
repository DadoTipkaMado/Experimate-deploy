package hr.tvz.experimate.experimate.model.user;

import hr.tvz.experimate.experimate.model.shared.Constraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateUserDto(
        @NotBlank @Size(min = Constraints.UserConstraints.FIRST_NAME_MIN, max = Constraints.UserConstraints.FIRST_NAME_MAX)
        String firstName,
        @NotBlank @Size(min = Constraints.UserConstraints.LAST_NAME_MIN, max = Constraints.UserConstraints.LAST_NAME_MAX)
        String lastName,
        @Past LocalDate dateOfBirth,
        @NotBlank
        String idNumber,
        @NotBlank @Size(min = Constraints.UserConstraints.USERNAME_MIN, max = Constraints.UserConstraints.USERNAME_MAX)
        String username,
        @NotBlank @Size(min = Constraints.UserConstraints.PASSWORD_MIN, max = Constraints.UserConstraints.PASSWORD_MAX)
        String password,
        @Size(max = Constraints.UserConstraints.BIO_MAX)
        String bio
) {}
