package hr.tvz.experimate.experimate.domain.promoted_ad;

import java.time.LocalDateTime;

/**
 * Request body for {@code PUT /api/promoted-ads/{id}}.
 * All fields are optional — only non-null values are applied.
 */
public record UpdatePromotedAdRequest(
        String title,
        String description,
        String linkUrl,
        Boolean active,
        LocalDateTime activeFrom,
        LocalDateTime activeUntil
) {}
