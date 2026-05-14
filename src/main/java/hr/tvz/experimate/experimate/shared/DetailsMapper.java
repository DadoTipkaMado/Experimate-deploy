package hr.tvz.experimate.experimate.shared;

import hr.tvz.experimate.experimate.domain.tour_listing.TourListing;
import hr.tvz.experimate.experimate.domain.user.User;
import org.springframework.stereotype.Component;

/**
 * Centralized mapper for shared "details" projections of domain entities.
 *
 * <p>Used by services that need to expose a lightweight, API-safe view of a
 * {@link User} or {@link TourListing} as part of their own response payloads
 * (e.g. {@code TourListingResponse}, {@code ReservationResponse},
 * {@code BookingRequestResponse}).
 */
@Component
public class DetailsMapper {

    /**
     * Maps a {@link User} entity to its public-facing {@link UserDetails} projection.
     *
     * <p>Exposes only fields safe for inclusion in API responses
     * (first name, last name, username) — sensitive attributes such as the
     * password hash or email are intentionally omitted.
     *
     * @param user the source user entity; must not be {@code null}
     * @return a populated {@link UserDetails} record
     */
    public UserDetails mapUserDetails(User user) {
        return new UserDetails(
                user.getFirstName(),
                user.getLastName(),
                user.getUsername()
        );
    }

    /**
     * Maps a {@link TourListing} entity to its public-facing {@link TourListingDetails} projection.
     *
     * <p>The listing's host is mapped through {@link #mapUserDetails(User)} so the
     * shape of nested host data stays consistent with every other place a user
     * is exposed in API responses.
     *
     * @param tourListing the source tour listing entity; must not be {@code null}
     * @return a populated {@link TourListingDetails} record with the host
     *         already projected to {@link UserDetails}
     */
    public TourListingDetails mapListingDetails(TourListing tourListing) {
        return new TourListingDetails(
            tourListing.getId(),
            tourListing.getMeetingDate(),
            tourListing.getCity(),
            mapUserDetails(tourListing.getHost())
        );
    }
}
