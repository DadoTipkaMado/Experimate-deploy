package hr.tvz.experimate.experimate.security.google.exception;

/**
 * Thrown when the Google ID token payload is missing required profile fields
 * ({@code email}, {@code given_name}, or {@code family_name}).
 * Maps to HTTP 400 via {@link hr.tvz.experimate.experimate.shared.GlobalExceptionHandler}.
 *
 * <p>Distinct from {@link InvalidGoogleTokenException} — the token itself is valid;
 * the payload simply does not contain the claims needed to proceed.
 */
public class IncompleteGoogleProfileException extends RuntimeException {

    public IncompleteGoogleProfileException() {
        super("Google ID token payload is missing required profile fields (email, given_name, or family_name).");
    }
}
