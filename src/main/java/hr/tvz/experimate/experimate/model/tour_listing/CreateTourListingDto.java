package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.user.User;

import java.time.LocalDateTime;

public record CreateTourListingDto(
        Integer hostId,
        String city,
        Double longitude,
        Double latitude,
        LocalDateTime meetingDate,
        String tourDescription) {
}
