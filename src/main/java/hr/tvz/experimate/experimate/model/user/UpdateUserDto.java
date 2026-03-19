package hr.tvz.experimate.experimate.model.user;

import java.util.Optional;

public record UpdateUserDto(
        String username,
        String password,
        String bio
) {
}
