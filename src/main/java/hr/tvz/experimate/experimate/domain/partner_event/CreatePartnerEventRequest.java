package hr.tvz.experimate.experimate.domain.partner_event;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request body for {@code POST /api/partner-pins/{pinId}/events}.
 *
 * <p>Both datetimes must be in the future at creation time, and {@code endDatetime}
 * must be strictly after {@code startDatetime}. This is validated in the service.
 */
public record CreatePartnerEventRequest(
        @NotBlank String title,
        String description,
        String ticketVendorUrl,
        @NotNull @Future LocalDateTime startDatetime,
        @NotNull @Future LocalDateTime endDatetime
) {}
