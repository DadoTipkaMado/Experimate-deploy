package hr.tvz.experimate.experimate.model.reservation;

import hr.tvz.experimate.experimate.model.tour_listing.TourListing;
import hr.tvz.experimate.experimate.model.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="tour_reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name="guest_id")
    private User guest;

    @ManyToOne
    @JoinColumn(name="listing_id")
    private TourListing tourListing;

    private LocalDateTime dateOfReservation;

    //For hibernate
    protected Reservation(){}

    public Reservation(User guest,
                       TourListing tourListing) {
        this.guest = validateGuest(guest);
        this.tourListing = validateTourListing(tourListing);
        this.dateOfReservation = LocalDateTime.now();
    }

    public User getGuest() {
        return guest;
    }

    public TourListing getTourListing() {
        return tourListing;
    }

    public LocalDateTime getDateOfReservation() {
        return dateOfReservation;
    }

    public Integer getId(){
        return id;
    }

    private User validateGuest(User guest){
        if(guest==null)
            throw new IllegalArgumentException("Guest cannot be null");
        return guest;
    }

    private TourListing validateTourListing(TourListing tourListing){
        if(tourListing==null)
            throw new IllegalArgumentException("TourListing cannot be null");
        return  tourListing;
    }
}
