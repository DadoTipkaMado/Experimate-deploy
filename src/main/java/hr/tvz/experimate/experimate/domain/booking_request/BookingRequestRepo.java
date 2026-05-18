package hr.tvz.experimate.experimate.domain.booking_request;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

import java.util.List;

public interface BookingRequestRepo extends JpaRepository<BookingRequest, Integer> {
    @Query(value = "SELECT r.id FROM BookingRequest r WHERE r.listing.id = :listingId")
    List<Integer> findBookingRequestIdsByTourListingId(@Param("listingId") Integer listingId);

    /**
     * Returns IDs of all booking requests for the given listing with the specified status.
     * Used when auto-declining pending requests after a listing becomes full — scoped to PENDING
     * to avoid overwriting the status of already-accepted requests.
     */
    @Query("SELECT r.id FROM BookingRequest r WHERE r.listing.id = :listingId AND r.status = :status")
    List<Integer> findBookingRequestIdsByTourListingIdAndStatus(
            @Param("listingId") Integer listingId,
            @Param("status") BookingRequestStatus status);

    @Query(value = "UPDATE BookingRequest r SET r.status = :status WHERE r.id IN :ids")
    @Modifying
    Integer updateStatusByIds(@Param("ids") List<Integer> ids,
                              @Param("status") BookingRequestStatus status);

    @Query("SELECT COUNT(*) > 0 FROM BookingRequest r " +
            "WHERE r.guest.id = :guestId " +
            "AND r.listing.id = :listingId " +
            "AND r.status = :status")
    boolean existsByGuestIdAndListingIdAndStatus(@Param("guestId") Integer guestId,
                                               @Param("listingId") Integer listingId,
                                               @Param("status") BookingRequestStatus status);

    Optional<BookingRequest> findByListing_Id(Integer listingId);

    int deleteAllByListing_IdIn(Collection<Integer> listing_id);

    List<BookingRequest> findAllByListing_Host_IdAndStatus(Integer hostId, BookingRequestStatus status, Sort sort);
    List<BookingRequest> findAllByGuest_IdAndStatus(Integer guestId, BookingRequestStatus status, Sort sort);

    Page<BookingRequest> findAllByListing_Host_IdAndStatus(Integer hostId, BookingRequestStatus status, Pageable pageable);
    Page<BookingRequest> findAllByGuest_IdAndStatus(Integer guestId, BookingRequestStatus status, Pageable pageable);
}
