package hr.tvz.experimate.experimate.domain.premium;

import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST controller for premium package purchases.
 *
 * <p>Only regular users ({@code ROLE_USER}) and existing premium users ({@code ROLE_PREMIUM_USER})
 * may purchase. Partners and admins are excluded at the service layer.
 */
@RestController
@RequestMapping("/api/premium")
public class PremiumController {

    private final PremiumService premiumService;

    public PremiumController(PremiumService premiumService) {
        this.premiumService = premiumService;
    }

    /**
     * Purchases a one-time premium package for the authenticated user.
     *
     * <p>If the user already has active premium, the new duration is added on top of
     * the remaining time (extend, not reset).
     *
     * @param principal the authenticated user derived from the JWT
     * @param request   the package to purchase
     * @return 201 Created with the purchased package and new expiry date
     */
    @PostMapping("/purchase")
    @PreAuthorize("hasAnyRole('USER', 'PREMIUM_USER')")
    public ResponseEntity<PurchasePremiumResponse> purchase(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody PurchasePremiumRequest request
    ) {
        LocalDateTime premiumUntil = premiumService.purchase(principal.getId(), request.premiumPackage());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PurchasePremiumResponse(request.premiumPackage(), premiumUntil));
    }
}
