package hr.tvz.experimate.experimate.model.booking_request;

import hr.tvz.experimate.experimate.model.shared.exception.NotFoundException;

public class BookingRequestNotFoundException extends NotFoundException {
    public BookingRequestNotFoundException(Integer id) {
        super("Booking request not found with id " + id);
    }

    public BookingRequestNotFoundException(String message) {
        super(message);
    }
}
