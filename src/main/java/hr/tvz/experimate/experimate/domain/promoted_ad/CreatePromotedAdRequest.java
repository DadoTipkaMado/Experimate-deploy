package hr.tvz.experimate.experimate.domain.promoted_ad;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Request body for {@code POST /api/promoted-ads}.
 * Image upload is handled by a separate {@code POST /api/promoted-ads/{id}/image} endpoint.
 */
public record CreatePromotedAdRequest(
        @NotBlank String title,
        String description,
        String linkUrl,
        LocalDateTime activeFrom,
        LocalDateTime activeUntil
) {}
