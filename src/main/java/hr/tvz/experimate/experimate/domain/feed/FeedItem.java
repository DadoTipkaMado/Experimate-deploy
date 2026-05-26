package hr.tvz.experimate.experimate.domain.feed;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import hr.tvz.experimate.experimate.domain.promoted_ad.PromotedAdResponse;
import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;

/**
 * Marker interface for items that appear in the public feed.
 *
 * <p>Jackson uses the {@code type} discriminator field to tell the frontend which
 * kind of card it received: {@code "LISTING"} for {@link TourListingResponse} and
 * {@code "AD"} for {@link PromotedAdResponse}.
 *
 * <p><b>Side-effect:</b> all existing endpoints that return {@code TourListingResponse}
 * (e.g. {@code GET /api/tour-listing}) will now include {@code "type": "LISTING"} in
 * their JSON output. This is additive and non-breaking for existing frontend consumers.
 *
 * <p>Only DTO inheritance is used here — JPA entities are unchanged.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TourListingResponse.class, name = "LISTING"),
        @JsonSubTypes.Type(value = PromotedAdResponse.class, name = "AD")
})
public interface FeedItem {}
