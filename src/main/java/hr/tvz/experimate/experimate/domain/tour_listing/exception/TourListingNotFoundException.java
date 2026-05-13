package hr.tvz.experimate.experimate.domain.tour_listing.exception;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

public class TourListingNotFoundException extends NotFoundException {
    public TourListingNotFoundException(Integer id) {
        super("Listing not found with id: " + id);
    }
}
