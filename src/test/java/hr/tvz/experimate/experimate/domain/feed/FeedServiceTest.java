package hr.tvz.experimate.experimate.domain.feed;

import hr.tvz.experimate.experimate.domain.promoted_ad.PromotedAdResponse;
import hr.tvz.experimate.experimate.domain.promoted_ad.PromotedAdService;
import hr.tvz.experimate.experimate.domain.tour_listing.TourListingService;
import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock private TourListingService tourListingService;
    @Mock private PromotedAdService promotedAdService;

    @InjectMocks private FeedService service;

    // ──────────────── helpers ────────────────

    private TourListingResponse listing(int id) {
        return new TourListingResponse(id, null, null, null, null, null, null, null, 0, null);
    }

    private PromotedAdResponse ad(int id) {
        return new PromotedAdResponse(id, "Ad " + id, null, null, null, true, 0, null, null, null);
    }

    // ──────────────── buildFeed ────────────────

    @Test
    void buildFeed_returnsOnlyListingsWhenNoActiveAds() {
        Pageable pageable = PageRequest.of(0, 20);
        List<TourListingResponse> listings = List.of(listing(1), listing(2), listing(3));
        when(tourListingService.getAllListings(1, pageable)).thenReturn(new PageImpl<>(listings, pageable, 3));
        when(promotedAdService.findActiveAds(any(LocalDateTime.class))).thenReturn(List.of());

        Page<FeedItem> feed = service.buildFeed(1, pageable, 5);

        assertThat(feed.getContent()).hasSize(3);
        assertThat(feed.getContent()).allMatch(item -> item instanceof TourListingResponse);
    }

    @Test
    void buildFeed_interleavesSingleAdAfterEveryNthListing() {
        // 4 listinga, 1 oglas, frequency=2
        // algoritam: i=0→L1, i=1→L2, i=2→Ad+L3, i=3→L4
        // rezultat: [L1, L2, Ad1, L3, L4]
        Pageable pageable = PageRequest.of(0, 20);
        List<TourListingResponse> listings = List.of(listing(1), listing(2), listing(3), listing(4));
        PromotedAdResponse adItem = ad(1);
        when(tourListingService.getAllListings(1, pageable)).thenReturn(new PageImpl<>(listings, pageable, 4));
        when(promotedAdService.findActiveAds(any(LocalDateTime.class))).thenReturn(List.of(adItem));

        List<FeedItem> content = service.buildFeed(1, pageable, 2).getContent();

        assertThat(content).hasSize(5);
        assertThat(content.get(0)).isEqualTo(listing(1));
        assertThat(content.get(1)).isEqualTo(listing(2));
        assertThat(content.get(2)).isEqualTo(adItem);
        assertThat(content.get(3)).isEqualTo(listing(3));
        assertThat(content.get(4)).isEqualTo(listing(4));
    }

    @Test
    void buildFeed_cyclesAdsRoundRobinWhenMultipleActive() {
        // 6 listinga, 2 oglasa, frequency=2
        // i=0→L1, i=1→L2, i=2→Ad1+L3, i=3→L4, i=4→Ad2+L5, i=5→L6
        // rezultat: [L1, L2, Ad1, L3, L4, Ad2, L5, L6]
        Pageable pageable = PageRequest.of(0, 20);
        List<TourListingResponse> listings = List.of(listing(1), listing(2), listing(3), listing(4), listing(5), listing(6));
        PromotedAdResponse ad1 = ad(1);
        PromotedAdResponse ad2 = ad(2);
        when(tourListingService.getAllListings(1, pageable)).thenReturn(new PageImpl<>(listings, pageable, 6));
        when(promotedAdService.findActiveAds(any(LocalDateTime.class))).thenReturn(List.of(ad1, ad2));

        List<FeedItem> content = service.buildFeed(1, pageable, 2).getContent();

        assertThat(content).hasSize(8);
        assertThat(content.get(2)).isEqualTo(ad1);
        assertThat(content.get(5)).isEqualTo(ad2);
    }

    @Test
    void buildFeed_clampsAdFrequencyToMinimumOfOne() {
        // frequency=0 → Math.max(1, 0) = 1 → oglas nakon svakog listinga
        // 2 listinga: i=0→L1, i=1→Ad1+L2
        // rezultat: [L1, Ad1, L2]
        Pageable pageable = PageRequest.of(0, 20);
        List<TourListingResponse> listings = List.of(listing(1), listing(2));
        PromotedAdResponse adItem = ad(1);
        when(tourListingService.getAllListings(1, pageable)).thenReturn(new PageImpl<>(listings, pageable, 2));
        when(promotedAdService.findActiveAds(any(LocalDateTime.class))).thenReturn(List.of(adItem));

        List<FeedItem> content = service.buildFeed(1, pageable, 0).getContent();

        assertThat(content).hasSize(3);
        assertThat(content.get(0)).isEqualTo(listing(1));
        assertThat(content.get(1)).isEqualTo(adItem);
        assertThat(content.get(2)).isEqualTo(listing(2));
    }
}
