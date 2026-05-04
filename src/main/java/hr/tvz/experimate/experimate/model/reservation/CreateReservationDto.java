package hr.tvz.experimate.experimate.model.reservation;

import jakarta.validation.constraints.Positive;

public record CreateReservationDto(
        @Positive Integer guestId,
        @Positive Integer tourListingId) {
}
