package hr.tvz.experimate.experimate.domain.partner_pin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.Period;

/**
 * A recurring, auto-renewing subscription that keeps a {@link PartnerPin} highlighted on the map.
 *
 * <p>Modelled on how recurring-billing providers (e.g. Stripe) represent a subscription: it owns
 * the current billing period ({@code currentPeriodStart}..{@code currentPeriodEnd}) and a
 * {@code cancelAtPeriodEnd} flag. At the end of each period a scheduler attempts to charge the
 * next period and advance the window; if the subscriber has requested cancellation the
 * subscription instead transitions to {@link SubscriptionStatus#EXPIRED}.
 *
 * <p>Each pin has at most one subscription, enforced by the unique join column. The
 * "highlighted" state shown to clients is <em>derived</em> from this entity via
 * {@link #isHighlightedAt(LocalDateTime)} rather than stored as a separate flag, so it can never
 * drift out of sync with the billing period.
 */
@Entity
@Table(name = "partner_pin_subscription")
public class PartnerPinSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_pin_id", nullable = false, unique = true)
    private PartnerPin partnerPin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private SubscriptionStatus status;

    @Column(name = "current_period_start", nullable = false)
    private LocalDateTime currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDateTime currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private Boolean cancelAtPeriodEnd = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PartnerPinSubscription() {}

    /**
     * Opens a new active subscription whose first billing period starts now.
     *
     * @param partnerPin the pin to highlight
     * @param start      the start of the first billing period (typically {@code now})
     * @param period     the length of a billing period (e.g. one month)
     * @param createdAt  the creation timestamp
     */
    public PartnerPinSubscription(PartnerPin partnerPin, LocalDateTime start, Period period,
                                  LocalDateTime createdAt) {
        this.partnerPin = partnerPin;
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodStart = start;
        this.currentPeriodEnd = start.plus(period);
        this.cancelAtPeriodEnd = false;
        this.createdAt = createdAt;
    }

    /**
     * Whether the pin should be shown as highlighted at the given instant: the subscription is
     * {@link SubscriptionStatus#ACTIVE} and the current paid period has not yet elapsed.
     *
     * @param now the instant to evaluate against
     */
    public boolean isHighlightedAt(LocalDateTime now) {
        return status == SubscriptionStatus.ACTIVE && currentPeriodEnd.isAfter(now);
    }

    /**
     * Advances the billing period by the given length after a successful renewal charge.
     * The new period starts exactly where the previous one ended, so billing stays continuous.
     *
     * @param period the length of the next billing period
     */
    public void renew(Period period) {
        this.currentPeriodStart = this.currentPeriodEnd;
        this.currentPeriodEnd = this.currentPeriodEnd.plus(period);
    }

    /**
     * Restarts an {@link SubscriptionStatus#EXPIRED} or {@link SubscriptionStatus#CANCELED}
     * subscription with a fresh active period after a successful charge.
     *
     * @param start  the start of the new billing period (typically {@code now})
     * @param period the length of the new billing period
     */
    public void reactivate(LocalDateTime start, Period period) {
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodStart = start;
        this.currentPeriodEnd = start.plus(period);
        this.cancelAtPeriodEnd = false;
    }

    /**
     * Marks the subscription to stop renewing at the end of the current paid period.
     * The pin stays highlighted until {@code currentPeriodEnd}, after which the scheduler expires it.
     */
    public void requestCancellation() {
        this.cancelAtPeriodEnd = true;
    }

    /**
     * Clears a pending cancellation, so the subscription will renew normally again.
     */
    public void resumeRenewal() {
        this.cancelAtPeriodEnd = false;
    }

    /**
     * Ends the subscription: the current period has elapsed without renewal.
     */
    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    public Integer getId() { return id; }

    public PartnerPin getPartnerPin() { return partnerPin; }

    public SubscriptionStatus getStatus() { return status; }

    public LocalDateTime getCurrentPeriodStart() { return currentPeriodStart; }

    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }

    public Boolean getCancelAtPeriodEnd() { return cancelAtPeriodEnd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
