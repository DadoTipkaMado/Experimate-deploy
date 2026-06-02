package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.domain.partner.PartnerProfile;
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
 * A permanent venue marker that a partner places on the map.
 *
 * <p>Each pin belongs to one {@link PartnerProfile} and may have multiple
 * {@link hr.tvz.experimate.experimate.domain.partner_event.PartnerEvent PartnerEvents}
 * attached to it. Partners may also upload a logo image for the pin, stored separately
 * from user profile photos.
 *
 * <p>Soft-deleted via the {@code active} flag so the pin can be hidden without losing
 * its associated events or history.
 */
@Entity
@Table(name = "partner_pin")
public class PartnerPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_profile_id", nullable = false)
    private PartnerProfile partnerProfile;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "logo_filename")
    private String logoFilename;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PartnerPin() {}

    public PartnerPin(PartnerProfile partnerProfile, String name, String description,
                      Double latitude, Double longitude, LocalDateTime createdAt) {
        this.partnerProfile = partnerProfile;
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }

    public PartnerProfile getPartnerProfile() { return partnerProfile; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getLogoFilename() { return logoFilename; }

    public void setLogoFilename(String logoFilename) { this.logoFilename = logoFilename; }

    public Double getLatitude() { return latitude; }

    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }

    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Boolean getActive() { return active; }

    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
