package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.shared.exception.ConflictException;

public class TourListingExpiredException extends ConflictException {
    public TourListingExpiredException(Integer listingId) {
        super("Listing with id %s is expired, cannot send booking request.".formatted(listingId));
    }
}
