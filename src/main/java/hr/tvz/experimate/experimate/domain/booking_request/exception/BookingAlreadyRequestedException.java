package hr.tvz.experimate.experimate.domain.booking_request.exception;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

public class BookingAlreadyRequestedException extends ConflictException {
    public BookingAlreadyRequestedException(Integer guestId, Integer listingId) {
        super("Guest with id %d already has a pending booking request for listing %d"
                .formatted(guestId, listingId)
        );
    }
}
