package hr.tvz.experimate.experimate.shared.exception;

/**
 * Thrown when a one-time token (email verification or password reset) is unknown,
 * already consumed, or expired.
 *
 * <p>Mapped to HTTP 400 in
 * {@link hr.tvz.experimate.experimate.shared.GlobalExceptionHandler}.
 */
public class InvalidTokenException extends AppAuthException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
