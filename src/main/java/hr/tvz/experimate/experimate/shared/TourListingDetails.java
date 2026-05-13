package hr.tvz.experimate.experimate.shared;

import java.time.LocalDateTime;

public record TourListingDetails(
        Integer id,
        LocalDateTime meetingDate,
        String city,
        UserDetails host
) {
}
