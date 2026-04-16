package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.shared.Constraints;
import hr.tvz.experimate.experimate.model.user.User;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateTourListingDto(
        @Positive Integer hostId,
        @NotBlank String city,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @Future LocalDateTime meetingDate,
        @NotBlank @Size(
                min = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MIN,
                max = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MAX)
        String tourDescription
) {}
