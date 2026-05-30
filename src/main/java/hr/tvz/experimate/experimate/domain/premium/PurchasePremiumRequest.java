package hr.tvz.experimate.experimate.domain.premium;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/premium/purchase}.
 *
 * @param premiumPackage the package the user wants to purchase
 */
public record PurchasePremiumRequest(@NotNull PremiumPackage premiumPackage) {}
