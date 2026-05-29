package hr.tvz.experimate.experimate.domain.partner_event;

import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A time-bound event attached to a {@link PartnerPin}.
 *
 * <p>Events are immediately public to all authenticated users once created.
 * An ordinary user can click an event to pre-fill a {@code TourListing} with
 * the event's datetime and the pin's coordinates.
 *
 * <p>Location is inherited from the parent pin — {@code PartnerEvent} itself
 * stores no coordinates. When a user creates a listing from an event they may
 * override both the datetime and location.
 */
@Entity
@Table(name = "partner_event")
public class PartnerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_pin_id", nullable = false)
    private PartnerPin partnerPin;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "ticket_vendor_url", length = 2048)
    private String ticketVendorUrl;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDatetime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PartnerEvent() {}

    public PartnerEvent(PartnerPin partnerPin, String title, String description,
                        String ticketVendorUrl, LocalDateTime startDatetime,
                        LocalDateTime endDatetime, LocalDateTime createdAt) {
        this.partnerPin = partnerPin;
        this.title = title;
        this.description = description;
        this.ticketVendorUrl = ticketVendorUrl;
        this.startDatetime = startDatetime;
        this.endDatetime = endDatetime;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }

    public PartnerPin getPartnerPin() { return partnerPin; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getTicketVendorUrl() { return ticketVendorUrl; }

    public void setTicketVendorUrl(String ticketVendorUrl) { this.ticketVendorUrl = ticketVendorUrl; }

    public LocalDateTime getStartDatetime() { return startDatetime; }

    public void setStartDatetime(LocalDateTime startDatetime) { this.startDatetime = startDatetime; }

    public LocalDateTime getEndDatetime() { return endDatetime; }

    public void setEndDatetime(LocalDateTime endDatetime) { this.endDatetime = endDatetime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
