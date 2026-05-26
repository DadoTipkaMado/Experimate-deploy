package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.security.AppUserDetails;
import hr.tvz.experimate.experimate.shared.exception.InternalServerException;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST endpoints for partner venue pins.
 *
 * <p>Read endpoints ({@code GET /api/partner-pins} and {@code GET /api/partner-pins/{id}})
 * are accessible to all authenticated users so the map can display pins from any partner.
 * The logo serve endpoint ({@code GET /api/partner-pins/logo/{filename}}) is {@code permitAll}
 * and whitelisted in {@link hr.tvz.experimate.experimate.config.SecurityConfig SecurityConfig}.
 * All write operations require {@code ROLE_PARTNER}, and edit/delete additionally verify
 * that the requesting partner owns the pin.
 */
@RestController
@RequestMapping("/api/partner-pins")
public class PartnerPinController {

    private final PartnerPinService partnerPinService;

    public PartnerPinController(PartnerPinService partnerPinService) {
        this.partnerPinService = partnerPinService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PartnerPinResponse> createPin(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody CreatePartnerPinRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(partnerPinService.createPin(userDetails.getId(), req));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<PartnerPinResponse>> getMyPins(
            @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(partnerPinService.getMyPins(userDetails.getId()));
    }

    @GetMapping
    public ResponseEntity<List<PartnerPinResponse>> getAllActivePins() {
        return ResponseEntity.ok(partnerPinService.getAllActivePins());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnerPinResponse> getPinById(@PathVariable Integer id) {
        return ResponseEntity.ok(partnerPinService.getPinById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PartnerPinResponse> updatePin(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestBody UpdatePartnerPinRequest req) {
        return ResponseEntity.ok(partnerPinService.updatePin(id, userDetails.getId(), req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<Void> deletePin(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails) {
        partnerPinService.deletePin(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PartnerPinResponse> uploadLogo(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(partnerPinService.uploadLogo(id, userDetails.getId(), file));
    }

    @GetMapping("/logo/{filename}")
    public ResponseEntity<Resource> getLogo(@PathVariable String filename) {
        Resource resource = partnerPinService.getLogoResource(filename);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElseThrow(() -> new InternalServerException(
                        "Could not determine media type for file: " + resource.getFilename()));
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }
}
