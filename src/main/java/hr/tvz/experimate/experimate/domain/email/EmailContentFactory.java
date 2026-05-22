package hr.tvz.experimate.experimate.domain.email;

import org.springframework.mail.SimpleMailMessage;

/**
 * Builds plain-text email messages for transactional flows.
 *
 * <p>Centralises subject and body construction so {@link EmailService} stays
 * focused on sending. Switching to HTML (v2) means changing only this class.
 */
public class EmailContentFactory {

    private EmailContentFactory() {}

    /**
     * Builds a plain-text email asking the user to verify their email address.
     *
     * @param to        recipient address
     * @param from      sender address ({@code app.mail.from})
     * @param firstName recipient's first name
     * @param link      full verification URL containing the raw token
     * @return ready-to-send {@link SimpleMailMessage}
     */
    public static SimpleMailMessage verification(String to, String from, String firstName, String link) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setFrom(from);
        msg.setSubject("Verify your ExperiMate email address");
        msg.setText("""
                Hi %s,

                Please verify your email address by clicking the link below:

                %s

                The link expires in 24 hours.

                If you did not create an ExperiMate account, you can ignore this email.

                — The ExperiMate team
                """.formatted(firstName, link));
        return msg;
    }

    /**
     * Builds a plain-text email with a password reset link.
     *
     * @param to        recipient address
     * @param from      sender address ({@code app.mail.from})
     * @param firstName recipient's first name
     * @param link      full reset URL containing the raw token
     * @return ready-to-send {@link SimpleMailMessage}
     */
    public static SimpleMailMessage passwordReset(String to, String from, String firstName, String link) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setFrom(from);
        msg.setSubject("Reset your ExperiMate password");
        msg.setText("""
                Hi %s,

                Click the link below to reset your password:

                %s

                The link expires in 1 hour.

                If you did not request a password reset, you can ignore this email.

                — The ExperiMate team
                """.formatted(firstName, link));
        return msg;
    }
}
