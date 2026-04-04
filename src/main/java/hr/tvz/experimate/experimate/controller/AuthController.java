package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.refresh_token.RefreshTokenService;
import hr.tvz.experimate.experimate.model.shared.TokenResponse;
import hr.tvz.experimate.experimate.security.AuthService;
import hr.tvz.experimate.experimate.security.LoginRequest;
import hr.tvz.experimate.experimate.security.MissingCookieException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RequestMapping("/api/auth")
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService,
                          RefreshTokenService refreshService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    ResponseEntity<TokenResponse> authenticate(@RequestBody LoginRequest loginRequest) {
        TokenResponse tokenResponse = authService.login(loginRequest.username(), loginRequest.password());

        ResponseCookie cookie = ResponseCookie.from("refresh_token", tokenResponse.refreshToken())
                .httpOnly(true)
                .secure(false)  //TODO dodaj HTTPS
                .path("/api/auth")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokenResponse);
    }

    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refresh_token");

        return ResponseEntity.ok(
                authService.refreshAccessToken(refreshToken)
        );
    }

    private String extractCookie(HttpServletRequest request, String name) {
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(name))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new MissingCookieException(name));
    }

}
