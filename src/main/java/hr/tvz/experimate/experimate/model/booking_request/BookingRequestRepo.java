package hr.tvz.experimate.experimate.model.booking_request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRequestRepo extends JpaRepository<BookingRequest, Integer> {

    @Query(value = "SELECT r.id FROM BookingRequest r WHERE r.listing.id = :listingId")
    List<Integer> findBookingRequestIdsByTourListingId(@Param("listingId") Integer listingId);

    @Query(value = "UPDATE BookingRequest r SET r.status = :status WHERE r.id IN :ids")
    @Modifying
    Integer updateStatusByIds(@Param("ids") List<Integer> ids,
                              @Param("status") BookingRequestStatus status);
}
