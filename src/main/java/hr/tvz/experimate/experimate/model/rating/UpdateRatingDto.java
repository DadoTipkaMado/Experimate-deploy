package hr.tvz.experimate.experimate.model.rating;

import hr.tvz.experimate.experimate.model.shared.Constraints;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateRatingDto(
        @Max(Constraints.RatingConstraints.SCORE_MAX) @Min(Constraints.RatingConstraints.SCORE_MIN)
        Integer score,
        @Size(min=Constraints.RatingConstraints.REVIEW_MIN, max=Constraints.RatingConstraints.REVIEW_MAX)
        String review
) {}
