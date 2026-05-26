package hr.tvz.experimate.experimate.domain.partner_pin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/partner-pins}.
 * Logo upload is handled by a separate {@code POST /api/partner-pins/{id}/logo} endpoint.
 */
public record CreatePartnerPinRequest(
        @NotBlank String name,
        String description,
        @NotNull Double latitude,
        @NotNull Double longitude
) {}
