package hr.tvz.experimate.experimate.model.tour_listing.exception;

import hr.tvz.experimate.experimate.model.shared.exception.NotFoundException;

public class TourListingNotFoundException extends NotFoundException {
    public TourListingNotFoundException(Integer id) {
        super("Listing not found with id: " + id);
    }
}
