package hr.tvz.experimate.experimate.domain.reservation;

import hr.tvz.experimate.experimate.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepo extends JpaRepository<Reservation, Integer> {
    boolean existsByGuestAndTourListing_MeetingDateBetweenAndStatusIn(User guest, LocalDateTime start, LocalDateTime end, Collection<ReservationStatus> statuses);
    boolean existsByTourListing_Host_Id(Integer hostId);
    boolean existsByGuest_Id(Integer userId);

    @Query(value = "SELECT listing_id FROM tour_reservation WHERE guest_id = :guestId", nativeQuery = true)
    List<Integer> findTourListingIdsByGuestId(@Param("guestId")  Integer guestId);

    int deleteAllByTourListing_Host_Id(Integer hostId);
    int deleteAllByGuest_Id(Integer guestId);
    int deleteAllByTourListing_IdIn(Collection<Integer> tourListingIds);

    List<Reservation> findAllByStatusAndEndTimestampBefore(ReservationStatus status, LocalDateTime endTimestampBefore);
    List<Reservation> findAllByStatusAndTourListing_MeetingDateBefore(ReservationStatus status, LocalDateTime meetingDateBefore);

    Optional<Reservation> findByGuest_IdAndTourListing_Host_IdAndStatus(Integer guestId, Integer tourListingHostId, ReservationStatus status);

    Optional<Reservation> findByTourListing_Id(Integer listingId);

    @Query("SELECT r FROM Reservation r WHERE r.guest.id = :userId OR r.tourListing.host.id = :userId")
    List<Reservation> findAllForUser(@Param("userId") Integer userId);

    List<Reservation> findAllByGuest_IdAndTourListing_MeetingDateAfter(Integer guestId, LocalDateTime now, Sort sort);
    List<Reservation> findAllByGuest_IdAndTourListing_MeetingDateBefore(Integer guestId, LocalDateTime now, Sort sort);
    List<Reservation> findAllByTourListing_Host_IdAndTourListing_MeetingDateAfter(Integer hostId, LocalDateTime now, Sort sort);
    List<Reservation> findAllByTourListing_Host_IdAndTourListing_MeetingDateBefore(Integer hostId, LocalDateTime now, Sort sort);

    Page<Reservation> findAllByGuest_IdAndTourListing_MeetingDateAfter(Integer guestId, LocalDateTime now, Pageable pageable);
    Page<Reservation> findAllByGuest_IdAndTourListing_MeetingDateBefore(Integer guestId, LocalDateTime now, Pageable pageable);
    Page<Reservation> findAllByTourListing_Host_IdAndTourListing_MeetingDateAfter(Integer hostId, LocalDateTime now, Pageable pageable);
    Page<Reservation> findAllByTourListing_Host_IdAndTourListing_MeetingDateBefore(Integer hostId, LocalDateTime now, Pageable pageable);
    Boolean existsByGuest_IdAndTourListing_MeetingDateBetweenAndStatusIn(Integer id, LocalDateTime start, LocalDateTime end, Collection<ReservationStatus> statuses);
    Boolean existsByTourListing_Host_IdAndTourListing_MeetingDateBetweenAndStatusIn(Integer id, LocalDateTime start, LocalDateTime end, Collection<ReservationStatus> statuses);

    long countByTourListing_IdAndStatusIn(Integer listingId, Collection<ReservationStatus> statuses);

    List<Reservation> findAllByTourListing_Id(Integer listingId);

    long countByTourListing_IdAndStatus(Integer listingId, ReservationStatus status);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.tourListing.id = :listingId AND r.status = :status AND r.guestCheckedIn = :guestCheckedIn")
    long countByListingIdAndStatusAndGuestCheckedIn(@Param("listingId") Integer listingId,
                                                    @Param("status") ReservationStatus status,
                                                    @Param("guestCheckedIn") Boolean guestCheckedIn);

    @Query("SELECT r.tourListing.id, COUNT(r) FROM Reservation r " +
           "WHERE r.tourListing.id IN :ids AND r.status IN :statuses " +
           "GROUP BY r.tourListing.id")
    List<Object[]> countByListingIdsAndStatusIn(@Param("ids") List<Integer> ids,
                                                @Param("statuses") Collection<ReservationStatus> statuses);

    /**
     * Returns true if the host has a confirmed or active reservation for a <em>different</em> listing
     * within the given time window. Used to prevent double-booking across tours while allowing
     * multiple guests on the same listing.
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.tourListing.host.id = :hostId " +
           "AND r.tourListing.id <> :excludeListingId " +
           "AND r.tourListing.meetingDate BETWEEN :start AND :end " +
           "AND r.status IN :statuses")
    boolean existsByHostOnDifferentListingInWindow(@Param("hostId") Integer hostId,
                                                   @Param("excludeListingId") Integer excludeListingId,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end,
                                                   @Param("statuses") Collection<ReservationStatus> statuses);
}
