package hr.tvz.experimate.experimate.model.shared.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) {
        super(message);
    }
}
