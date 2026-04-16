package hr.tvz.experimate.experimate.model.user.response;

public record UserResponse(
        Integer id,
        String username,
        String firstName,
        String lastName,
        String bio,
        double rating,
        String profilePhotoUrl
) {
}
