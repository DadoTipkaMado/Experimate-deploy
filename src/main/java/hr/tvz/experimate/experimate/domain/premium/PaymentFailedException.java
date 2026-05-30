package hr.tvz.experimate.experimate.domain.premium;

/**
 * Thrown by {@link PremiumService} when a {@link PaymentGateway} charge is declined.
 * Mapped to HTTP 402 Payment Required in {@code GlobalExceptionHandler}.
 */
public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }
}
