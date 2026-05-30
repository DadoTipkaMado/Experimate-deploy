package hr.tvz.experimate.experimate.domain.premium;

import java.math.BigDecimal;

/**
 * Port (abstraction boundary) for charging a user for a premium package.
 *
 * <p>The service layer depends on this interface, not on any concrete provider.
 * To swap in a real payment provider (e.g. Stripe), implement this interface and
 * register the new class as a Spring bean — no changes required in {@code PremiumService}.
 */
public interface PaymentGateway {

    /**
     * Attempts to charge the given amount.
     *
     * @param amount      the amount to charge
     * @param currency    ISO-4217 currency code (e.g. {@code "EUR"})
     * @param description human-readable charge description forwarded to the provider
     * @return the result of the charge attempt
     */
    PaymentResult charge(BigDecimal amount, String currency, String description);
}
