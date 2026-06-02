package hr.tvz.experimate.experimate.domain.promoted_ad;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromotedAdIT extends AbstractIntegrationTest {

    private static final String ADS_URL = "/api/promoted-ads";

    // ──────────────── createAd ────────────────

    @Test
    void createAd_returns201WithAdData() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);

        ResponseEntity<PromotedAdResponse> response = restTemplate.exchange(
                ADS_URL, HttpMethod.POST,
                new HttpEntity<>(new CreatePromotedAdRequest("Summer Sale", "Best deal", "https://example.com", null, null), bearerHeaders(jwt)),
                PromotedAdResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().title()).isEqualTo("Summer Sale");
        assertThat(response.getBody().active()).isTrue();
    }

    // ──────────────── getMyAds ────────────────

    @Test
    void getMyAds_returnsOnlyOwnAds() {
        String jwt1 = loginAndGetTokens("partner1").get("accessToken");
        String jwt2 = loginAndGetTokens("partner2").get("accessToken");
        applyAsPartner(jwt1);
        applyAsPartner(jwt2);
        createAd(jwt1, "Partner1 Ad");
        createAd(jwt2, "Partner2 Ad");

        ResponseEntity<List<PromotedAdResponse>> response = restTemplate.exchange(
                ADS_URL + "/mine", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt1)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).title()).isEqualTo("Partner1 Ad");
    }

    // ──────────────── updateAd ────────────────

    @Test
    void updateAd_returns200WithUpdatedTitle() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer adId = createAd(jwt, "Old Title");

        ResponseEntity<PromotedAdResponse> response = restTemplate.exchange(
                ADS_URL + "/" + adId, HttpMethod.PUT,
                new HttpEntity<>(new UpdatePromotedAdRequest("New Title", null, null, null, null, null), bearerHeaders(jwt)),
                PromotedAdResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().title()).isEqualTo("New Title");
    }

    @Test
    void updateAd_returns403ForNonOwner() {
        String ownerJwt = loginAndGetTokens("owner").get("accessToken");
        String otherJwt = loginAndGetTokens("other").get("accessToken");
        applyAsPartner(ownerJwt);
        applyAsPartner(otherJwt);
        Integer adId = createAd(ownerJwt, "Owner Ad");

        ResponseEntity<Void> response = restTemplate.exchange(
                ADS_URL + "/" + adId, HttpMethod.PUT,
                new HttpEntity<>(new UpdatePromotedAdRequest("Hijacked", null, null, null, null, null), bearerHeaders(otherJwt)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ──────────────── extend ────────────────

    @Test
    void extend_returns200AndPushesActiveUntilByPurchasedDays() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        LocalDateTime activeUntil = LocalDateTime.of(2030, 1, 1, 12, 0);
        Integer adId = restTemplate.exchange(
                ADS_URL, HttpMethod.POST,
                new HttpEntity<>(new CreatePromotedAdRequest("Boosted", null, null, null, activeUntil), bearerHeaders(jwt)),
                PromotedAdResponse.class
        ).getBody().id();

        ResponseEntity<PromotedAdResponse> response = restTemplate.exchange(
                ADS_URL + "/" + adId + "/extend", HttpMethod.POST,
                new HttpEntity<>(new ExtendPromotedAdRequest(7), bearerHeaders(jwt)),
                PromotedAdResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // window still in the future, so the 7 days are added onto the existing end
        assertThat(response.getBody().activeUntil()).isEqualTo(activeUntil.plusDays(7));
    }

    // ──────────────── deleteAd ────────────────

    @Test
    void deleteAd_returns204AndAdNoLongerVisible() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer adId = createAd(jwt, "To Delete");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                ADS_URL + "/" + adId, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(jwt)),
                Void.class
        );

        ResponseEntity<List<PromotedAdResponse>> listResponse = restTemplate.exchange(
                ADS_URL + "/mine", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listResponse.getBody()).isEmpty();
    }
}
