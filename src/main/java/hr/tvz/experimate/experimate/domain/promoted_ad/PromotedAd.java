package hr.tvz.experimate.experimate.domain.promoted_ad;

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
 * A feed advertisement created by a partner, interleaved with {@code TourListing} cards
 * in the public feed at a configurable frequency.
 *
 * <p>{@code activeFrom} and {@code activeUntil} are nullable scheduling boundaries.
 * A null value means "no boundary on that side" (active immediately or indefinitely).
 *
 * <p>{@code viewCount} is a stub always stored as 0. It is reserved for a future
 * impression-tracking iteration and intentionally excluded from all write paths.
 */
@Entity
@Table(name = "promoted_ad")
public class PromotedAd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_profile_id", nullable = false)
    private PartnerProfile partnerProfile;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_filename")
    private String imageFilename;

    @Column(name = "link_url", length = 2048)
    private String linkUrl;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "active_from")
    private LocalDateTime activeFrom;

    @Column(name = "active_until")
    private LocalDateTime activeUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PromotedAd() {}

    public PromotedAd(PartnerProfile partnerProfile, String title, String description,
                      String linkUrl, LocalDateTime activeFrom, LocalDateTime activeUntil,
                      LocalDateTime createdAt) {
        this.partnerProfile = partnerProfile;
        this.title = title;
        this.description = description;
        this.linkUrl = linkUrl;
        this.activeFrom = activeFrom;
        this.activeUntil = activeUntil;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }

    public PartnerProfile getPartnerProfile() { return partnerProfile; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getImageFilename() { return imageFilename; }

    public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }

    public String getLinkUrl() { return linkUrl; }

    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public Boolean getActive() { return active; }

    public void setActive(Boolean active) { this.active = active; }

    public Integer getViewCount() { return viewCount; }

    public LocalDateTime getActiveFrom() { return activeFrom; }

    public void setActiveFrom(LocalDateTime activeFrom) { this.activeFrom = activeFrom; }

    public LocalDateTime getActiveUntil() { return activeUntil; }

    public void setActiveUntil(LocalDateTime activeUntil) { this.activeUntil = activeUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
