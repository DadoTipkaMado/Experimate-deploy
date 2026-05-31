package hr.tvz.experimate.experimate.domain.partner_event;

import hr.tvz.experimate.experimate.domain.promoted_ad.PromoteEventRequest;
import hr.tvz.experimate.experimate.domain.promoted_ad.PromotedAdResponse;
import hr.tvz.experimate.experimate.domain.promoted_ad.PromotedAdService;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for partner events.
 *
 * <p>Events are nested under their parent pin for creation and listing
 * ({@code POST /api/partner-pins/{pinId}/events} and
 * {@code GET /api/partner-pins/{pinId}/events}).
 * Individual event access and mutations use the flat {@code /api/partner-events/{id}} path.
 * {@code GET /api/partner-events/upcoming} returns a paginated feed of all future events
 * across every partner — accessible to any authenticated user.
 *
 * <p>Read endpoints are accessible to all authenticated users.
 * Write operations require {@code ROLE_PARTNER} and ownership of the parent pin.
 */
@RestController
public class PartnerEventController {

    private final PartnerEventService partnerEventService;
    private final PromotedAdService promotedAdService;

    public PartnerEventController(PartnerEventService partnerEventService,
                                  PromotedAdService promotedAdService) {
        this.partnerEventService = partnerEventService;
        this.promotedAdService = promotedAdService;
    }

    @PostMapping("/api/partner-pins/{pinId}/events")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PartnerEventResponse> createEvent(
            @PathVariable Integer pinId,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody CreatePartnerEventRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(partnerEventService.createEvent(pinId, userDetails.getId(), req));
    }

    @GetMapping("/api/partner-pins/{pinId}/events")
    public ResponseEntity<List<PartnerEventResponse>> getEventsForPin(@PathVariable Integer pinId) {
        return ResponseEntity.ok(partnerEventService.getEventsForPin(pinId));
    }

    @GetMapping("/api/partner-events/upcoming")
    public ResponseEntity<Page<PartnerEventResponse>> getUpcomingEvents(
            @PageableDefault(size = 20, sort = "startDatetime", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ResponseEntity.ok(partnerEventService.findUpcoming(pageable));
    }

    @GetMapping("/api/partner-events/{id}")
    public ResponseEntity<PartnerEventResponse> getEventById(@PathVariable Integer id) {
        return ResponseEntity.ok(partnerEventService.getEventById(id));
    }

    @PutMapping("/api/partner-events/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PartnerEventResponse> updateEvent(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody UpdatePartnerEventRequest req) {
        return ResponseEntity.ok(partnerEventService.updateEvent(id, userDetails.getId(), req));
    }

    @DeleteMapping("/api/partner-events/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails) {
        partnerEventService.deleteEvent(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Promotes this event into the public feed as a sponsored ad.
     * Send an empty object {@code {}} to promote with no overrides.
     * Requires {@code ROLE_PARTNER} and ownership of the event's pin.
     */
    @PostMapping("/api/partner-events/{id}/promote")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PromotedAdResponse> promoteEvent(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestBody PromoteEventRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotedAdService.promoteEvent(userDetails.getId(), id, req));
    }

    /**
     * Removes this event's promotion from the feed.
     * Requires {@code ROLE_PARTNER} and ownership of the event's pin.
     */
    @DeleteMapping("/api/partner-events/{id}/promote")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<Void> unpromoteEvent(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails) {
        promotedAdService.unpromoteEvent(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
