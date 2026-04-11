package hr.tvz.experimate.experimate.model.rating;

import hr.tvz.experimate.experimate.model.shared.exception.ConflictException;

public class DuplicateRatingException extends ConflictException {
    public DuplicateRatingException(Integer raterId, Integer ratedId) {
        super("User with id %d has already rated user with id %d..".formatted(raterId, ratedId));
    }
}
