package hr.tvz.experimate.experimate.domain.reservation;

import hr.tvz.experimate.experimate.domain.tour_listing.TourListing;
import hr.tvz.experimate.experimate.domain.user.User;
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

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    private ReservationStatus status;

    @Column(name="hostCheckedIn")
    private Boolean hostCheckedIn;
    @Column(name="hostRated")
    private Boolean hostRated;
    @Column(name="guestCheckedIn")
    private Boolean guestCheckedIn;
    @Column(name="guestRated")
    private Boolean guestRated;

    @ManyToOne
    @JoinColumn(name="endedByUser_id")
    private User endedBy;

    private LocalDateTime hostCheckInTimestamp;
    private LocalDateTime guestCheckInTimestamp;

    private LocalDateTime startTimestamp;
    private LocalDateTime endTimestamp;
    private LocalDateTime completedTimestamp;
    private LocalDateTime cancelledTimestamp;

    //For hibernate
    protected Reservation(){}

    public Reservation(User guest,
                       TourListing tourListing) {
        this.guest = validateGuest(guest);
        this.tourListing = validateTourListing(tourListing);
        this.dateOfReservation = LocalDateTime.now();

        status = ReservationStatus.CONFIRMED;
        hostCheckedIn = false;
        guestCheckedIn = false;
        hostRated = false;
        guestRated = false;
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

    public LocalDateTime getHostCheckInTimestamp() {
        return hostCheckInTimestamp;
    }

    public LocalDateTime getGuestCheckInTimestamp() {
        return guestCheckInTimestamp;
    }

    public LocalDateTime getStartTimestamp() {
        return startTimestamp;
    }

    public LocalDateTime getEndTimestamp() {
        return endTimestamp;
    }

    public LocalDateTime getCompletedTimestamp() {
        return completedTimestamp;
    }

    public LocalDateTime getCancelTimeStamp(){
        return cancelledTimestamp;
    }

    public Boolean isGuestCheckedIn(){
        return guestCheckedIn;
    }

    public Boolean guestRated(){
        return guestRated;
    }

    public Boolean isHostCheckedIn(){
        return hostCheckedIn;
    }

    public Boolean hostRated(){
        return hostRated;
    }

    public Boolean bothCheckedIn(){
        return hostCheckedIn && guestCheckedIn;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public User getEndedBy(){
        return endedBy;
    }

    public void checkGuestIn(){
        guestCheckedIn = true;
        guestCheckInTimestamp = LocalDateTime.now();
    }

    public void checkHostIn(){
        hostCheckedIn = true;
        hostCheckInTimestamp = LocalDateTime.now();
    }

    public void setGuestRated(Boolean value){
        guestRated = value;
    }

    public void setHostRated(Boolean value){
        hostRated = value;
    }

    public void endBy(User user){
        status = ReservationStatus.CLOSED;
        endedBy = user;
        endTimestamp = LocalDateTime.now();
    }

    public void activate(){
        status = ReservationStatus.ACTIVE;
        startTimestamp = LocalDateTime.now();
    }

    public void cancel(){
        status = ReservationStatus.CANCELLED;
        cancelledTimestamp = LocalDateTime.now();
    }

    public void expire(){
        status = ReservationStatus.EXPIRED;
    }

    public void complete(){
        status = ReservationStatus.COMPLETED;
        completedTimestamp = LocalDateTime.now();
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
