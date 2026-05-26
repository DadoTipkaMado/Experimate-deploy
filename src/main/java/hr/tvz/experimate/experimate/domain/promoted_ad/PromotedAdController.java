package hr.tvz.experimate.experimate.domain.promoted_ad;

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
 * REST endpoints for partner promoted ads.
 *
 * <p>All endpoints except the image serve endpoint require {@code ROLE_PARTNER}.
 * The image serve endpoint ({@code GET /api/promoted-ads/image/{filename}}) is {@code permitAll}
 * and whitelisted in {@link hr.tvz.experimate.experimate.config.SecurityConfig SecurityConfig}.
 * Write operations additionally verify that the requesting partner owns the target ad.
 */
@RestController
@RequestMapping("/api/promoted-ads")
public class PromotedAdController {

    private final PromotedAdService promotedAdService;

    public PromotedAdController(PromotedAdService promotedAdService) {
        this.promotedAdService = promotedAdService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PromotedAdResponse> createAd(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody CreatePromotedAdRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotedAdService.createAd(userDetails.getId(), req));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<List<PromotedAdResponse>> getMyAds(
            @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(promotedAdService.getMyAds(userDetails.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PromotedAdResponse> updateAd(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestBody UpdatePromotedAdRequest req) {
        return ResponseEntity.ok(promotedAdService.updateAd(id, userDetails.getId(), req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<Void> deleteAd(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails) {
        promotedAdService.deleteAd(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<PromotedAdResponse> uploadImage(
            @PathVariable Integer id,
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(promotedAdService.uploadImage(id, userDetails.getId(), file));
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = promotedAdService.getImageResource(filename);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElseThrow(() -> new InternalServerException(
                        "Could not determine media type for file: " + resource.getFilename()));
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }
}
