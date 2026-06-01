package hr.tvz.experimate.experimate.shared.payment;

/**
 * Port (abstraction boundary) for charging money through a payment provider.
 *
 * <p>This is a provider-agnostic, reusable abstraction: any feature that needs to take a
 * payment (premium purchases, partner pin subscriptions, event advertising, promoted ads)
 * depends only on this interface, never on a concrete provider.
 *
 * <p>To swap in a real provider (e.g. Stripe), implement this interface and register the new
 * class as a Spring bean — no changes required in any calling service. Recurring billing is
 * intentionally <em>not</em> modelled here: a subscription is just repeated {@link #charge}
 * calls driven by the owning domain's scheduler, keeping this port simple and universal.
 */
public interface PaymentGateway {

    /**
     * Attempts to charge the amount described by the given request.
     *
     * @param request the charge to attempt (amount, currency, description)
     * @return the result of the charge attempt
     */
    PaymentResult charge(ChargeRequest request);
}
