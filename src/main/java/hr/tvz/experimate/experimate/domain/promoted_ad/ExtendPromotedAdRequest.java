package hr.tvz.experimate.experimate.domain.promoted_ad;

import jakarta.validation.constraints.Positive;

/**
 * Request body for {@code POST /api/promoted-ads/{id}/extend}.
 *
 * @param additionalDays number of extra days of feed display to purchase; must be positive
 */
public record ExtendPromotedAdRequest(
        @Positive Integer additionalDays
) {}
