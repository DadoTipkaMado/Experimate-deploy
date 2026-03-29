package hr.tvz.experimate.experimate.model.rating;

import hr.tvz.experimate.experimate.model.shared.exception.NotFoundException;

public class RatingNotFoundException extends NotFoundException {
    public RatingNotFoundException(Integer id) {
        super("Rating not found with id " + id);
    }
}
