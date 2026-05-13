package hr.tvz.experimate.experimate.domain.tour_listing.exception;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

public class TourListingExpiredException extends ConflictException {
    public TourListingExpiredException(Integer listingId) {
        super("Listing with id %s is expired, cannot send booking request.".formatted(listingId));
    }
}
