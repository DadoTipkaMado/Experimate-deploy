package hr.tvz.experimate.experimate.domain.user.dto;

import hr.tvz.experimate.experimate.shared.Constraints;
import jakarta.validation.constraints.Size;

public record UpdateUserDto(
        @Size(min = Constraints.UserConstraints.USERNAME_MIN, max = Constraints.UserConstraints.USERNAME_MAX)
        String username,
        @Size(min = Constraints.UserConstraints.PASSWORD_MIN, max = Constraints.UserConstraints.PASSWORD_MAX)
        String password,
        @Size(max = Constraints.UserConstraints.BIO_MAX)
        String bio
) {
}
