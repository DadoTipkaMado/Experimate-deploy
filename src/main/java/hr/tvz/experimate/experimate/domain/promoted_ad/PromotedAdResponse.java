package hr.tvz.experimate.experimate.domain.promoted_ad;

import com.fasterxml.jackson.annotation.JsonTypeName;
import hr.tvz.experimate.experimate.domain.feed.FeedItem;

import java.time.LocalDateTime;

/**
 * API response for a {@link PromotedAd}.
 *
 * <p>{@code imageUrl} is {@code null} when no image has been uploaded.
 * {@code viewCount} is always 0 (stub for future impression tracking).
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
        LocalDateTime activeFrom,
        LocalDateTime activeUntil,
        LocalDateTime createdAt
) implements FeedItem {}
