package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TourListingRepo extends JpaRepository<TourListing, Integer> {
    boolean existsByHostAndMeetingDateBetween(User user, LocalDateTime start, LocalDateTime end);
    boolean existsByHost_Id(Integer id);
    int deleteAllByHost_Id(Integer id);

    List<TourListing> findAllByReservedAndMeetingDateBefore(Boolean isReserved, LocalDateTime meetingDateTime);

    List<TourListing> findAllByHost_Id(Integer hostId);

    /**
     * Returns all active listings eligible as match candidates for the given viewer.
     * A listing is eligible when it is not reserved, its meeting date is in the future,
     * and its host is not the viewer.
     *
     * @param viewerId the ID of the requesting user, excluded from results
     * @param now      the current timestamp used to filter out expired listings
     * @return list of candidate listings ordered by meeting date ascending
     */
    @Query("SELECT l FROM TourListing l WHERE l.host.id != :viewerId AND l.reserved = false AND l.meetingDate > :now ORDER BY l.meetingDate ASC")
    List<TourListing> findMatchCandidateListings(@Param("viewerId") Integer viewerId, @Param("now") LocalDateTime now);
}
