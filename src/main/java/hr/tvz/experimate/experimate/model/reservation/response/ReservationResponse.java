package hr.tvz.experimate.experimate.model.reservation.response;

import hr.tvz.experimate.experimate.model.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.model.shared.TourListingDetails;
import hr.tvz.experimate.experimate.model.shared.UserDetails;

import java.time.LocalDateTime;

public record ReservationResponse(
        Integer id,
        LocalDateTime dateOfReservation,
        TourListingDetails tourListing,
        UserDetails guest,
        ReservationStatus status
) {
}
