package hr.tvz.experimate.experimate.shared.exception;

/**
 * Thrown when a mail+password user attempts to log in before verifying their email address.
 *
 * <p>Mapped to HTTP 403 in
 * {@link hr.tvz.experimate.experimate.shared.GlobalExceptionHandler} so the frontend
 * can distinguish "wrong credentials" (401) from "unverified account" (403).
 */
public class EmailNotVerifiedException extends AppAuthException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }
}
