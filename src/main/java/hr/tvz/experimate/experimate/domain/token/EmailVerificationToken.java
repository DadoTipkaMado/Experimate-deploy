package hr.tvz.experimate.experimate.domain.token;

import hr.tvz.experimate.experimate.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * One-time email verification token persisted per user.
 *
 * <p>Only the SHA-256 hash of the raw token is stored — a DB leak cannot be
 * used to claim accounts. The raw value is returned by
 * {@link VerificationTokenService#issueToken} and sent only via email.
 *
 * <p>At most one active token exists per user at the DB level (UNIQUE on {@code user_id}).
 * Issuing a new token deletes the previous one.
 */
@Entity
@Table(name = "email_verification_token")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "token_hash", unique = true, nullable = false)
    private String tokenHash;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "expiration_date_time", nullable = false)
    private LocalDateTime expirationDateTime;

    protected EmailVerificationToken() {}

    public EmailVerificationToken(String tokenHash, User user, LocalDateTime expirationDateTime) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expirationDateTime = expirationDateTime;
    }

    public Integer getId() { return id; }

    public String getTokenHash() { return tokenHash; }

    public User getUser() { return user; }

    public LocalDateTime getExpirationDateTime() { return expirationDateTime; }
}
