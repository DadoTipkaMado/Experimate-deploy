package hr.tvz.experimate.experimate.domain.reservation.exception;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

public class ReservationNotFoundException extends NotFoundException {
    public ReservationNotFoundException(Integer id) {
        super("Reservation not found with id " + id);
    }

    public ReservationNotFoundException(String message) {
        super(message);
    }
}
