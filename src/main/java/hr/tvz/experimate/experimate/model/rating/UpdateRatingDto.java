package hr.tvz.experimate.experimate.model.rating;

public record UpdateRatingDto(
        Integer score,
        String review
) {}
