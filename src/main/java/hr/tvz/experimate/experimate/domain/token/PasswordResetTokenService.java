package hr.tvz.experimate.experimate.domain.token;

import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.shared.exception.InvalidTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Issues and consumes single-use password reset tokens.
 *
 * <p>The raw token is returned by {@link #issueToken} and must be sent to the user via email.
 * Only the SHA-256 hash is persisted so a DB leak cannot be used to reset arbitrary accounts.
 * At most one active token exists per user — issuing a new one removes the previous.
 */
@Service
public class PasswordResetTokenService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenService.class);

    @Value("${password-reset-token.expiration}")
    private long expirationMs;

    private final PasswordResetTokenRepo repo;

    public PasswordResetTokenService(PasswordResetTokenRepo repo) {
        this.repo = repo;
    }

    /**
     * Generates a new password reset token for the given user, replacing any existing one.
     *
     * @param user the user to issue the token for
     * @return the raw (unhashed) token — must be sent to the user via email, never logged
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String issueToken(User user) {
        String raw = generateToken();
        repo.deleteByUser_Id(user.getId());
        repo.save(new PasswordResetToken(
                sha256Hex(raw),
                user,
                LocalDateTime.now().plusSeconds(expirationMs / 1000)
        ));
        log.debug("Issued password reset token for user {}", user.getId());
        return raw;
    }

    /**
     * Validates and consumes a raw password reset token.
     *
     * @param rawToken the token received from the user (from the email link)
     * @return the {@link User} the token belongs to
     * @throws InvalidTokenException if the token is unknown or has expired
     */
    @Transactional
    public User consumeToken(String rawToken) {
        PasswordResetToken token = repo.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token."));

        // expired token: delete immediately rather than waiting for scheduled cleanup
        if (token.getExpirationDateTime().isBefore(LocalDateTime.now())) {
            repo.delete(token);
            throw new InvalidTokenException("Password reset token has expired.");
        }

        User user = token.getUser();
        repo.delete(token);
        log.debug("Consumed password reset token for user {}", user.getId());
        return user;
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    void cleanupExpiredTokens() {
        repo.deleteAllByExpirationDateTimeBefore(LocalDateTime.now());
        log.debug("Cleaned up expired password reset tokens");
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
