package hr.tvz.experimate.experimate.domain.premium;

import java.math.BigDecimal;
import java.time.Period;

/**
 * Available one-time premium packages a user can purchase.
 *
 * <p>Each constant carries its calendar duration and price so the purchase logic
 * can derive the new expiry ({@code now.plus(period)}) and charge the correct amount
 * without any switch/if chains elsewhere.
 *
 * <p>{@link java.time.Period} is used instead of {@link java.time.Duration} because
 * a "month" or "year" is calendar-relative (28–31 days), not a fixed number of seconds.
 * {@link BigDecimal} is used for price because floating-point types cannot represent
 * decimal currency values exactly.
 */
public enum PremiumPackage {

    WEEK (Period.ofWeeks(1),  new BigDecimal("2.99")),
    MONTH(Period.ofMonths(1), new BigDecimal("7.99")),
    YEAR (Period.ofYears(1),  new BigDecimal("59.99"));

    private final Period duration;
    private final BigDecimal price;

    PremiumPackage(Period duration, BigDecimal price) {
        this.duration = duration;
        this.price = price;
    }

    /** @return calendar duration of this package (used to compute the new expiry date). */
    public Period getDuration() {
        return duration;
    }

    /** @return price in EUR for a one-time purchase of this package. */
    public BigDecimal getPrice() {
        return price;
    }
}
