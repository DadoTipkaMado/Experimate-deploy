package hr.tvz.experimate.experimate.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.dto.CreateUserDto;
import hr.tvz.experimate.experimate.security.AuthResponse;
import hr.tvz.experimate.experimate.security.google.dto.GoogleLoginRequest;
import hr.tvz.experimate.experimate.security.google.dto.GoogleRegistrationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleAuthIT extends AbstractIntegrationTest {

    @MockitoBean
    GoogleIdTokenVerifier googleIdTokenVerifier;

    @Autowired
    UserRepo userRepo;

    private static final String GOOGLE_URL   = "/api/auth/google";
    private static final String COMPLETE_URL = "/api/auth/google/complete-registration";
    private static final String GOOGLE_SUB   = "google-sub-test-123";
    private static final String EMAIL        = "google@gmail.com";
    private static final String FIRST_NAME   = "Google";
    private static final String LAST_NAME    = "User";

    private record GoogleLoginTestResponse(
            String status,
            String accessToken,
            GoogleProfileTestResponse googleProfile) {}

    private record GoogleProfileTestResponse(
            String firstName,
            String lastName,
            String email) {}

    /**
     * Builds a mocked {@link GoogleIdToken} with the given subject, email, and email verification state.
     * The caller is responsible for stubbing the verifier with the returned token.
     */
    private GoogleIdToken buildMockToken(String sub, String email, boolean emailVerified) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(sub);
        payload.setEmailVerified(emailVerified);
        payload.set("email", email);
        payload.set("given_name", FIRST_NAME);
        payload.set("family_name", LAST_NAME);

        GoogleIdToken mockToken = mock(GoogleIdToken.class);
        when(mockToken.getPayload()).thenReturn(payload);
        return mockToken;
    }

    private GoogleRegistrationRequest registrationRequestWithUsername(String username) {
        return new GoogleRegistrationRequest(
                "any-token",
                username,
                "password123",
                LocalDate.of(2000, 1, 1),
                "12345678901234567890"
        );
    }

    @Test
    void googleLogin_existingUserByGoogleSub_returnsLoggedIn() throws Exception {
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, true);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(token);
        restTemplate.postForEntity(COMPLETE_URL, registrationRequestWithUsername("googleuser"), Void.class);

        ResponseEntity<GoogleLoginTestResponse> response = restTemplate.postForEntity(
                GOOGLE_URL, new GoogleLoginRequest("any-token"), GoogleLoginTestResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo("LOGGED_IN");
        assertThat(response.getBody().accessToken()).isNotNull();
        assertThat(response.getHeaders().getFirst("Set-Cookie")).contains("refresh_token");
    }

    @Test
    void googleLogin_existingUserByEmail_linksGoogleSubAndReturnsLoggedIn() throws Exception {
        restTemplate.postForEntity("/api/user", new CreateUserDto(
                "Existing", "User", LocalDate.of(2000, 1, 1),
                "99999999999999999999", EMAIL, "existinguser", "password123", null
        ), Void.class);
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, true);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(token);

        ResponseEntity<GoogleLoginTestResponse> response = restTemplate.postForEntity(
                GOOGLE_URL, new GoogleLoginRequest("any-token"), GoogleLoginTestResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo("LOGGED_IN");
        assertThat(userRepo.findByEmail(EMAIL).get().getGoogleSub()).isEqualTo(GOOGLE_SUB);
    }

    @Test
    void googleLogin_existingUserWithUnverifiedEmail_linksGoogleSubAndSetsEmailVerified() throws Exception {
        restTemplate.postForEntity("/api/user", new CreateUserDto(
                "Existing", "User", LocalDate.of(2000, 1, 1),
                "99999999999999999999", EMAIL, "existinguser", "password123", null
        ), Void.class);
        assertThat(userRepo.findByEmail(EMAIL).get().isEmailVerified()).isFalse();
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, true);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(token);

        ResponseEntity<GoogleLoginTestResponse> response = restTemplate.postForEntity(
                GOOGLE_URL, new GoogleLoginRequest("any-token"), GoogleLoginTestResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo("LOGGED_IN");
        assertThat(userRepo.findByEmail(EMAIL).get().getGoogleSub()).isEqualTo(GOOGLE_SUB);
        assertThat(userRepo.findByEmail(EMAIL).get().isEmailVerified()).isTrue();
    }

    @Test
    void googleLogin_noExistingUser_returnsRegistrationRequired() throws Exception {
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, true);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(token);

        ResponseEntity<GoogleLoginTestResponse> response = restTemplate.postForEntity(
                GOOGLE_URL, new GoogleLoginRequest("any-token"), GoogleLoginTestResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo("REGISTRATION_REQUIRED");
        assertThat(response.getBody().googleProfile().email()).isEqualTo(EMAIL);
        assertThat(userRepo.findByGoogleSub(GOOGLE_SUB)).isEmpty();
    }

    @Test
    void googleLogin_invalidToken_returns401() throws Exception {
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(null);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                GOOGLE_URL, new GoogleLoginRequest("invalid-token"), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void googleLogin_unverifiedEmail_returns401() throws Exception {
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, false);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(token);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                GOOGLE_URL, new GoogleLoginRequest("any-token"), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void completeRegistration_validRequest_createsUserAndReturnsTokens() throws Exception {
        GoogleIdToken token = buildMockToken(GOOGLE_SUB, EMAIL, true);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(token);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                COMPLETE_URL, registrationRequestWithUsername("googleuser"), AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().jwt()).isNotNull();
        assertThat(response.getHeaders().getFirst("Set-Cookie")).contains("refresh_token");
        assertThat(userRepo.findByGoogleSub(GOOGLE_SUB)).isPresent();
        assertThat(userRepo.findByEmail(EMAIL).get().isEmailVerified()).isTrue();
    }

    @Test
    void completeRegistration_takenUsername_returns409() throws Exception {
        GoogleIdToken firstToken = buildMockToken(GOOGLE_SUB, EMAIL, true);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(firstToken);
        restTemplate.postForEntity(COMPLETE_URL, registrationRequestWithUsername("googleuser"), Void.class);

        GoogleIdToken secondToken = buildMockToken("other-sub-456", "other@gmail.com", true);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(secondToken);
        ResponseEntity<Void> response = restTemplate.postForEntity(
                COMPLETE_URL, registrationRequestWithUsername("googleuser"), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void completeRegistration_takenEmail_returns409() throws Exception {
        restTemplate.postForEntity("/api/user", new CreateUserDto(
                "Existing", "User", LocalDate.of(2000, 1, 1),
                "99999999999999999999", EMAIL, "existinguser", "password123", null
        ), Void.class);
        GoogleIdToken token = buildMockToken("new-sub-456", EMAIL, true);
        when(googleIdTokenVerifier.verify(anyString())).thenReturn(token);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                COMPLETE_URL, registrationRequestWithUsername("newgoogleuser"), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
