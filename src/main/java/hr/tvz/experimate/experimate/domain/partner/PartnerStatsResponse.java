package hr.tvz.experimate.experimate.domain.partner;

/**
 * Response body for {@code GET /api/partner/stats}.
 *
 * <p>{@code profileViews} and {@code bookings} are stubbed as 0 until view-tracking
 * and a dedicated partner event model (PartnerPin) are implemented. {@code activeEvents}
 * reflects the partner's current live TourListings.
 */
public record PartnerStatsResponse(
        int profileViews,
        long bookings,
        long activeEvents
) {}
