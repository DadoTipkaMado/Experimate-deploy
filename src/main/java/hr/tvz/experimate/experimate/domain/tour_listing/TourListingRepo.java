package hr.tvz.experimate.experimate.domain.tour_listing;

import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TourListingRepo extends JpaRepository<TourListing, Integer> {

    /** Projection used by {@link MeetGraphicAssigner} to read only the graphic pair — avoids loading the full entity. */
    interface MeetGraphicProjection {
        Integer getColorIndex();
        Integer getSymbolIndex();
    }
    boolean existsByHost_Id(Integer id);
    int deleteAllByHost_Id(Integer id);

    long countByHost_IdAndMeetingDateAfter(Integer hostId, LocalDateTime now);

    List<TourListing> findAllByHost_Id(Integer hostId, Sort sort);
    Page<TourListing> findAllByHost_Id(Integer hostId, Pageable pageable);

    /**
     * Returns a paginated slice of listings whose host is not the given viewer.
     * Filtering is done at the database level to ensure correct page sizes.
     *
     * @param hostId   ID of the viewer to exclude from results
     * @param pageable pagination and sorting parameters
     * @return page of listings not owned by the viewer
     */
    Page<TourListing> findAllByHost_IdNot(Integer hostId, Pageable pageable);

    /**
     * Returns all expired listings (past their meeting date) that have no active reservations.
     * Used by the scheduled cleanup job to remove listings that never filled up or were abandoned.
     *
     * @param now           the current timestamp; listings with meetingDate before this are expired
     * @param activeStatuses reservation statuses that count as an occupied slot (CONFIRMED, ACTIVE)
     * @return list of listings safe to delete
     */
    @Query("""
        SELECT l FROM TourListing l
        WHERE l.meetingDate < :now
        AND (SELECT COUNT(r) FROM Reservation r
             WHERE r.tourListing = l
             AND r.status IN :activeStatuses) = 0
        """)
    List<TourListing> findExpiredListingsWithNoActiveReservations(
            @Param("now") LocalDateTime now,
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses
    );

    /**
     * Returns all listings eligible as match candidates for the given viewer.
     * A listing qualifies when it has available guest slots, its meeting date is in the future,
     * and its host is not the viewer.
     *
     * @param viewerId      the ID of the requesting user, excluded from results
     * @param now           the current timestamp used to filter out expired listings
     * @param activeStatuses reservation statuses that count as an occupied slot (CONFIRMED, ACTIVE)
     * @return list of candidate listings ordered by meeting date ascending
     */
    /**
     * Returns the graphic pair of all nearby listings that already have a meet graphic assigned.
     * Used by {@link MeetGraphicAssigner} to determine which pairs are taken before running first-fit.
     *
     * <p>Bounding-box filter (not a circle) — conservative pre-inclusion is intentional.
     *
     * @param excludeId the listing being assigned (excluded from results)
     * @param start     lower bound of the meeting-date window
     * @param end       upper bound of the meeting-date window
     * @param minLat    southern edge of the bounding box
     * @param maxLat    northern edge of the bounding box
     * @param minLng    western edge of the bounding box
     * @param maxLng    eastern edge of the bounding box
     */
    @Query("""
        SELECT l.colorIndex AS colorIndex, l.symbolIndex AS symbolIndex
        FROM TourListing l
        WHERE l.id != :excludeId
        AND l.colorIndex IS NOT NULL
        AND l.meetingDate BETWEEN :start AND :end
        AND l.latitude BETWEEN :minLat AND :maxLat
        AND l.longitude BETWEEN :minLng AND :maxLng
        """)
    List<MeetGraphicProjection> findNearbyAssignedGraphics(
            @Param("excludeId") Integer excludeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("minLat") double minLat,
            @Param("maxLat") double maxLat,
            @Param("minLng") double minLng,
            @Param("maxLng") double maxLng
    );

    @Query("""
        SELECT l FROM TourListing l
        WHERE l.host.id != :viewerId
        AND l.meetingDate > :now
        AND (SELECT COUNT(r) FROM Reservation r
             WHERE r.tourListing = l
             AND r.status IN :activeStatuses) < l.maxGuests
        ORDER BY l.meetingDate ASC
        """)
    List<TourListing> findMatchCandidateListings(
            @Param("viewerId") Integer viewerId,
            @Param("now") LocalDateTime now,
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses
    );

}
