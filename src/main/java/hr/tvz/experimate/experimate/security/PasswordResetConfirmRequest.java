package hr.tvz.experimate.experimate.security;

import hr.tvz.experimate.experimate.shared.Constraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank String token,
        @NotBlank @Size(min = Constraints.UserConstraints.PASSWORD_MIN, max = Constraints.UserConstraints.PASSWORD_MAX)
        String newPassword
) {}
