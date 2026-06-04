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
import hr.tvz.experimate.experimate.shared.exception.InvalidTokenException;
import hr.tvz.experimate.experimate.shared.exception.RateLimitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountVerificationServiceTest {

    @Mock private UserRepo userRepo;
    @Mock private VerificationTokenService verificationTokenService;
    @Mock private PasswordResetTokenService passwordResetTokenService;
    @Mock private EmailService emailService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private BCryptPasswordEncoder encoder;
    @Mock private RateLimiterService rateLimiterService;

    @InjectMocks
    private AccountVerificationService service;

    // ── onUserRegistered ─────────────────────────────────────────────────────

    @Test
    void onUserRegistered_mailUserReceivesVerificationEmail() {
        User user = buildUser();
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(verificationTokenService.issueToken(user)).thenReturn("raw-token");

        service.onUserRegistered(new UserRegisteredEvent(1, user.getEmail(), user.getFirstName()));

        verify(verificationTokenService).issueToken(user);
        verify(emailService).sendVerificationEmail(user.getEmail(), user.getFirstName(), "raw-token");
    }

    @Test
    void onUserRegistered_googleUserSkipsVerificationEmail() {
        User user = buildUser();
        user.setGoogleSub("google-sub-123");
        when(userRepo.findById(1)).thenReturn(Optional.of(user));

        service.onUserRegistered(new UserRegisteredEvent(1, user.getEmail(), user.getFirstName()));

        verifyNoInteractions(verificationTokenService);
        verifyNoInteractions(emailService);
    }

    @Test
    void onUserRegistered_userNotFoundThrowsUserNotFoundException() {
        when(userRepo.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.onUserRegistered(new UserRegisteredEvent(99, "test@test.com", "Test")))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ── verifyEmail ──────────────────────────────────────────────────────────

    @Test
    void verifyEmail_validToken_setsEmailVerifiedAndSaves() {
        User user = buildUser();
        when(verificationTokenService.consumeToken("raw-token")).thenReturn(user);

        service.verifyEmail("raw-token");

        assertThat(user.isEmailVerified()).isTrue();
        verify(userRepo).save(user);
    }

    @Test
    void verifyEmail_alreadyVerified_doesNotSave() {
        User user = buildUser();
        user.setEmailVerified(true);
        when(verificationTokenService.consumeToken("raw-token")).thenReturn(user);

        service.verifyEmail("raw-token");

        verify(userRepo, never()).save(any());
    }

    @Test
    void verifyEmail_invalidToken_throwsInvalidTokenException() {
        when(verificationTokenService.consumeToken("bad-token"))
                .thenThrow(new InvalidTokenException("Invalid or expired verification token."));

        assertThatThrownBy(() -> service.verifyEmail("bad-token"))
                .isInstanceOf(InvalidTokenException.class);

        verify(userRepo, never()).save(any());
    }

    // ── resendVerification ───────────────────────────────────────────────────

    @Test
    void resendVerification_unverifiedMailUser_sendsEmail() {
        User user = buildUser();
        when(userRepo.findByEmail("marko@test.com")).thenReturn(Optional.of(user));
        when(verificationTokenService.issueToken(user)).thenReturn("raw-token");

        service.resendVerification("marko@test.com");

        verify(emailService).sendVerificationEmail(user.getEmail(), user.getFirstName(), "raw-token");
    }

    @Test
    void resendVerification_alreadyVerifiedUser_doesNotSendEmail() {
        User user = buildUser();
        user.setEmailVerified(true);
        when(userRepo.findByEmail("marko@test.com")).thenReturn(Optional.of(user));

        service.resendVerification("marko@test.com");

        verifyNoInteractions(emailService);
    }

    @Test
    void resendVerification_googleUser_doesNotSendEmail() {
        User user = buildUser();
        user.setGoogleSub("google-sub-123");
        when(userRepo.findByEmail("marko@test.com")).thenReturn(Optional.of(user));

        service.resendVerification("marko@test.com");

        verifyNoInteractions(emailService);
    }

    @Test
    void resendVerification_unknownEmail_doesNotSendEmail() {
        when(userRepo.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        service.resendVerification("unknown@test.com");

        verifyNoInteractions(emailService);
    }

    @Test
    void resendVerification_rateLimitExceeded_throwsRateLimitException() {
        doThrow(new RateLimitException("Rate limit exceeded."))
                .when(rateLimiterService).consume(RateLimitOperation.EMAIL_RESEND, "marko@test.com");

        assertThatThrownBy(() -> service.resendVerification("marko@test.com"))
                .isInstanceOf(RateLimitException.class);

        verifyNoInteractions(userRepo);
        verifyNoInteractions(emailService);
    }

    // ── requestPasswordReset ─────────────────────────────────────────────────

    @Test
    void requestPasswordReset_mailUser_sendsResetEmail() {
        User user = buildUser();
        when(userRepo.findByEmail("marko@test.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenService.issueToken(user)).thenReturn("reset-token");

        service.requestPasswordReset("marko@test.com");

        verify(emailService).sendPasswordResetEmail(user.getEmail(), user.getFirstName(), "reset-token");
    }

    @Test
    void requestPasswordReset_googleUser_doesNotSendEmail() {
        User user = buildUser();
        user.setGoogleSub("google-sub-123");
        when(userRepo.findByEmail("marko@test.com")).thenReturn(Optional.of(user));

        service.requestPasswordReset("marko@test.com");

        verifyNoInteractions(emailService);
    }

    @Test
    void requestPasswordReset_unknownEmail_doesNotSendEmail() {
        when(userRepo.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("unknown@test.com");

        verifyNoInteractions(emailService);
    }

    @Test
    void requestPasswordReset_rateLimitExceeded_throwsRateLimitException() {
        doThrow(new RateLimitException("Rate limit exceeded."))
                .when(rateLimiterService).consume(RateLimitOperation.PASSWORD_RESET, "marko@test.com");

        assertThatThrownBy(() -> service.requestPasswordReset("marko@test.com"))
                .isInstanceOf(RateLimitException.class);

        verifyNoInteractions(userRepo);
        verifyNoInteractions(emailService);
    }

    // ── confirmPasswordReset ─────────────────────────────────────────────────

    @Test
    void confirmPasswordReset_validToken_encodesPasswordAndInvalidatesToken() {
        User user = buildUser();
        when(passwordResetTokenService.consumeToken("reset-token")).thenReturn(user);
        when(encoder.encode("newpassword")).thenReturn("encoded-newpassword");

        service.confirmPasswordReset("reset-token", "newpassword");

        assertThat(user.getPassword()).isEqualTo("encoded-newpassword");
        assertThat(user.isEmailVerified()).isTrue();
        verify(userRepo).save(user);
        verify(refreshTokenService).invalidateAllForUser(any());
    }

    @Test
    void confirmPasswordReset_invalidToken_throwsAndNeverSaves() {
        when(passwordResetTokenService.consumeToken("bad-token"))
                .thenThrow(new InvalidTokenException("Invalid or expired reset token."));

        assertThatThrownBy(() -> service.confirmPasswordReset("bad-token", "newpassword"))
                .isInstanceOf(InvalidTokenException.class);

        verify(userRepo, never()).save(any());
        verifyNoInteractions(refreshTokenService);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User buildUser() {
        return new User.UserBuilder(
                "Marko", "Marić",
                LocalDate.of(2000, 1, 1),
                "12345678901234567",
                "marko@test.com",
                "mmaric",
                "hashed_password"
        ).build();
    }
}
