package hr.tvz.experimate.experimate.domain.partner_pin;

import java.time.LocalDateTime;

/**
 * API response for a {@link PartnerPin}.
 *
 * <p>{@code partnerCompanyName} is embedded directly so the map UI can display the
 * business name without a second request to {@code GET /api/partner/profile}.
 *
 * <p>{@code logoUrl} is {@code null} when no logo has been uploaded yet.
 */
public record PartnerPinResponse(
        Integer id,
        String name,
        String description,
        String logoUrl,
        Double latitude,
        Double longitude,
        Boolean active,
        LocalDateTime createdAt,
        String partnerCompanyName
) {}
