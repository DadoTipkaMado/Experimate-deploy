package hr.tvz.experimate.experimate.model.rating;

public record RatingResponse(
        Integer id,
        Integer score,
        String review
) {
}
