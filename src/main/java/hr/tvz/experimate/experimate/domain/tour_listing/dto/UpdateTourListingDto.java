package hr.tvz.experimate.experimate.domain.tour_listing.dto;

import hr.tvz.experimate.experimate.shared.Constraints;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateTourListingDto(
        @Future LocalDateTime meetingDate,
        @NotBlank @Size(
                min = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MIN,
                max = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MAX)
        String tourDescription
) {}
