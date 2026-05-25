package hr.tvz.experimate.experimate.domain.partner;

import hr.tvz.experimate.experimate.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Stores business-specific data for users who have been granted the PARTNER role
 * via self-serve onboarding ({@code POST /api/partner/apply}).
 *
 * <p>There is exactly one {@code PartnerProfile} per partner {@link User}. Deleted
 * automatically when the user account is removed (ON DELETE CASCADE in V7 migration).
 *
 * <p>Location and advertising data will live on {@code PartnerPin} in a future iteration —
 * this entity is intentionally kept to company identity and contact info only.
 */
@Entity
@Table(name = "partner_profile")
public class PartnerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    private String website;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PartnerProfile() {}

    public PartnerProfile(User user, String companyName, String contactEmail, String website, LocalDateTime createdAt) {
        this.user = user;
        this.companyName = companyName;
        this.contactEmail = contactEmail;
        this.website = website;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }

    public User getUser() { return user; }

    public String getCompanyName() { return companyName; }

    public String getContactEmail() { return contactEmail; }

    public String getWebsite() { return website; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
