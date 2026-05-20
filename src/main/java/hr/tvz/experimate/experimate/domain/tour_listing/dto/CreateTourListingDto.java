package hr.tvz.experimate.experimate.domain.tour_listing.dto;

import hr.tvz.experimate.experimate.shared.Constraints;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateTourListingDto(
        @NotBlank String city,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @Future LocalDateTime meetingDate,
        @NotBlank @Size(
                min = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MIN,
                max = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MAX)
        String tourDescription
) {}
