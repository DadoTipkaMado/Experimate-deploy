package hr.tvz.experimate.experimate.domain.promoted_ad;

import com.fasterxml.jackson.annotation.JsonTypeName;
import hr.tvz.experimate.experimate.domain.feed.FeedItem;

import java.time.LocalDateTime;

/**
 * API response for a {@link PromotedAd}.
 *
 * <p>{@code imageUrl} is {@code null} when no image has been uploaded.
 * {@code viewCount} is always 0 (stub for future impression tracking).
 *
 * <p>{@code eventId} is the ID of the {@link hr.tvz.experimate.experimate.domain.partner_event.PartnerEvent}
 * this ad promotes, or {@code null} for a regular free-form ad. The frontend uses its presence to
 * render the card as a sponsored event.
 */
@JsonTypeName("AD")
public record PromotedAdResponse(
        Integer id,
        String title,
        String description,
        String imageUrl,
        String linkUrl,
        Boolean active,
        Integer viewCount,
        Integer eventId,
        LocalDateTime activeFrom,
        LocalDateTime activeUntil,
        LocalDateTime createdAt
) implements FeedItem {}
