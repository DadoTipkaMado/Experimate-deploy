package hr.tvz.experimate.experimate.model.reservation;

import hr.tvz.experimate.experimate.model.shared.ConflictException;

public class GuestAlreadyBookedException extends ConflictException {
    public GuestAlreadyBookedException(Integer id) {
        super("User with id %d has already booked a listing on the same date.".formatted(id));
    }
}
