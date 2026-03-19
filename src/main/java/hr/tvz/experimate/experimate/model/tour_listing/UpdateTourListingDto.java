package hr.tvz.experimate.experimate.model.tour_listing;

import java.time.LocalDateTime;

public record UpdateTourListingDto(LocalDateTime meetingDate,
                                   String tourDescription) {
}
