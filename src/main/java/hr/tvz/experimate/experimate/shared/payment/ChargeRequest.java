package hr.tvz.experimate.experimate.shared.payment;

import java.math.BigDecimal;

/**
 * Immutable description of a single charge handed to a {@link PaymentGateway}.
 *
 * <p>Bundling the charge parameters into one value object keeps the gateway port stable:
 * future fields (e.g. an idempotency key, a payer reference, or provider metadata) can be
 * added here without breaking the {@link PaymentGateway#charge} signature or any call site.
 *
 * @param amount      the amount to charge; {@link BigDecimal} is used because floating-point
 *                    types cannot represent decimal currency values exactly
 * @param currency    ISO-4217 currency code (e.g. {@code "EUR"})
 * @param description human-readable charge description forwarded to the provider
 */
public record ChargeRequest(BigDecimal amount, String currency, String description) {}
