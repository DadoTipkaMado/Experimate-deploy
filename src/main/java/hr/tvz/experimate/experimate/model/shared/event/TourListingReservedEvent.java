package hr.tvz.experimate.experimate.model.shared.event;

import hr.tvz.experimate.experimate.model.tour_listing.UpdateTourListingDto;

public record TourListingReservedEvent(Integer listingId, UpdateTourListingDto updateDetails) {
}
