package hr.tvz.experimate.experimate.domain.reservation.exception;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

public class GuestAlreadyBookedException extends ConflictException {
    public GuestAlreadyBookedException(Integer id) {
        super("User with id %d has already booked a listing on the same date.".formatted(id));
    }
}
