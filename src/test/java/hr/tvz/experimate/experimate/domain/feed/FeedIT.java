package hr.tvz.experimate.experimate.domain.feed;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.promoted_ad.PromotedAdResponse;
import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class FeedIT extends AbstractIntegrationTest {

    private static final String FEED_URL = "/api/feed";

    // ──────────────── getFeed ────────────────

    @Test
    void getFeed_excludesViewerOwnListings() {
        String jwt = loginAndGetTokens("host").get("accessToken");
        createListing(jwt, 1, ChronoUnit.DAYS);

        ResponseEntity<FeedPageResponse> response = restTemplate.exchange(
                FEED_URL, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                FeedPageResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).isEmpty();
    }

    @Test
    void getFeed_returnsOnlyListingsWhenNoActiveAds() {
        String hostJwt   = loginAndGetTokens("host").get("accessToken");
        String viewerJwt = loginAndGetTokens("viewer").get("accessToken");
        createListing(hostJwt, 1, ChronoUnit.DAYS);

        ResponseEntity<FeedPageResponse> response = restTemplate.exchange(
                FEED_URL, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(viewerJwt)),
                FeedPageResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).isNotEmpty();
        assertThat(response.getBody().content())
                .allSatisfy(item -> assertThat(item).isInstanceOf(TourListingResponse.class));
    }

    @Test
    void getFeed_interleaveAdAtCorrectPosition() {
        String hostJwt    = loginAndGetTokens("host").get("accessToken");
        String partnerJwt = loginAndGetTokens("partner").get("accessToken");
        String viewerJwt  = loginAndGetTokens("viewer").get("accessToken");

        // Two listings from a different user so the viewer sees both
        createListing(hostJwt, 1, ChronoUnit.DAYS);
        createListing(hostJwt, 2, ChronoUnit.DAYS);

        // One always-active ad (null activeFrom/activeUntil = no scheduling boundary)
        applyAsPartner(partnerJwt);
        createAd(partnerJwt, "Test Ad");

        // adFrequency=1 → algorithm: [L0, AD, L1]
        ResponseEntity<FeedPageResponse> response = restTemplate.exchange(
                FEED_URL + "?adFrequency=1", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(viewerJwt)),
                FeedPageResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).hasSize(3);
        assertThat(response.getBody().content().get(0)).isInstanceOf(TourListingResponse.class);
        assertThat(response.getBody().content().get(1)).isInstanceOf(PromotedAdResponse.class);
        assertThat(response.getBody().content().get(2)).isInstanceOf(TourListingResponse.class);
    }
}
