package hr.tvz.experimate.experimate.domain.reservation.exception;

public class IllegalReservationStateException extends IllegalStateException {
    public IllegalReservationStateException(String message) {
        super(message);
    }
}
