package hr.tvz.experimate.experimate.domain.partner_event;

import jakarta.validation.constraints.Future;

import java.time.LocalDateTime;

/**
 * Request body for {@code PUT /api/partner-events/{id}}.
 *
 * <p>All fields are optional — only non-null values are applied. When a datetime
 * is supplied it must be in the future ({@code @Future} passes on {@code null}),
 * preventing an event from being moved into the past. The end-after-start rule is
 * enforced in the service.
 */
public record UpdatePartnerEventRequest(
        String title,
        String description,
        String ticketVendorUrl,
        @Future LocalDateTime startDatetime,
        @Future LocalDateTime endDatetime
) {}
