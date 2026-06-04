package hr.tvz.experimate.experimate.domain.booking_request.response;

import hr.tvz.experimate.experimate.domain.booking_request.BookingRequestStatus;
import hr.tvz.experimate.experimate.shared.TourListingDetails;
import hr.tvz.experimate.experimate.shared.UserDetails;

import java.time.LocalDateTime;

public record BookingRequestResponse(Integer id,
                                     BookingRequestStatus status,
                                     LocalDateTime requestDate,
                                     TourListingDetails tourListing,
                                     UserDetails user) {
}
