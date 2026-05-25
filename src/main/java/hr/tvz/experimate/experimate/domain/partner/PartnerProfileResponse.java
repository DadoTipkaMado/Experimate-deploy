package hr.tvz.experimate.experimate.domain.partner;

import java.time.LocalDateTime;

/**
 * Response body for {@code GET /api/partner/profile} and {@code POST /api/partner/apply}.
 */
public record PartnerProfileResponse(
        Integer id,
        String companyName,
        String contactEmail,
        String website,
        LocalDateTime createdAt
) {}
