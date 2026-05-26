package hr.tvz.experimate.experimate.domain.partner;

import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventResponse;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for partner account management, consumed by the partner portal frontend
 * ({@code /partner} route, {@code PartnerAPI} in api.js).
 *
 * <p>Endpoints: {@code /apply} (any authenticated user), {@code /profile},
 * {@code /stats}, {@code /events} (all require {@code ROLE_PARTNER}).
 * Ad management is handled separately by {@code PromotedAdController}.
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

    @GetMapping("/events")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<PartnerEventResponse>> getEvents(
            @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(partnerService.getEvents(userDetails.getId()));
    }

}
