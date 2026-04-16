package hr.tvz.experimate.experimate.model.booking_request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBookingRequestDto(
        @Positive(message="Guest Id must be positive")
        Integer guestId,
        @Positive(message="Listing Id must be positive")
        Integer listingId) {
}
