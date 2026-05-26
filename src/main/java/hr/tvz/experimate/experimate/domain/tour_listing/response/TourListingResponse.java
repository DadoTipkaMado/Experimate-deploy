package hr.tvz.experimate.experimate.domain.tour_listing.response;

import com.fasterxml.jackson.annotation.JsonTypeName;
import hr.tvz.experimate.experimate.domain.feed.FeedItem;
import hr.tvz.experimate.experimate.shared.UserDetails;

import java.time.LocalDateTime;

@JsonTypeName("LISTING")
public record TourListingResponse(
        Integer id,
        String city,
        Double lng,
        Double lat,
        LocalDateTime meetingDate,
        LocalDateTime postDate,
        String tourDescription,
        Integer maxGuests,
        Integer bookedCount,
        UserDetails host
) implements FeedItem {}
