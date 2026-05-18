package hr.tvz.experimate.experimate.domain.rating.dto;

import hr.tvz.experimate.experimate.shared.Constraints;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateRatingDto(
        @Max(Constraints.RatingConstraints.SCORE_MAX) @Min(Constraints.RatingConstraints.SCORE_MIN)
        Integer score,
        @Size(min=Constraints.RatingConstraints.REVIEW_MIN, max=Constraints.RatingConstraints.REVIEW_MAX)
        String review
) {}
