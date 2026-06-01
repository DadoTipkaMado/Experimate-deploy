package hr.tvz.experimate.experimate.shared.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only endpoint exposing the application's billing rates.
 *
 * <p>Lets the frontend render prices (subscription, event advertising, ad display) from a single
 * backend source instead of duplicating the numbers. Requires authentication like the rest of
 * {@code /api/**}, but no specific role — any signed-in user may read the price list.
 */
@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    /**
     * Returns the current billing rates as defined in {@link Pricing}.
     */
    @GetMapping
    public ResponseEntity<PricingResponse> getPricing() {
        return ResponseEntity.ok(PricingResponse.current());
    }
}
