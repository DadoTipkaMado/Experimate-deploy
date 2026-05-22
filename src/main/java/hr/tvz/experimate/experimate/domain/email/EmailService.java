package hr.tvz.experimate.experimate.domain.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails asynchronously so SMTP latency never blocks a request thread.
 *
 * <p>Mail failures are logged but not rethrown — a failed email must not roll back a
 * completed registration or password reset.
 *
 * <p>Message content is built by {@link EmailContentFactory}. To switch from plain text
 * to HTML (v2), change only that class.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.mail.from}")
    private String mailFrom;

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a verification email containing a link with the raw token.
     *
     * @param to        recipient email address
     * @param firstName recipient's first name
     * @param rawToken  the unhashed token to embed in the link
     */
    @Async
    public void sendVerificationEmail(String to, String firstName, String rawToken) {
        String link = baseUrl + "/verify-email?token=" + rawToken;
        try {
            mailSender.send(EmailContentFactory.verification(to, mailFrom, firstName, link));
            log.info("Verification email sent to {}", to);
        } catch (MailException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Sends a password reset email containing a link with the raw token.
     *
     * @param to        recipient email address
     * @param firstName recipient's first name
     * @param rawToken  the unhashed token to embed in the link
     */
    @Async
    public void sendPasswordResetEmail(String to, String firstName, String rawToken) {
        String link = baseUrl + "/reset-password?token=" + rawToken;
        try {
            mailSender.send(EmailContentFactory.passwordReset(to, mailFrom, firstName, link));
            log.info("Password reset email sent to {}", to);
        } catch (MailException e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }
}
