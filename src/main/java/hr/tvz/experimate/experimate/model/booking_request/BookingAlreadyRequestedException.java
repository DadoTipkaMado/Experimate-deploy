package hr.tvz.experimate.experimate.model.booking_request;

import hr.tvz.experimate.experimate.model.shared.exception.ConflictException;

public class BookingAlreadyRequestedException extends ConflictException {
    public BookingAlreadyRequestedException(Integer guestId, Integer listingId) {
        super("Guest with id %d already has a pending booking request for listing %d"
                .formatted(guestId, listingId)
        );
    }
}
