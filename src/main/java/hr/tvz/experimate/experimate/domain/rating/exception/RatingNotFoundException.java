package hr.tvz.experimate.experimate.domain.rating.exception;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

public class RatingNotFoundException extends NotFoundException {
    public RatingNotFoundException(Integer id) {
        super("Rating not found with id " + id);
    }
}
