package hr.tvz.experimate.experimate.domain.partner_pin;

import java.time.LocalDateTime;

/**
 * API response for a {@link PartnerPin}.
 *
 * <p>{@code partnerCompanyName} is embedded directly so the map UI can display the
 * business name without a second request to {@code GET /api/partner/profile}.
 *
 * <p>{@code logoUrl} is {@code null} when no logo has been uploaded yet.
 *
 * <p>{@code highlighted} is derived from the pin's {@link PartnerPinSubscription}: it is
 * {@code true} only while an active subscription's paid period is still running. The map uses it
 * to render highlighted pins more prominently.
 */
public record PartnerPinResponse(
        Integer id,
        String name,
        String description,
        String logoUrl,
        Double latitude,
        Double longitude,
        Boolean active,
        Boolean highlighted,
        LocalDateTime createdAt,
        String partnerCompanyName
) {}
