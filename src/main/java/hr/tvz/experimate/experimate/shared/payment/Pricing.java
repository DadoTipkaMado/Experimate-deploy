package hr.tvz.experimate.experimate.shared.payment;

import java.math.BigDecimal;
import java.time.Period;

/**
 * Single source of truth for every billable amount, period and currency in the application.
 *
 * <p>Centralising these here means a price change is a one-line edit in one file instead of a hunt
 * through several services (and their schedulers). The services and schedulers reference these
 * constants rather than declaring their own.
 *
 * <p>This is a constants holder (a {@code final} class with a private constructor) rather than an
 * {@code enum} because the values are heterogeneous — fixed prices, a calendar {@link Period}, a
 * day count and a currency code — which do not share the single shape an enum constant requires.
 * If prices ever need to be tuned without a redeploy, this is the natural place to switch to
 * {@code @ConfigurationProperties} bound to {@code application.properties}.
 */
public final class Pricing {

    /** ISO-4217 currency code used for every charge. */
    public static final String CURRENCY = "EUR";

    /** Fixed monthly price of a partner pin highlight subscription. */
    public static final BigDecimal PIN_HIGHLIGHT_MONTHLY = new BigDecimal("19.99");

    /**
     * Length of one pin highlight billing period. {@link Period} (calendar-based) is used because
     * a month is 28–31 days, not a fixed number of seconds.
     */
    public static final Period PIN_HIGHLIGHT_PERIOD = Period.ofMonths(1);

    /** Advertising fee per (rounded-up) day of a partner event's duration. */
    public static final BigDecimal EVENT_ADVERTISING_PER_DAY = new BigDecimal("5.00");

    /** Price per (rounded-up) day a promoted ad is displayed in the feed. */
    public static final BigDecimal PROMOTED_AD_PER_DAY = new BigDecimal("3.00");

    /**
     * Days billed for a promoted ad created with no {@code activeUntil} (open-ended display).
     * A finite default is required because an unbounded window cannot be priced; the partner can
     * extend the ad later to pay for more time.
     */
    public static final int PROMOTED_AD_OPEN_ENDED_DAYS = 30;

    private Pricing() {}
}
