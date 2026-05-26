package hr.tvz.experimate.experimate.domain.feed;

import hr.tvz.experimate.experimate.security.AppUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



/**
 * Serves the interleaved public feed of tour listings and promoted ads.
 *
 * <p>The {@code adFrequency} query parameter controls how many listings appear
 * between each ad slot. Defaults to {@link FeedService#DEFAULT_AD_FREQUENCY}.
 */
@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    /**
     * Returns a paginated feed of listings and ads interleaved at the requested frequency.
     *
     * @param userDetails  the authenticated viewer; their listings are excluded from results
     * @param pageable     pagination and sort parameters (default: 20 items, sorted by meetingDate ASC)
     * @param adFrequency  one ad is inserted after every N listings; defaults to {@value FeedService#DEFAULT_AD_FREQUENCY}
     */
    @GetMapping
    public ResponseEntity<FeedPageResponse> getFeed(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PageableDefault(size = 20, sort = "meetingDate", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(defaultValue = "5") int adFrequency) {
        Page<FeedItem> page = feedService.buildFeed(userDetails.getId(), pageable, adFrequency);
        return ResponseEntity.ok(new FeedPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        ));
    }
}
