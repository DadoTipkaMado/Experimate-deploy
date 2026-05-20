package hr.tvz.experimate.experimate.shared.exception;

/**
 * Base class for application-level authentication and authorisation exceptions.
 *
 * <p>Subclasses represent specific auth failure modes:
 * <ul>
 *   <li>{@link RefreshTokenException} (→ HTTP 403) — refresh token missing, expired, or invalid</li>
 *   <li>{@code InvalidGoogleTokenException} (→ HTTP 401) — Google ID token failed verification</li>
 *   <li>{@code UnverifiedGoogleEmailException} (→ HTTP 401) — Google account email is not verified</li>
 * </ul>
 *
 * <p>Mapped to HTTP 401 by default in
 * {@link hr.tvz.experimate.experimate.shared.GlobalExceptionHandler}.
 * Subclasses with different status codes declare their own {@code @ExceptionHandler} methods
 * and Spring picks the most specific match.
 */
public class AppAuthException extends RuntimeException {

    public AppAuthException(String message) {
        super(message);
    }
}
