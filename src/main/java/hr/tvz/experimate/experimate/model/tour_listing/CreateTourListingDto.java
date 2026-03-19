package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.user.User;

import java.time.LocalDateTime;

public record CreateTourListingDto(
        Integer hostId,
        String city,
        LocalDateTime postDate,
        LocalDateTime meetingDate,
        String tourDescription) {
}
