package hr.tvz.experimate.experimate.domain.rating;

public record RatingResponse(
        Integer id,
        Integer score,
        String review
) {
}
