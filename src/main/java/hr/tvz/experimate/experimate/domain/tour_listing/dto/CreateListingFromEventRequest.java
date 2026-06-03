package hr.tvz.experimate.experimate.domain.tour_listing.dto;

import hr.tvz.experimate.experimate.shared.Constraints;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Request body for creating a {@code TourListing} pre-filled from a {@code PartnerEvent}.
 *
 * <p>The event supplies the default meeting time and the parent pin's coordinates.
 * {@code city} is always required because {@code PartnerPin} stores only coordinates,
 * not a human-readable city name.
 *
 * <p>The three {@code override*} fields are all optional — when omitted the values
 * from the event/pin are used as-is:
 * <ul>
 *   <li>{@code overrideMeetingDate} — use if the host wants a different time than the event start</li>
 *   <li>{@code overrideLatitude} / {@code overrideLongitude} — use if the host wants a slightly different meeting spot</li>
 * </ul>
 */
public record CreateListingFromEventRequest(
        @NotNull Integer partnerEventId,
        @NotBlank String city,
        @NotBlank @Size(max = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MAX)
        String tourDescription,
        @NotNull @Min(Constraints.TourListingConstraints.MIN_GUESTS)
        @Max(Constraints.TourListingConstraints.MAX_GUESTS)
        Integer maxGuests,
        LocalDateTime overrideMeetingDate,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double overrideLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double overrideLongitude
) {}
