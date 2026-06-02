package hr.tvz.experimate.experimate.domain.partner_event;

import java.time.LocalDateTime;

/**
 * API response for a {@link PartnerEvent}.
 *
 * <p>Includes the parent pin's coordinates and name so the frontend does not need
 * a separate pin lookup when displaying event details or pre-filling a listing form.
 */
public record PartnerEventResponse(
        Integer id,
        Integer partnerPinId,
        String pinName,
        Double pinLatitude,
        Double pinLongitude,
        String title,
        String description,
        String ticketVendorUrl,
        LocalDateTime startDatetime,
        LocalDateTime endDatetime,
        LocalDateTime createdAt
) {}
