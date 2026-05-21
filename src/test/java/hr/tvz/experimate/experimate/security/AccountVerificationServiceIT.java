package hr.tvz.experimate.experimate.security;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.dto.CreateUserDto;
import hr.tvz.experimate.experimate.domain.user.response.UserResponse;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class AccountVerificationServiceIT extends AbstractIntegrationTest {

    @RegisterExtension
    GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private AccountVerificationService accountVerificationService;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private BCryptPasswordEncoder encoder;

    @Test
    void verifyEmail_validToken_setsEmailVerified() {
        restTemplate.postForEntity("/api/user", buildDto("marko.horvat@test.com", "mhorvat"), UserResponse.class);

        await().atMost(5, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length == 1);
        String token = extractToken(GreenMailUtil.getBody(greenMail.getReceivedMessages()[0]));

        accountVerificationService.verifyEmail(token);

        assertThat(userRepo.findByEmail("marko.horvat@test.com").orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void confirmPasswordReset_validToken_changesPasswordAndVerifiesEmail() {
        // Register — verification email arrives async
        restTemplate.postForEntity("/api/user", buildDto("ana.kos@test.com", "akos"), UserResponse.class);
        await().atMost(5, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length == 1);

        // Request password reset — reset email arrives async
        accountVerificationService.requestPasswordReset("ana.kos@test.com");
        await().atMost(5, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length == 2);

        // Reset email is the second message
        String token = extractToken(GreenMailUtil.getBody(greenMail.getReceivedMessages()[1]));

        accountVerificationService.confirmPasswordReset(token, "newPassword1234");

        User user = userRepo.findByEmail("ana.kos@test.com").orElseThrow();
        assertThat(encoder.matches("newPassword1234", user.getPassword())).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Extracts the raw token from a verification or reset email body.
     * Both email types embed the token as the last path segment: {@code ?token=<raw>}.
     */
    private static String extractToken(String body) {
        int idx = body.indexOf("?token=") + "?token=".length();
        // token is URL-safe Base64 — ends at the first whitespace character
        return body.substring(idx).split("\\s+")[0];
    }

    private static CreateUserDto buildDto(String email, String username) {
        return new CreateUserDto(
                "Marko", "Horvat",
                LocalDate.of(2000, 6, 15),
                "98765432109876543",
                email,
                username,
                "password123456",
                null
        );
    }
}
