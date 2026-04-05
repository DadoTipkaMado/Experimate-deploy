package hr.tvz.experimate.experimate.model.booking_request;

import hr.tvz.experimate.experimate.model.shared.TourListingDetails;
import hr.tvz.experimate.experimate.model.shared.UserDetails;

import java.time.LocalDateTime;

public record BookingRequestResponse(Integer id,
                                     BookingRequestStatus status,
                                     LocalDateTime requestDate,
                                     TourListingDetails tourListing,
                                     UserDetails user) {
}
