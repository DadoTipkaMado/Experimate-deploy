package hr.tvz.experimate.experimate.security.google.exception;

import hr.tvz.experimate.experimate.shared.exception.AppAuthException;

/**
 * Thrown when the Google ID token payload contains {@code email_verified = false}.
 * This can occur for Google Workspace accounts with custom domains.
 * Maps to HTTP 401 via {@link hr.tvz.experimate.experimate.shared.GlobalExceptionHandler}.
 */
public class UnverifiedGoogleEmailException extends AppAuthException {

    public UnverifiedGoogleEmailException() {
        super("Google account email is not verified.");
    }
}
