package hr.tvz.experimate.experimate.domain.tour_listing.response;

import hr.tvz.experimate.experimate.shared.UserDetails;

import java.time.LocalDateTime;

public record TourListingResponse(
        Integer id,
        String city,
        Double lng,
        Double lat,
        LocalDateTime meetingDate,
        LocalDateTime postDate,
        String tourDescription,
        boolean reserved,
        UserDetails host
) {
}
