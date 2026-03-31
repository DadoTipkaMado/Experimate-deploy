package hr.tvz.experimate.experimate.model.user;

public record UserResponse(
        Integer id,
        String username,
        double rating
) {
}
