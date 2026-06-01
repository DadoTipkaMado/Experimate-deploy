package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

/**
 * Thrown when a pin has no highlight subscription on record.
 * Mapped to HTTP 404 via {@link NotFoundException}.
 */
public class PartnerPinSubscriptionNotFoundException extends NotFoundException {
    public PartnerPinSubscriptionNotFoundException(Integer pinId) {
        super("No highlight subscription found for partner pin with id " + pinId);
    }
}
