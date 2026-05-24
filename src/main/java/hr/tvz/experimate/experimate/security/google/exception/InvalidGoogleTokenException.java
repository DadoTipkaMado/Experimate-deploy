package hr.tvz.experimate.experimate.security.google.exception;

import hr.tvz.experimate.experimate.shared.exception.AppAuthException;

/**
 * Thrown when a Google ID token fails signature verification, is expired, or is malformed.
 * Maps to HTTP 401 via {@link hr.tvz.experimate.experimate.shared.GlobalExceptionHandler}.
 */
public class InvalidGoogleTokenException extends AppAuthException {

    public InvalidGoogleTokenException() {
        super("Invalid or expired Google ID token.");
    }
}
