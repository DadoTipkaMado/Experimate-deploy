package hr.tvz.experimate.experimate.domain.promoted_ad;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

/**
 * Thrown when promoting a partner event that already has an active promotion.
 * An event may have at most one promotion at a time.
 */
public class EventAlreadyPromotedException extends ConflictException {
    public EventAlreadyPromotedException(Integer eventId) {
        super("Event with id " + eventId + " is already promoted");
    }
}
