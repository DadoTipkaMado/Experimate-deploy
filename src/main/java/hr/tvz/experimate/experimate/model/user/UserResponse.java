package hr.tvz.experimate.experimate.model.user;

public record UserResponse(
        Integer id,
        String username,
        String firstName,
        String lastName,
        String bio,
        double rating
) {
}
