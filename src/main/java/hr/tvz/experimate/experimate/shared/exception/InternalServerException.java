package hr.tvz.experimate.experimate.shared.exception;

/**
 * Thrown when the server encounters an unexpected internal state that prevents
 * it from fulfilling the request. Results in HTTP 500 Internal Server Error.
 */
public class InternalServerException extends RuntimeException {
    public InternalServerException(String message) {
        super(message);
    }
}
