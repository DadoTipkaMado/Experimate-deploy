package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.domain.token.EmailVerificationToken;
import hr.tvz.experimate.experimate.domain.token.EmailVerificationTokenRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily scheduled job that sends a push notification to users who have not yet
 * verified their email address.
 *
 * <p>A user is considered unverified as long as their {@code email_verification_token}
 * row exists and has not expired. The token is deleted upon successful verification,
 * so this job naturally stops sending reminders once the user verifies.
 *
 * <p>If the user has no active push subscription (e.g. they never opened the PWA),
 * {@link PushNotificationService#sendToUser} is a no-op — no error is thrown.
 */
@Component
public class EmailVerificationReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationReminderScheduler.class);

    private final EmailVerificationTokenRepo emailVerificationTokenRepo;
    private final PushNotificationService pushNotificationService;

    public EmailVerificationReminderScheduler(EmailVerificationTokenRepo emailVerificationTokenRepo,
                                              PushNotificationService pushNotificationService) {
        this.emailVerificationTokenRepo = emailVerificationTokenRepo;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Fires every day at 09:00 and sends a push reminder to each user
     * who still has an active email verification token.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendVerificationReminders() {
        List<EmailVerificationToken> unverified =
                emailVerificationTokenRepo.findAllByExpirationDateTimeAfter(LocalDateTime.now());

        log.info("Sending email verification push reminders to {} user(s)", unverified.size());

        for (EmailVerificationToken token : unverified) {
            pushNotificationService.sendToUser(
                    token.getUser().getId(),
                    "Verify your email",
                    "Confirm your email address to unlock all ExperiMate features.",
                    "/verify-email"
            );
        }
    }
}
