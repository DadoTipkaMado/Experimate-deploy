package hr.tvz.experimate.experimate.domain.tour_listing.exception;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

public class TourListingAlreadyReservedException extends ConflictException {
    public TourListingAlreadyReservedException(Integer id) {
        super("Tour listing with id %s is already reserved!".formatted(id));
    }
}
