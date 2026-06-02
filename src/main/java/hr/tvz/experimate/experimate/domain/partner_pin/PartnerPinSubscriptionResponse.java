package hr.tvz.experimate.experimate.domain.partner_pin;

import java.time.LocalDateTime;

/**
 * API response describing the highlighting subscription of a {@link PartnerPin}.
 *
 * <p>{@code cancelAtPeriodEnd} tells the UI whether the subscription will stop at
 * {@code currentPeriodEnd} (cancellation requested) or renew automatically.
 *
 * @param pinId              the subscribed pin
 * @param status             current lifecycle state
 * @param currentPeriodStart start of the current paid billing period
 * @param currentPeriodEnd   end of the current paid period; the pin stays highlighted until then
 * @param cancelAtPeriodEnd  {@code true} if renewal has been cancelled for the end of this period
 */
public record PartnerPinSubscriptionResponse(
        Integer pinId,
        SubscriptionStatus status,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd,
        Boolean cancelAtPeriodEnd
) {}
