package hr.tvz.experimate.experimate.model.user;

public record UpdateUserDto(
        String username,
        String password,
        String bio,
        String profilePhotoUrl
) {
}
