package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="tour_listing")
public class TourListing {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "host_id")
    private User host;
    private String city;
    private LocalDateTime postDate;
    private LocalDateTime meetingDate;
    @Lob        //unlimited VARCHAR length
    private String tourDescription;
    private boolean reserved;

    //For hibernate
    protected TourListing() {
    }

    public TourListing(User host,
                       String city,
                       LocalDateTime meetingDate,
                       String tourDescription) {
        this.host = validateHost(host);
        this.city = validateCity(city);
        this.postDate = LocalDateTime.now();
        this.meetingDate = validateMeetingDate(meetingDate);
        this.tourDescription = validateTourDescription(tourDescription);

        this.reserved = false;
    }

    public User getHost() {
        return host;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setMeetingDate(LocalDateTime meetingDate) {
        this.meetingDate = meetingDate;
    }

    public void setTourDescription(String tourDescription) {
        this.tourDescription = tourDescription;
    }

    public Integer getId() {
        return id;
    }

    public void setReserved(boolean isReserved){
        this.reserved = isReserved;
    }
    
    private User validateHost(User host){
        if(host==null)
            throw new IllegalArgumentException("User cannot be null");
        return host;
    }

    //TODO napravi u bazi tablicu s gradovima i na temelju toga ce se provjeravati je li grad validan
    private String validateCity(String city){
        if(city==null)
            throw new IllegalArgumentException("City cannot be null");
        return city;
    }

    private LocalDateTime validateMeetingDate(LocalDateTime meetingDate){
        if(meetingDate==null)
            throw new IllegalArgumentException("Meeting date cannot be null");
        if(meetingDate.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Meeting date cannot be before the current date");
        return meetingDate;
    }

    private String validateTourDescription(String tourDescription){
        if(tourDescription==null || tourDescription.isBlank())
            throw new IllegalArgumentException("Tour description cannot be blank");
        if(tourDescription.length() < 20 || tourDescription.length() > 2000)
            throw new IllegalArgumentException("Tour description must be between 200 and 2000 characters long.");
        return tourDescription;
    }
}
