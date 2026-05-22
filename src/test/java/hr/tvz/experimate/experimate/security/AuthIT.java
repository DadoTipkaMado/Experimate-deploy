package hr.tvz.experimate.experimate.security;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.refresh_token.RefreshTokenRepo;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.UserService;
import hr.tvz.experimate.experimate.domain.user.dto.CreateUserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIT extends AbstractIntegrationTest {

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String LOGOUT_URL = "/api/auth/logout";
    private static final String REFRESH_URL = "/api/auth/refresh";

    @Autowired
    UserService userService;

    @Autowired
    UserRepo userRepo;

    @Autowired
    RefreshTokenRepo refreshTokenRepo;

    @Test
    void login_validCredentials_returns200() {
        createTestUser();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                LOGIN_URL,
                new LoginRequest("dtopic", "123123123123"),
                AuthResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_validCredentials_returnsJWT() {
        createTestUser();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                LOGIN_URL,
                new LoginRequest("dtopic", "123123123123"),
                AuthResponse.class
        );

        assertThat(response.getBody().jwt()).isNotBlank();
    }

    @Test
    void login_validCredentials_setsRefreshCookie() {
        createTestUser();

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                LOGIN_URL,
                new LoginRequest("dtopic", "123123123123"),
                AuthResponse.class
        );

        List<String> cookies = response.getHeaders().get("Set-Cookie");
        assertThat(cookies).anyMatch(cookie -> cookie.contains("refresh_token="));
    }

    @Test
    void logout_authenticatedUser_returns200() {
        Map<String, String> tokens = loginAndGetTokens("dtopic");

        ResponseEntity<Void> response = sendLogoutRequest(tokens.get("refreshToken"), tokens.get("accessToken"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logout_authenticatedUser_clearsCookie() {
        Map<String, String> tokens = loginAndGetTokens("dtopic");

        ResponseEntity<Void> response = sendLogoutRequest(tokens.get("refreshToken"), tokens.get("accessToken"));

        List<String> cookies = response.getHeaders().get("Set-Cookie");
        assertThat(cookies).anyMatch(c -> c.contains("refresh_token=") && c.contains("Max-Age=0"));
    }

    @Test
    void logout_authenticatedUser_deletesTokenFromDatabase() {
        Map<String, String> tokens = loginAndGetTokens("dtopic");

        sendLogoutRequest(tokens.get("refreshToken"), tokens.get("accessToken"));

        assertThat(refreshTokenRepo.findByToken(tokens.get("refreshToken"))).isEmpty();
    }

    @Test
    void refreshAccessToken_validToken_returns200() {
        Map<String, String> tokens = loginAndGetTokens("dtopic");

        ResponseEntity<AuthResponse> response = sendRefreshRequest(tokens.get("refreshToken"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void refreshAccessToken_validToken_returnsNewJWT() {
        Map<String, String> tokens = loginAndGetTokens("dtopic");
        String originalJwt = tokens.get("accessToken");

        ResponseEntity<AuthResponse> response = sendRefreshRequest(tokens.get("refreshToken"));

        assertThat(response.getBody().jwt()).isNotBlank();
        assertThat(response.getBody().jwt()).isNotEqualTo(originalJwt);
    }

    @Test
    void refreshAccessToken_reusedOldToken_returns403() {
        Map<String, String> tokens = loginAndGetTokens("dtopic");
        String oldRefreshToken = tokens.get("refreshToken");

        sendRefreshRequest(oldRefreshToken);

        ResponseEntity<AuthResponse> response = sendRefreshRequest(oldRefreshToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void refreshAccessToken_validToken_rotatesRefreshCookie() {
        Map<String, String> tokens = loginAndGetTokens("dtopic");

        ResponseEntity<AuthResponse> response = sendRefreshRequest(tokens.get("refreshToken"));

        String newRefreshToken = response.getHeaders().get("Set-Cookie").stream()
                .filter(c -> c.startsWith("refresh_token="))
                .map(c -> c.split(";")[0].split("=", 2)[1])
                .findFirst()
                .orElseThrow();

        assertThat(newRefreshToken).isNotEqualTo(tokens.get("refreshToken"));
    }

    private void createTestUser() {
        userService.createUser(new CreateUserDto(
                "David",
                "Topić",
                LocalDate.of(2005, 4, 28),
                "12312312312312312312",
                "MarKoPetrovic.HorVAT@gmaIL.com",
                "dtopic",
                "123123123123",
                null
        ));
        userRepo.findByUsername("dtopic").ifPresent(u -> {
            u.setEmailVerified(true);
            userRepo.save(u);
        });
    }

    private ResponseEntity<Void> sendLogoutRequest(String refreshToken, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.add(HttpHeaders.COOKIE, "refresh_token=" + refreshToken);
        return restTemplate.exchange(LOGOUT_URL, HttpMethod.POST, new HttpEntity<>(headers), Void.class);
    }

    private ResponseEntity<AuthResponse> sendRefreshRequest(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "refresh_token=" + refreshToken);
        return restTemplate.exchange(REFRESH_URL, HttpMethod.POST, new HttpEntity<>(headers), AuthResponse.class);
    }
}
