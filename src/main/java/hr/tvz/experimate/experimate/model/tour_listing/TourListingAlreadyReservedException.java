package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.shared.ConflictException;

public class TourListingAlreadyReservedException extends ConflictException {
    public TourListingAlreadyReservedException(Integer id) {
        super("Tour listing with id %s is already reserved!".formatted(id));
    }
}
