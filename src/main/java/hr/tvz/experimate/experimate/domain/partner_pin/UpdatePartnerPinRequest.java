package hr.tvz.experimate.experimate.domain.partner_pin;

/**
 * Request body for {@code PUT /api/partner-pins/{id}}.
 * All fields are optional — only non-null values are applied to the pin.
 */
public record UpdatePartnerPinRequest(
        String name,
        String description,
        Double latitude,
        Double longitude,
        Boolean active
) {}
