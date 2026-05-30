package hr.tvz.experimate.experimate.domain.premium;

/**
 * Result returned by a {@link PaymentGateway} charge attempt.
 *
 * @param success            whether the charge was accepted by the gateway
 * @param transactionReference provider-assigned transaction ID, or a stub reference when using
 *                             {@link StubPaymentGateway}; useful for audit trails in the future
 */
public record PaymentResult(boolean success, String transactionReference) {}
