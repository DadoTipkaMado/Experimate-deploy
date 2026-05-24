package hr.tvz.experimate.experimate.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import hr.tvz.experimate.experimate.domain.refresh_token.RefreshTokenService;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.security.JwtService;
import hr.tvz.experimate.experimate.security.google.dto.GoogleLoginRequest;
import hr.tvz.experimate.experimate.security.google.dto.GoogleRegistrationRequest;
import hr.tvz.experimate.experimate.security.google.exception.IncompleteGoogleProfileException;
import hr.tvz.experimate.experimate.security.google.exception.InvalidGoogleTokenException;
import hr.tvz.experimate.experimate.security.google.exception.UnverifiedGoogleEmailException;
import hr.tvz.experimate.experimate.shared.response.TokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private GoogleIdTokenVerifier verifier;
    @Mock
    private UserRepo userRepo;
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private GoogleAuthService service;

    private static final String GOOGLE_SUB  = "google-sub-123";
    private static final String EMAIL       = "test@gmail.com";
    private static final String FIRST_NAME  = "Test";
    private static final String LAST_NAME   = "User";

    /**
     * Builds a mocked {@link GoogleIdToken} with the given claims.
     * The caller is responsible for stubbing {@code verifier.verify()} with the returned token.
     */
    private GoogleIdToken buildMockToken(String sub, String email, String givenName, String familyName, boolean emailVerified) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(sub);
        payload.setEmailVerified(emailVerified);
        payload.set("email", email);
        payload.set("given_name", givenName);
        payload.set("family_name", familyName);

        GoogleIdToken token = mock(GoogleIdToken.class);
        when(token.getPayload()).thenReturn(payload);
        return token;
    }

    private User buildUser(String username) {
        return new User.UserBuilder(FIRST_NAME, LAST_NAME, LocalDate.of(2000, 1, 1),
                "12345678901234567890", EMAIL, username, "password123").build();
    }

    @Test
    void loginOrInitiateRegistration_matchByGoogleSub_returnsTokens() throws Exception {
        User user = buildUser("testuser");
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, FIRST_NAME, LAST_NAME, true);
        when(verifier.verify(anyString())).thenReturn(token);
        when(userRepo.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.of(user));
        when(jwtService.generateToken("testuser")).thenReturn("jwt-token");
        when(refreshTokenService.createOrUpdateRefreshToken(user)).thenReturn("refresh-token");

        GoogleAuthResult result = service.loginOrInitiateRegistration(new GoogleLoginRequest("any-token"));

        assertThat(result.tokens()).isNotNull();
        assertThat(result.tokens().accessToken()).isEqualTo("jwt-token");
        assertThat(result.googleProfile()).isNull();
        verify(userRepo, never()).findByEmail(anyString());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void loginOrInitiateRegistration_foundByEmail_linksGoogleSubAndMarksEmailVerified() throws Exception {
        User user = buildUser("existinguser");
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, FIRST_NAME, LAST_NAME, true);
        when(verifier.verify(anyString())).thenReturn(token);
        when(userRepo.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateToken("existinguser")).thenReturn("jwt-token");
        when(refreshTokenService.createOrUpdateRefreshToken(user)).thenReturn("refresh-token");

        GoogleAuthResult result = service.loginOrInitiateRegistration(new GoogleLoginRequest("any-token"));

        assertThat(result.tokens()).isNotNull();
        assertThat(result.googleProfile()).isNull();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getGoogleSub()).isEqualTo(GOOGLE_SUB);
        assertThat(captor.getValue().isEmailVerified()).isTrue();
    }

    @Test
    void loginOrInitiateRegistration_noMatch_returnsRegistrationRequiredProfile() throws Exception {
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, FIRST_NAME, LAST_NAME, true);
        when(verifier.verify(anyString())).thenReturn(token);
        when(userRepo.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.empty());
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.empty());

        GoogleAuthResult result = service.loginOrInitiateRegistration(new GoogleLoginRequest("any-token"));

        assertThat(result.tokens()).isNull();
        assertThat(result.googleProfile()).isNotNull();
        assertThat(result.googleProfile().email()).isEqualTo(EMAIL);
        assertThat(result.googleProfile().firstName()).isEqualTo(FIRST_NAME);
        assertThat(result.googleProfile().lastName()).isEqualTo(LAST_NAME);
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void loginOrInitiateRegistration_invalidToken_throwsInvalidGoogleTokenException() throws Exception {
        when(verifier.verify(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.loginOrInitiateRegistration(new GoogleLoginRequest("bad-token")))
                .isInstanceOf(InvalidGoogleTokenException.class);
    }

    @Test
    void loginOrInitiateRegistration_emailVerifiedFalse_throwsUnverifiedGoogleEmailException() throws Exception {
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, FIRST_NAME, LAST_NAME, false);
        when(verifier.verify(anyString())).thenReturn(token);

        assertThatThrownBy(() -> service.loginOrInitiateRegistration(new GoogleLoginRequest("any-token")))
                .isInstanceOf(UnverifiedGoogleEmailException.class);
    }

    @Test
    void completeRegistration_userAlreadyExistsByGoogleSub_returnsTokens() throws Exception {
        User user = buildUser("existinguser");
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, FIRST_NAME, LAST_NAME, true);
        when(verifier.verify(anyString())).thenReturn(token);
        when(userRepo.findByGoogleSub(GOOGLE_SUB)).thenReturn(Optional.of(user));
        when(jwtService.generateToken("existinguser")).thenReturn("jwt-token");
        when(refreshTokenService.createOrUpdateRefreshToken(user)).thenReturn("refresh-token");

        TokenResponse result = service.completeRegistration(new GoogleRegistrationRequest(
                "any-token", "existinguser", "password123", LocalDate.of(2000, 1, 1), "12345678901234567890"
        ));

        assertThat(result.accessToken()).isEqualTo("jwt-token");
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void loginOrInitiateRegistration_nullEmailClaim_throwsIncompleteGoogleProfileException() throws Exception {
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, null, FIRST_NAME, LAST_NAME, true);
        when(verifier.verify(anyString())).thenReturn(token);

        assertThatThrownBy(() -> service.loginOrInitiateRegistration(new GoogleLoginRequest("any-token")))
                .isInstanceOf(IncompleteGoogleProfileException.class);
    }

    @Test
    void loginOrInitiateRegistration_nullGivenName_throwsIncompleteGoogleProfileException() throws Exception {
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, null, LAST_NAME, true);
        when(verifier.verify(anyString())).thenReturn(token);

        assertThatThrownBy(() -> service.loginOrInitiateRegistration(new GoogleLoginRequest("any-token")))
                .isInstanceOf(IncompleteGoogleProfileException.class);
    }

    @Test
    void loginOrInitiateRegistration_nullFamilyName_throwsIncompleteGoogleProfileException() throws Exception {
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, FIRST_NAME, null, true);
        when(verifier.verify(anyString())).thenReturn(token);

        assertThatThrownBy(() -> service.loginOrInitiateRegistration(new GoogleLoginRequest("any-token")))
                .isInstanceOf(IncompleteGoogleProfileException.class);
    }
}
