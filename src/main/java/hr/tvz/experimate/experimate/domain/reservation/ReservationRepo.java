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
    boolean existsByGuestAndTourListing_MeetingDateBetween(User guest, LocalDateTime start, LocalDateTime end);
    boolean existsByTourListing_Host_Id(Integer hostId);
    boolean existsByGuest_Id(Integer userId);

    @Query(value = "SELECT listing_id FROM tour_reservation WHERE guest_id = :guestId", nativeQuery = true)
    List<Integer> findTourListingIdsByGuestId(@Param("guestId")  Integer guestId);

    int deleteAllByTourListing_Host_Id(Integer hostId);
    int deleteAllByGuest_Id(Integer guestId);
    int deleteAllByTourListing_IdIn(Collection<Integer> tourListingIds);

    List<Reservation> findAllByStatusAndEndTimestampBefore(ReservationStatus status, LocalDateTime endTimestampBefore);

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
    Boolean existsByGuest_IdAndTourListing_MeetingDateBetween(Integer id, LocalDateTime start, LocalDateTime end);
    Boolean existsByTourListing_Host_IdAndTourListing_MeetingDateBetween(Integer id, LocalDateTime start, LocalDateTime end);
}
