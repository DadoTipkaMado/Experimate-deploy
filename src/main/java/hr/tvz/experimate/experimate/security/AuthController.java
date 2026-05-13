package hr.tvz.experimate.experimate.security;

import hr.tvz.experimate.experimate.shared.response.TokenResponse;
import hr.tvz.experimate.experimate.security.AuthResponse;
import hr.tvz.experimate.experimate.security.AuthService;
import hr.tvz.experimate.experimate.security.LoginRequest;
import hr.tvz.experimate.experimate.security.MissingCookieException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RequestMapping("/api/auth")
@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    private final String REFRESH_COOKIE = "refresh_token";
    @Value("${refresh-token.expiration}")
    private long refreshTokenExpirationMS;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody LoginRequest loginRequest) {
        TokenResponse tokenResponse = authService.login(loginRequest.username(), loginRequest.password());

        ResponseCookie cookie = buildResponseCookie(REFRESH_COOKIE, tokenResponse.refreshToken(), refreshTokenExpirationMS / 1000);

        log.debug("Returning new JWT.");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(tokenResponse.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        log.debug("Attempting to refresh token.");
        String incomingRefreshToken = extractRequestCookie(request, REFRESH_COOKIE);

        TokenResponse tokenResponse = authService.refreshAccessToken(incomingRefreshToken);
        String outgoingRefreshToken = tokenResponse.refreshToken();
        String outgoingAccessToken = tokenResponse.accessToken();

        ResponseCookie cookie = buildResponseCookie(REFRESH_COOKIE, outgoingRefreshToken, refreshTokenExpirationMS/1000);

        log.debug("JWT refreshed: {}. New refresh token: {}",  outgoingAccessToken, outgoingRefreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(
                        outgoingAccessToken
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String refreshToken = extractRequestCookie(request, REFRESH_COOKIE);
        authService.logout(refreshToken);

        ResponseCookie clearedCookie = buildResponseCookie(REFRESH_COOKIE, "", 0);

        log.info("User logged out.");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .build();
    }

    private String extractRequestCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if(cookies == null) {
            log.debug("No cookies found.");
            throw new MissingCookieException(name);
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> {
                    log.debug("{} cookie not found.", name);
                    return new MissingCookieException(name);
                    });
    }

    private ResponseCookie buildResponseCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)  //TODO dodaj HTTPS
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
