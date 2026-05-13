package hr.tvz.experimate.experimate.domain.rating.exception;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

public class DuplicateRatingException extends ConflictException {
    public DuplicateRatingException(Integer raterId, Integer ratedId) {
        super("User with id %d has already rated user with id %d..".formatted(raterId, ratedId));
    }
}
