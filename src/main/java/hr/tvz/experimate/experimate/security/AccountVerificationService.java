package hr.tvz.experimate.experimate.security;

import hr.tvz.experimate.experimate.domain.email.EmailService;
import hr.tvz.experimate.experimate.domain.refresh_token.RefreshTokenService;
import hr.tvz.experimate.experimate.domain.token.PasswordResetTokenService;
import hr.tvz.experimate.experimate.domain.token.VerificationTokenService;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.shared.RateLimitOperation;
import hr.tvz.experimate.experimate.shared.RateLimiterService;
import hr.tvz.experimate.experimate.shared.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Orchestrates email verification and password reset flows.
 *
 * <p>Intentionally separate from {@link AuthService} to keep login and token-refresh
 * concerns isolated from account lifecycle operations.
 */
@Service
public class AccountVerificationService {

    private static final Logger log = LoggerFactory.getLogger(AccountVerificationService.class);

    private final UserRepo userRepo;
    private final VerificationTokenService verificationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder encoder;
    private final RateLimiterService rateLimiterService;

    public AccountVerificationService(UserRepo userRepo,
                                      VerificationTokenService verificationTokenService,
                                      PasswordResetTokenService passwordResetTokenService,
                                      EmailService emailService,
                                      RefreshTokenService refreshTokenService,
                                      BCryptPasswordEncoder encoder,
                                      RateLimiterService rateLimiterService) {
        this.userRepo = userRepo;
        this.verificationTokenService = verificationTokenService;
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
        this.encoder = encoder;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Marks the user's email as verified by consuming a valid verification token.
     * Idempotent — silently succeeds if the email is already verified.
     *
     * @param rawToken the token from the verification link
     * @throws hr.tvz.experimate.experimate.shared.exception.InvalidTokenException if the token is unknown or expired
     */
    @Transactional
    public void verifyEmail(String rawToken) {
        User user = verificationTokenService.consumeToken(rawToken);
        if (user.isEmailVerified()) return;
        user.setEmailVerified(true);
        userRepo.save(user);
        log.info("Email verified for user {}", user.getId());
    }

    /**
     * Sends a new verification email if the given address belongs to an unverified
     * mail+password account. Always returns without indicating whether the address exists
     * to prevent account enumeration.
     *
     * @param email the address to resend to (case-insensitive)
     */
    public void resendVerification(String email) {
        rateLimiterService.consume(RateLimitOperation.EMAIL_RESEND, email.toLowerCase());
        userRepo.findByEmail(email.toLowerCase()).ifPresent(user -> {
            // only unverified mail+password accounts need a verification email
            if (user.getGoogleSub() == null && !user.isEmailVerified()) {
                emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(),
                        verificationTokenService.issueToken(user));
            }
        });
    }

    /**
     * Sends a password reset email if the given address belongs to a mail+password account.
     * Always returns without indicating whether the address exists to prevent account enumeration.
     *
     * @param email the address to send the reset link to (case-insensitive)
     */
    public void requestPasswordReset(String email) {
        rateLimiterService.consume(RateLimitOperation.PASSWORD_RESET, email.toLowerCase());
        userRepo.findByEmail(email.toLowerCase()).ifPresent(user -> {
            // Google users have no password to reset
            if (user.getGoogleSub() != null) return;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(),
                    passwordResetTokenService.issueToken(user));
        });
    }

    /**
     * Resets the user's password by consuming a valid reset token, then invalidates
     * any active refresh tokens to force re-authentication with the new password.
     * Also marks the email as verified — a successful reset proves mailbox ownership.
     *
     * @param rawToken    the token from the reset link
     * @param newPassword the new plaintext password (will be encoded before persistence)
     * @throws hr.tvz.experimate.experimate.shared.exception.InvalidTokenException if the token is unknown or expired
     */
    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        User user = passwordResetTokenService.consumeToken(rawToken);
        user.setPassword(encoder.encode(newPassword));
        user.setEmailVerified(true);
        userRepo.save(user);
        refreshTokenService.invalidateAllForUser(user.getId());
        log.info("Password reset confirmed for user {}", user.getId());
    }

    /**
     * Sends a verification email after a new mail+password user is committed to the database.
     *
     * <p>Runs asynchronously after the registration transaction commits, so SMTP latency
     * never delays the registration response and the user FK is guaranteed to exist when
     * the token is persisted.
     *
     * <p>Google users are skipped — their email is already verified via OAuth.
     *
     * @param event published by {@link hr.tvz.experimate.experimate.domain.user.UserService#createUser}
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = userRepo.findById(event.userId())
                .orElseThrow(() -> new UserNotFoundException(event.userId()));

        // Google users are already verified — skip sending
        if (user.getGoogleSub() != null) return;

        String raw = verificationTokenService.issueToken(user);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), raw);
        log.debug("Triggered verification email for user {}", user.getId());
    }
}
