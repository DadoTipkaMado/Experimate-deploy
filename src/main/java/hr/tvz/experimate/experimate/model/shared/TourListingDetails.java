package hr.tvz.experimate.experimate.model.shared;

import java.time.LocalDateTime;

public record TourListingDetails(
        LocalDateTime meetingDate,
        String city,
        UserDetails host
) {
}
