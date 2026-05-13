package hr.tvz.experimate.experimate.domain.tour_listing.exception;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

public class HostAlreadyTakenException extends ConflictException {
    public HostAlreadyTakenException(Integer id) {
        super("User with id %d already listed a tour on the same date.".formatted(id));
    }
}
