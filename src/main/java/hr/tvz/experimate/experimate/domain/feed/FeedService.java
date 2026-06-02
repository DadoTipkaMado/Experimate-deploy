package hr.tvz.experimate.experimate.domain.feed;

import hr.tvz.experimate.experimate.domain.promoted_ad.PromotedAdResponse;
import hr.tvz.experimate.experimate.domain.promoted_ad.PromotedAdService;
import hr.tvz.experimate.experimate.domain.tour_listing.TourListingService;
import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles the interleaved feed of {@link TourListingResponse tour listings} and
 * {@link PromotedAdResponse promoted ads}.
 *
 * <p>The interleave algorithm inserts one ad slot after every {@code adFrequency} listings.
 * Ads are drawn from the currently active set in a round-robin fashion, so all active ads
 * appear with equal frequency regardless of how many listings are on the page.
 *
 * <p>The {@code Page} returned reflects the combined item count, not just listings, so
 * pagination on the frontend is consistent across all feed types.
 */
@Service
public class FeedService {

    private final TourListingService tourListingService;
    private final PromotedAdService promotedAdService;

    public static final int DEFAULT_AD_FREQUENCY = 5;

    public FeedService(TourListingService tourListingService,
                       PromotedAdService promotedAdService) {
        this.tourListingService = tourListingService;
        this.promotedAdService = promotedAdService;
    }

    /**
     * Builds a paginated feed by interleaving tour listings with promoted ads.
     *
     * <p>Every {@code adFrequency}-th position in the feed is replaced by an ad drawn
     * round-robin from the active ad pool. If there are no active ads the feed contains
     * only listings.
     *
     * @param viewerId    the authenticated viewer's user ID; used to exclude their own listings
     * @param pageable    pagination and sort parameters
     * @param adFrequency how many listings appear between each ad slot (minimum 1)
     * @return a page of mixed {@link FeedItem} elements
     */
    @Transactional(readOnly = true)
    public Page<FeedItem> buildFeed(Integer viewerId, Pageable pageable, int adFrequency) {
        int frequency = Math.max(1, adFrequency);

        Page<TourListingResponse> listingPage = tourListingService.getAllListings(viewerId, pageable);
        List<TourListingResponse> listings = listingPage.getContent();

        List<PromotedAdResponse> activeAds = promotedAdService.findActiveAds(LocalDateTime.now());

        if (activeAds.isEmpty()) {
            return listingPage.map(item -> (FeedItem) item);
        }

        List<FeedItem> feed = new ArrayList<>(listings.size() + listings.size() / frequency + 1);
        int adIndex = 0;
        for (int i = 0; i < listings.size(); i++) {
            if (i > 0 && i % frequency == 0) {
                feed.add(activeAds.get(adIndex % activeAds.size()));
                adIndex++;
            }
            feed.add(listings.get(i));
        }

        long totalElements = listingPage.getTotalElements()
                + listingPage.getTotalElements() / frequency;

        return new PageImpl<>(feed, pageable, totalElements);
    }
}
