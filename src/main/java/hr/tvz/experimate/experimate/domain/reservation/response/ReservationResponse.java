package hr.tvz.experimate.experimate.domain.reservation.response;

import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.shared.TourListingDetails;
import hr.tvz.experimate.experimate.shared.UserDetails;

import java.time.LocalDateTime;

public record ReservationResponse(
        Integer id,
        LocalDateTime dateOfReservation,
        TourListingDetails tourListing,
        UserDetails guest,
        ReservationStatus status
) {
}
