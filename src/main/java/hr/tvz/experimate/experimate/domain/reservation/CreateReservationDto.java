package hr.tvz.experimate.experimate.domain.reservation;

import jakarta.validation.constraints.Positive;

public record CreateReservationDto(
        @Positive Integer guestId,
        @Positive Integer tourListingId) {
}
