package hr.tvz.experimate.experimate.domain.tour_listing.dto;

import hr.tvz.experimate.experimate.shared.Constraints;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTourListingDto(
        @NotBlank String city,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @Future LocalDateTime meetingDate,
        @NotBlank @Size(max = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MAX)
        String tourDescription,
        @NotNull @Min(Constraints.TourListingConstraints.MIN_GUESTS) @Max(Constraints.TourListingConstraints.MAX_GUESTS)
        Integer maxGuests
) {}
