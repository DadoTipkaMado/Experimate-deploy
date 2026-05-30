package hr.tvz.experimate.experimate.domain.premium;

import java.time.LocalDateTime;

/**
 * Response body for a successful premium purchase.
 *
 * @param premiumPackage the package that was purchased
 * @param premiumUntil   the new expiry date of the user's premium access
 */
public record PurchasePremiumResponse(PremiumPackage premiumPackage, LocalDateTime premiumUntil) {}
