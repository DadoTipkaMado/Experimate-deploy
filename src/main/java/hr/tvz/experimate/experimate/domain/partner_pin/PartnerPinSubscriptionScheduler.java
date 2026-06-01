package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.shared.payment.ChargeRequest;
import hr.tvz.experimate.experimate.shared.payment.PaymentGateway;
import hr.tvz.experimate.experimate.shared.payment.PaymentResult;
import hr.tvz.experimate.experimate.shared.payment.Pricing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily job that drives recurring billing for {@link PartnerPinSubscription}s.
 *
 * <p>For every {@link SubscriptionStatus#ACTIVE} subscription whose current period has elapsed:
 * <ul>
 *   <li>if the subscriber requested cancellation, the subscription is expired;</li>
 *   <li>otherwise the next period is charged — on success the billing window advances, on a
 *       declined charge the subscription is expired (no retry/grace period in this stub).</li>
 * </ul>
 *
 * <p>{@code @Transactional} is required: subscriptions loaded here are managed by Hibernate, so the
 * state changes are flushed automatically on commit via dirty checking — no explicit save needed.
 *
 * <p>Pricing and billing period come from {@link Pricing}, the single source shared with
 * {@link PartnerPinSubscriptionService}.
 */
@Component
public class PartnerPinSubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PartnerPinSubscriptionScheduler.class);

    private final PartnerPinSubscriptionRepository subscriptionRepository;
    private final PaymentGateway paymentGateway;

    public PartnerPinSubscriptionScheduler(PartnerPinSubscriptionRepository subscriptionRepository,
                                           PaymentGateway paymentGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Fires every day at 03:00 and renews or expires all due pin highlight subscriptions.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void processDueSubscriptions() {
        List<PartnerPinSubscription> due = subscriptionRepository
                .findByStatusAndCurrentPeriodEndBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());

        log.info("Processing {} due pin highlight subscription(s)", due.size());

        for (PartnerPinSubscription subscription : due) {
            if (subscription.getCancelAtPeriodEnd()) {
                subscription.expire();
                continue;
            }

            PaymentResult result = paymentGateway.charge(new ChargeRequest(
                    Pricing.PIN_HIGHLIGHT_MONTHLY, Pricing.CURRENCY,
                    "ExperiMate pin highlight renewal — " + subscription.getPartnerPin().getName()));

            if (result.success()) {
                subscription.renew(Pricing.PIN_HIGHLIGHT_PERIOD);
            } else {
                subscription.expire();
            }
        }
    }
}
