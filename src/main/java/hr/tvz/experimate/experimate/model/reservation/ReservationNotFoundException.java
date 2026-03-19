package hr.tvz.experimate.experimate.model.reservation;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Integer id) {
        super("Reservation not found with id " + id);
    }

    public ReservationNotFoundException(String message) {
        super(message);
    }
}
