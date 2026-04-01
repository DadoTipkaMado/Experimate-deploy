package hr.tvz.experimate.experimate.model.rating;

public record CreateRatingDto(
        Integer raterId,
        Integer ratedId,
        Integer score,
        String review
) {}
