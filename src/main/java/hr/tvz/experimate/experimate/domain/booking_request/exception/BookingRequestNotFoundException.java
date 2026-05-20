package hr.tvz.experimate.experimate.domain.booking_request.exception;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

public class BookingRequestNotFoundException extends NotFoundException {
    public BookingRequestNotFoundException(Integer id) {
        super("Booking request not found with id " + id);
    }

    public BookingRequestNotFoundException(String message) {
        super(message);
    }
}
