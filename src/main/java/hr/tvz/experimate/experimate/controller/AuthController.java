package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.refresh_token.RefreshTokenService;
import hr.tvz.experimate.experimate.model.shared.TokenResponse;
import hr.tvz.experimate.experimate.security.AuthResponse;
import hr.tvz.experimate.experimate.security.AuthService;
import hr.tvz.experimate.experimate.security.LoginRequest;
import hr.tvz.experimate.experimate.security.MissingCookieException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RequestMapping("/api/auth")
@RestController
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService,
                          RefreshTokenService refreshService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> authenticate(@RequestBody LoginRequest loginRequest) {
        TokenResponse tokenResponse = authService.login(loginRequest.username(), loginRequest.password());

        ResponseCookie cookie = buildResponseCookie("refresh_token", tokenResponse.refreshToken());

        log.debug("Returning new JWT.");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(tokenResponse.accessToken()));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String incomingRefreshToken = extractRequestCookie(request, "refresh_token");

        TokenResponse tokenResponse = authService.refreshAccessToken(incomingRefreshToken);
        String outgoingRefreshToken = tokenResponse.refreshToken();
        String outgoingAccessToken = tokenResponse.accessToken();

        ResponseCookie cookie = buildResponseCookie("refresh_token", outgoingRefreshToken);

        log.debug("JWT refreshed: {}. New refresh token: {}",  outgoingAccessToken, outgoingRefreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(
                        outgoingAccessToken
                ));
    }

    private String extractRequestCookie(HttpServletRequest request, String name) {
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new MissingCookieException(name));
    }

    private ResponseCookie buildResponseCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(false)  //TODO dodaj HTTPS
                .path("/api/auth")
                .build();
    }

}
