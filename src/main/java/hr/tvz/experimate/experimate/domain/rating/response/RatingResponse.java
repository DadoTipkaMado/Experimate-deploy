package hr.tvz.experimate.experimate.domain.rating.response;

import hr.tvz.experimate.experimate.shared.UserDetails;

public record RatingResponse(
        Integer id,
        Integer score,
        String review,
        UserDetails rater,
        UserDetails rated
) {
}
