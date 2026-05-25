package hr.tvz.experimate.experimate.domain.partner;

import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for partner account management, consumed by the partner portal frontend
 * ({@code /partner} route, {@code PartnerAPI} in api.js).
 *
 * <p>All endpoints except {@code /apply} require {@code ROLE_PARTNER}. The {@code /apply}
 * endpoint is open to any authenticated user so they can self-serve upgrade their account.
 */
@RestController
@RequestMapping("/api/partner")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @PostMapping("/apply")
    public ResponseEntity<PartnerProfileResponse> apply(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody ApplyPartnerRequest dto) {
        PartnerProfileResponse response = partnerService.apply(userDetails.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PartnerProfileResponse> getProfile(
            @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(partnerService.getProfile(userDetails.getId()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PartnerStatsResponse> getStats(
            @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(partnerService.getStats(userDetails.getId()));
    }

    @GetMapping("/listings")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<TourListingResponse>> getListings(
            @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(partnerService.getListings(userDetails.getId()));
    }

    /**
     * Stub endpoint for ad management. Returns a placeholder response until the
     * PartnerPin and ad schema are defined in a future iteration.
     */
    @PutMapping("/ad")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<Map<String, String>> updateAd(
            @AuthenticationPrincipal AppUserDetails userDetails) {
        // TODO: implement once PartnerPin/PartnerEvent ad schema is defined
        return ResponseEntity.ok(Map.of("status", "not_implemented_yet"));
    }
}
