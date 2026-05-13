package hr.tvz.experimate.experimate.shared.event;

import hr.tvz.experimate.experimate.domain.tour_listing.UpdateTourListingDto;

public record TourListingReservedEvent(Integer listingId, UpdateTourListingDto updateDetails) {
}
