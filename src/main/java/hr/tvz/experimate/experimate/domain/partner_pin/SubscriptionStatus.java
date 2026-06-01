package hr.tvz.experimate.experimate.domain.partner_pin;

/**
 * Lifecycle state of a {@link PartnerPinSubscription}.
 *
 * <p>Mirrors the minimal subset of states used by recurring-billing providers (e.g. Stripe):
 * <ul>
 *   <li>{@link #ACTIVE} — the pin is highlighted; the scheduler will attempt to renew it at
 *       the end of the current billing period.</li>
 *   <li>{@link #CANCELED} — the subscriber asked to stop renewals and the paid period has
 *       already elapsed; reserved for an explicit immediate cancellation path. With the
 *       cancel-at-period-end flow, an {@link #ACTIVE} subscription transitions straight to
 *       {@link #EXPIRED} once its period ends.</li>
 *   <li>{@link #EXPIRED} — the billing period ended without renewal (cancellation took effect
 *       or a renewal charge was declined); the pin is no longer highlighted.</li>
 * </ul>
 */
public enum SubscriptionStatus {
    ACTIVE,
    CANCELED,
    EXPIRED
}
