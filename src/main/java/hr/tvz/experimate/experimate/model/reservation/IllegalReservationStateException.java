package hr.tvz.experimate.experimate.model.reservation;

public class IllegalReservationStateException extends IllegalStateException {
    public IllegalReservationStateException(String message) {
        super(message);
    }
}
