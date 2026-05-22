package hr.tvz.experimate.experimate.security;

import hr.tvz.experimate.experimate.security.google.GoogleAuthResult;
import hr.tvz.experimate.experimate.security.google.GoogleAuthService;
import hr.tvz.experimate.experimate.security.google.dto.GoogleLoginRequest;
import hr.tvz.experimate.experimate.security.google.dto.GoogleLoginResponse;
import hr.tvz.experimate.experimate.security.google.dto.GoogleRegistrationRequest;
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
    private final GoogleAuthService googleAuthService;
    private final AccountVerificationService accountVerificationService;

    private final String REFRESH_COOKIE = "refresh_token";
    @Value("${refresh-token.expiration}")
    private long refreshTokenExpirationMS;

    public AuthController(AuthService authService,
                          GoogleAuthService googleAuthService,
                          AccountVerificationService accountVerificationService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
        this.accountVerificationService = accountVerificationService;
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

    @PostMapping("/google")
    public ResponseEntity<GoogleLoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest req) {
        GoogleAuthResult result = googleAuthService.loginOrInitiateRegistration(req);
        if (result.tokens() != null) {
            ResponseCookie cookie = buildResponseCookie(REFRESH_COOKIE, result.tokens().refreshToken(), refreshTokenExpirationMS / 1000);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(GoogleLoginResponse.authenticated(result.tokens().accessToken()));
        }
        return ResponseEntity.ok(GoogleLoginResponse.registrationRequired(result.googleProfile()));
    }

    @PostMapping("/google/complete-registration")
    public ResponseEntity<AuthResponse> googleCompleteRegistration(@Valid @RequestBody GoogleRegistrationRequest req) {
        TokenResponse tokenResponse = googleAuthService.completeRegistration(req);
        ResponseCookie cookie = buildResponseCookie(REFRESH_COOKIE, tokenResponse.refreshToken(), refreshTokenExpirationMS / 1000);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(tokenResponse.accessToken()));
    }

    /**
     * Marks the caller's email address as verified by consuming a one-time token.
     *
     * @param req contains the raw token from the verification link
     * @return {@code 204 No Content} on success; {@code 400} if the token is invalid or expired
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        accountVerificationService.verifyEmail(req.token());
        return ResponseEntity.noContent().build();
    }

    /**
     * Sends a fresh verification email if the address belongs to an unverified mail+password account.
     * Always returns {@code 202} regardless of whether the address exists, to prevent enumeration.
     *
     * @param req contains the email address to resend to
     * @return {@code 202 Accepted}
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest req) {
        accountVerificationService.resendVerification(req.email());
        return ResponseEntity.accepted().build();
    }

    /**
     * Sends a password reset email if the address belongs to a mail+password account.
     * Always returns {@code 202} regardless of whether the address exists, to prevent enumeration.
     *
     * @param req contains the email address to send the reset link to
     * @return {@code 202 Accepted}
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest req) {
        accountVerificationService.requestPasswordReset(req.email());
        return ResponseEntity.accepted().build();
    }

    /**
     * Resets the caller's password by consuming a one-time reset token.
     * Invalidates all active refresh tokens so the old password cannot resume a session.
     *
     * @param req contains the raw token from the reset link and the new plaintext password
     * @return {@code 204 No Content} on success; {@code 400} if the token is invalid or expired
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        accountVerificationService.confirmPasswordReset(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
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
                .secure(true)
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }
}
