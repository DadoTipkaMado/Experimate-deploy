package hr.tvz.experimate.experimate.domain.partner_pin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartnerPinSubscriptionRepository extends JpaRepository<PartnerPinSubscription, Integer> {

    /**
     * Returns the subscription for the given pin, if one exists.
     * Used to subscribe, cancel, or report subscription state for a single pin.
     */
    Optional<PartnerPinSubscription> findByPartnerPin_Id(Integer pinId);

    /**
     * Whether the given pin currently has a highlighting subscription that is active and within
     * its paid period. Used to set the {@code highlighted} flag on a single pin response.
     *
     * @param pinId  the pin to check
     * @param status the status to match (always {@link SubscriptionStatus#ACTIVE})
     * @param now    the instant the period end must still be after
     */
    boolean existsByPartnerPin_IdAndStatusAndCurrentPeriodEndAfter(
            Integer pinId, SubscriptionStatus status, LocalDateTime now);

    /**
     * Returns the IDs of all pins that are currently highlighted (active subscription whose paid
     * period has not elapsed). Fetched once per list request to set the {@code highlighted} flag
     * without an N+1 query per pin.
     *
     * @param status the status to match (always {@link SubscriptionStatus#ACTIVE})
     * @param now    the instant the period end must still be after
     */
    @Query("SELECT s.partnerPin.id FROM PartnerPinSubscription s "
            + "WHERE s.status = :status AND s.currentPeriodEnd > :now")
    List<Integer> findHighlightedPinIds(@Param("status") SubscriptionStatus status,
                                        @Param("now") LocalDateTime now);

    /**
     * Returns all subscriptions in the given status whose current billing period has elapsed.
     * Used by the renewal scheduler to either charge the next period or expire the subscription.
     *
     * @param status the status to match (always {@link SubscriptionStatus#ACTIVE})
     * @param now    the instant the period end must be before
     */
    List<PartnerPinSubscription> findByStatusAndCurrentPeriodEndBefore(
            SubscriptionStatus status, LocalDateTime now);
}
