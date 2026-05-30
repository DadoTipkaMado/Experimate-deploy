package hr.tvz.experimate.experimate.domain.promoted_ad;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

public class PromotedAdNotFoundException extends NotFoundException {
    public PromotedAdNotFoundException(Integer id) {
        super("Promoted ad not found with id " + id);
    }

    private PromotedAdNotFoundException(String message) {
        super(message);
    }

    /**
     * Builds an exception for the case where an event has no promotion to act on.
     *
     * @param eventId the event that is not currently promoted
     */
    public static PromotedAdNotFoundException forEvent(Integer eventId) {
        return new PromotedAdNotFoundException("No promotion found for event with id " + eventId);
    }
}
