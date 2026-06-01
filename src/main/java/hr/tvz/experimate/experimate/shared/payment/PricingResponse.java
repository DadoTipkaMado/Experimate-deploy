package hr.tvz.experimate.experimate.shared.payment;

import java.math.BigDecimal;

/**
 * API response exposing the current billing rates so the frontend can display prices without
 * hard-coding them. Every field is sourced from {@link Pricing}, the single source of truth, so a
 * price change in one file is reflected here automatically.
 *
 * @param currency                 ISO-4217 currency code used for every charge
 * @param pinHighlightMonthly       fixed monthly price of a pin highlight subscription
 * @param pinHighlightPeriodMonths  length of one pin highlight billing period, in months
 * @param eventAdvertisingPerDay    advertising fee per day of a partner event's duration
 * @param promotedAdPerDay          price per day a promoted ad is displayed in the feed
 * @param promotedAdOpenEndedDays   days billed for a promoted ad created with no end date
 */
public record PricingResponse(
        String currency,
        BigDecimal pinHighlightMonthly,
        int pinHighlightPeriodMonths,
        BigDecimal eventAdvertisingPerDay,
        BigDecimal promotedAdPerDay,
        int promotedAdOpenEndedDays
) {

    /**
     * Builds the response from the current {@link Pricing} constants.
     */
    public static PricingResponse current() {
        return new PricingResponse(
                Pricing.CURRENCY,
                Pricing.PIN_HIGHLIGHT_MONTHLY,
                Pricing.PIN_HIGHLIGHT_PERIOD.getMonths(),
                Pricing.EVENT_ADVERTISING_PER_DAY,
                Pricing.PROMOTED_AD_PER_DAY,
                Pricing.PROMOTED_AD_OPEN_ENDED_DAYS
        );
    }
}
