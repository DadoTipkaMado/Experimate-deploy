package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.shared.exception.ConflictException;

public class HostAlreadyTakenException extends ConflictException {
    public HostAlreadyTakenException(Integer id) {
        super("User with id %d already listed a tour on the same date.".formatted(id));
    }
}
