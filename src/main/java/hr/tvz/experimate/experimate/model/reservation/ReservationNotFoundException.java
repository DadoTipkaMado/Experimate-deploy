package hr.tvz.experimate.experimate.model.reservation;

import hr.tvz.experimate.experimate.model.shared.NotFoundException;

public class ReservationNotFoundException extends NotFoundException {
    public ReservationNotFoundException(Integer id) {
        super("Reservation not found with id " + id);
    }

    public ReservationNotFoundException(String message) {
        super(message);
    }
}
