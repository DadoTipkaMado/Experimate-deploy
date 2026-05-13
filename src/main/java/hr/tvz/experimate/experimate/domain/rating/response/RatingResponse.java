package hr.tvz.experimate.experimate.domain.rating.response;

public record RatingResponse(
        Integer id,
        Integer score,
        String review
) {
}
