package hr.tvz.experimate.experimate.domain.premium;

import hr.tvz.experimate.experimate.domain.user.Role;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles purchase of one-time premium packages.
 *
 * <p>Eligibility: only {@link Role#USER} and {@link Role#PREMIUM_USER} can purchase.
 * {@link Role#PARTNER} and {@link Role#ADMIN} are rejected because the premium tier
 * is designed exclusively for regular users.
 *
 * <p>Re-purchase (buying while still premium) extends the current expiry rather than
 * resetting it — the new period is added on top of {@code max(now, premiumUntil)},
 * so the user never loses paid time.
 *
 * <p>The actual charge is delegated to {@link PaymentGateway}, which is currently backed
 * by {@link StubPaymentGateway}. Swapping in a real provider requires only a new
 * {@code PaymentGateway} bean — this service does not change.
 */
@Service
public class PremiumService {

    private final UserRepo userRepo;
    private final PaymentGateway paymentGateway;

    public PremiumService(UserRepo userRepo, PaymentGateway paymentGateway) {
        this.userRepo = userRepo;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Purchases a premium package for the given user.
     *
     * <p>Steps: eligibility check → charge via gateway → grant (or extend) premium.
     *
     * @param userId  the ID of the user purchasing the package
     * @param pkg     the package to purchase
     * @return the updated expiry date after the purchase
     * @throws IllegalArgumentException if the user is not found or is not eligible for premium
     * @throws PaymentFailedException   if the gateway declines the charge
     */
    @Transactional
    public LocalDateTime purchase(Integer userId, PremiumPackage pkg) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() != Role.USER && user.getRole() != Role.PREMIUM_USER) {
            throw new IllegalArgumentException("Only regular users can purchase premium");
        }

        PaymentResult result = paymentGateway.charge(pkg.getPrice(), "EUR",
                "ExperiMate Premium — " + pkg.name());

        if (!result.success()) {
            throw new PaymentFailedException("Payment declined for package " + pkg.name());
        }

        LocalDateTime base = (user.getPremiumExpiryDate() != null && user.getPremiumExpiryDate().isAfter(LocalDateTime.now()))
                ? user.getPremiumExpiryDate()
                : LocalDateTime.now();

        user.grantPremium(base.plus(pkg.getDuration()));
        return user.getPremiumExpiryDate();
    }
}
