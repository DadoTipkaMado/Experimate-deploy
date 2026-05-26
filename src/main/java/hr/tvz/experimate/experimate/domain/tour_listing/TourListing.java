package hr.tvz.experimate.experimate.domain.tour_listing;

import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.shared.Constraints;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="tour_listing")
public class TourListing {

    private static final int MINIMUM_DESCRIPTION_LENGTH = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MIN;
    private static final int MAXIMUM_DESCRIPTION_LENGTH = Constraints.TourListingConstraints.TOUR_DESCRIPTION_MAX;

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "host_id")
    private User host;

    //TODO napravi provjeru grada u bazi
    private String city;
    private Double longitude;
    private Double latitude;
    private LocalDateTime postDate;
    private LocalDateTime meetingDate;
    @Column(columnDefinition = "TEXT")
    private String tourDescription;
    private Integer maxGuests;

    private Boolean hostCheckedIn;
    private LocalDateTime hostCheckInTimestamp;
    private LocalDateTime tourStartedAt;

    //For hibernate
    protected TourListing() {
    }

    public TourListing(User host,
                       String city,
                       Double longitude,
                       Double latitude,
                       LocalDateTime meetingDate,
                       String tourDescription,
                       Integer maxGuests) {
        this.host = validateHost(host);
        this.city = validateCity(city);
        this.longitude = longitude;
        this.latitude = latitude;
        this.postDate = LocalDateTime.now();
        this.meetingDate = validateMeetingDate(meetingDate);
        this.tourDescription = validateTourDescription(tourDescription);
        this.maxGuests = validateMaxGuests(maxGuests);
        this.hostCheckedIn = false;
    }

    public Integer getId() {
        return id;
    }

    public User getHost() {
        return host;
    }

    public String getCity() {
        return city;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public LocalDateTime getMeetingDate() {
        return meetingDate;
    }

    public LocalDateTime getPostDate() {
        return postDate;
    }

    public String getTourDescription() {
        return tourDescription;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public Boolean isHostCheckedIn() {
        return hostCheckedIn;
    }

    public LocalDateTime getHostCheckInTimestamp() {
        return hostCheckInTimestamp;
    }

    public LocalDateTime getTourStartedAt() {
        return tourStartedAt;
    }

    /** Returns true when the tour has been explicitly started (auto or manual). */
    public boolean isTourStarted() {
        return tourStartedAt != null;
    }

    /** Marks the host as present; records the timestamp. */
    public void checkHostIn() {
        this.hostCheckedIn = true;
        this.hostCheckInTimestamp = LocalDateTime.now();
    }

    /** Records the moment the tour begins; used as the single source of truth for the ACTIVE transition. */
    public void startTour() {
        this.tourStartedAt = LocalDateTime.now();
    }

    public void setMeetingDate(LocalDateTime meetingDate) {
        this.meetingDate = meetingDate;
    }

    public void setTourDescription(String tourDescription) {
        this.tourDescription = tourDescription;
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
        if(tourDescription.length() < MINIMUM_DESCRIPTION_LENGTH || tourDescription.length() > MAXIMUM_DESCRIPTION_LENGTH)
            throw new IllegalArgumentException("Tour description must be between "
                + MINIMUM_DESCRIPTION_LENGTH + " and " + MAXIMUM_DESCRIPTION_LENGTH + " characters long.");
        return tourDescription;
    }

    private Integer validateMaxGuests(Integer maxGuests) {
        if (maxGuests == null ||
            maxGuests < Constraints.TourListingConstraints.MIN_GUESTS ||
            maxGuests > Constraints.TourListingConstraints.MAX_GUESTS)
            throw new IllegalArgumentException("Max guests must be between "
                + Constraints.TourListingConstraints.MIN_GUESTS + " and "
                + Constraints.TourListingConstraints.MAX_GUESTS + ".");
        return maxGuests;
    }
}
