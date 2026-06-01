package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.domain.partner.PartnerProfile;
import hr.tvz.experimate.experimate.domain.partner.PartnerProfileRepository;
import hr.tvz.experimate.experimate.shared.exception.ConflictException;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.shared.payment.ChargeRequest;
import hr.tvz.experimate.experimate.shared.payment.PaymentFailedException;
import hr.tvz.experimate.experimate.shared.payment.PaymentGateway;
import hr.tvz.experimate.experimate.shared.payment.PaymentResult;
import hr.tvz.experimate.experimate.shared.payment.Pricing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for the recurring "highlight" subscription on a {@link PartnerPin}.
 *
 * <p>Buying a subscription charges the first billing period immediately and opens an
 * {@link SubscriptionStatus#ACTIVE} subscription. Renewal is handled out-of-band by
 * {@link PartnerPinSubscriptionScheduler}, not here. Cancellation follows the cancel-at-period-end
 * convention used by mainstream providers: the pin stays highlighted until the paid period ends.
 *
 * <p>The actual charge is delegated to {@link PaymentGateway} (currently the stub), so swapping in
 * a real provider requires no change to this service.
 */
@Service
public class PartnerPinSubscriptionService {

    private final PartnerPinSubscriptionRepository subscriptionRepository;
    private final PartnerPinRepository partnerPinRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PaymentGateway paymentGateway;

    public PartnerPinSubscriptionService(PartnerPinSubscriptionRepository subscriptionRepository,
                                         PartnerPinRepository partnerPinRepository,
                                         PartnerProfileRepository partnerProfileRepository,
                                         PaymentGateway paymentGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.partnerPinRepository = partnerPinRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Subscribes the given pin to highlighting, or resumes/reactivates an existing subscription.
     *
     * <p>Behaviour by existing state:
     * <ul>
     *   <li>no subscription → charge and open a new active period;</li>
     *   <li>active with a pending cancellation → simply clear the cancellation (no charge), since
     *       the partner has already paid for the current period;</li>
     *   <li>active without cancellation → rejected as already subscribed;</li>
     *   <li>expired/canceled → charge and start a fresh active period.</li>
     * </ul>
     *
     * @param userId the authenticated partner's user ID
     * @param pinId  the pin to highlight
     * @return the resulting subscription state
     * @throws ForbiddenActionException if the user does not own the pin
     * @throws ConflictException        if the pin already has an active, renewing subscription
     * @throws PaymentFailedException   if the gateway declines the charge
     */
    @Transactional
    public PartnerPinSubscriptionResponse subscribe(Integer userId, Integer pinId) {
        PartnerPin pin = findPinOrThrow(pinId);
        checkOwnership(pin, resolveProfile(userId));

        LocalDateTime now = LocalDateTime.now();
        PartnerPinSubscription subscription = subscriptionRepository.findByPartnerPin_Id(pinId)
                .orElse(null);

        if (subscription == null) {
            charge(pin);
            subscription = new PartnerPinSubscription(pin, now, Pricing.PIN_HIGHLIGHT_PERIOD, now);
        } else if (subscription.isHighlightedAt(now)) {
            if (subscription.getCancelAtPeriodEnd()) {
                subscription.resumeRenewal();
            } else {
                throw new ConflictException("Pin already has an active highlight subscription");
            }
        } else {
            charge(pin);
            subscription.reactivate(now, Pricing.PIN_HIGHLIGHT_PERIOD);
        }

        return toResponse(subscriptionRepository.save(subscription));
    }

    /**
     * Requests cancellation of a pin's highlight subscription at the end of the current paid period.
     * The pin stays highlighted until {@code currentPeriodEnd}; the scheduler then expires it.
     *
     * @param userId the authenticated partner's user ID
     * @param pinId  the pin whose subscription to cancel
     * @return the updated subscription state with {@code cancelAtPeriodEnd = true}
     * @throws ForbiddenActionException if the user does not own the pin
     * @throws ConflictException        if there is no active subscription to cancel
     */
    @Transactional
    public PartnerPinSubscriptionResponse cancel(Integer userId, Integer pinId) {
        PartnerPin pin = findPinOrThrow(pinId);
        checkOwnership(pin, resolveProfile(userId));

        PartnerPinSubscription subscription = subscriptionRepository.findByPartnerPin_Id(pinId)
                .filter(s -> s.isHighlightedAt(LocalDateTime.now()))
                .orElseThrow(() -> new ConflictException("Pin has no active highlight subscription"));

        subscription.requestCancellation();
        return toResponse(subscriptionRepository.save(subscription));
    }

    /**
     * Returns the current subscription state for a pin owned by the requesting partner.
     *
     * @param userId the authenticated partner's user ID
     * @param pinId  the pin whose subscription to read
     * @throws ForbiddenActionException if the user does not own the pin
     * @throws PartnerPinSubscriptionNotFoundException if the pin has never been subscribed
     */
    @Transactional(readOnly = true)
    public PartnerPinSubscriptionResponse getSubscription(Integer userId, Integer pinId) {
        PartnerPin pin = findPinOrThrow(pinId);
        checkOwnership(pin, resolveProfile(userId));

        return subscriptionRepository.findByPartnerPin_Id(pinId)
                .map(this::toResponse)
                .orElseThrow(() -> new PartnerPinSubscriptionNotFoundException(pinId));
    }

    private void charge(PartnerPin pin) {
        PaymentResult result = paymentGateway.charge(new ChargeRequest(
                Pricing.PIN_HIGHLIGHT_MONTHLY, Pricing.CURRENCY,
                "ExperiMate pin highlight — " + pin.getName()));
        if (!result.success()) {
            throw new PaymentFailedException("Payment declined for pin highlight subscription");
        }
    }

    private PartnerPin findPinOrThrow(Integer pinId) {
        return partnerPinRepository.findById(pinId)
                .orElseThrow(() -> new PartnerPinNotFoundException(pinId));
    }

    private PartnerProfile resolveProfile(Integer userId) {
        return partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Partner profile not found for user " + userId));
    }

    private void checkOwnership(PartnerPin pin, PartnerProfile profile) {
        if (!pin.getPartnerProfile().getId().equals(profile.getId())) {
            throw new ForbiddenActionException("You do not own this partner pin.");
        }
    }

    private PartnerPinSubscriptionResponse toResponse(PartnerPinSubscription subscription) {
        return new PartnerPinSubscriptionResponse(
                subscription.getPartnerPin().getId(),
                subscription.getStatus(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getCancelAtPeriodEnd()
        );
    }
}
