package hr.tvz.experimate.experimate.domain.partner_event;

import java.time.LocalDateTime;

/**
 * Request body for {@code PUT /api/partner-events/{id}}.
 * All fields are optional — only non-null values are applied.
 */
public record UpdatePartnerEventRequest(
        String title,
        String description,
        String ticketVendorUrl,
        LocalDateTime startDatetime,
        LocalDateTime endDatetime
) {}
