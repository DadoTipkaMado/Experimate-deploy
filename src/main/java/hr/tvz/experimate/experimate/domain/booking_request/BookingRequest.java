package hr.tvz.experimate.experimate.domain.booking_request;

import hr.tvz.experimate.experimate.domain.tour_listing.TourListing;
import hr.tvz.experimate.experimate.domain.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_request")
public class BookingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "guest_id")
    private final User guest;

    @ManyToOne
    @JoinColumn(name = "listing_id")
    private final TourListing listing;

    private final LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    private BookingRequestStatus status;

    // For Hibernate
    protected BookingRequest() {
        guest = null;
        listing = null;
        requestDate = null;
    }

    public BookingRequest(User guest, TourListing listing) {
        this.guest = validateGuest(guest);
        this.listing = validateListing(listing);
        this.status = BookingRequestStatus.PENDING;
        this.requestDate = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public User getGuest() {
        return guest;
    }

    public TourListing getListing() {
        return listing;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public BookingRequestStatus getStatus(){
        return status;
    }

    public void setStatus(BookingRequestStatus status){
        this.status = status;
    }

    private User validateGuest(User guest) {
        if (guest == null)
            throw new IllegalArgumentException("Guest cannot be null");
        return guest;
    }

    private TourListing validateListing(TourListing listing) {
        if (listing == null)
            throw new IllegalArgumentException("Listing cannot be null");
        return listing;
    }
}
