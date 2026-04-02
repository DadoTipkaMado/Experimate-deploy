package hr.tvz.experimate.experimate.model.tour_listing;

import java.time.LocalDateTime;

public record TourListingResponse(
        Integer id,
        String city,
        Double lng,
        Double lat,
        LocalDateTime meetingDate,
        LocalDateTime postDate,
        String tourDescription,
        boolean reserved
) {
}
