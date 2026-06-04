package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerPinSubscriptionIT extends AbstractIntegrationTest {

    private static final String PIN_URL = "/api/partner-pins";

    private String subscriptionUrl(Integer pinId) {
        return PIN_URL + "/" + pinId + "/subscription";
    }

    // ──────────────── subscribe ────────────────

    @Test
    void subscribe_returns201WithActiveSubscription() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);

        ResponseEntity<PartnerPinSubscriptionResponse> response = restTemplate.exchange(
                subscriptionUrl(pinId), HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(jwt)),
                PartnerPinSubscriptionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().pinId()).isEqualTo(pinId);
        assertThat(response.getBody().status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.getBody().cancelAtPeriodEnd()).isFalse();
        assertThat(response.getBody().currentPeriodEnd())
                .isAfter(response.getBody().currentPeriodStart());
    }

    @Test
    void subscribe_highlightsPinInListing() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);

        restTemplate.exchange(subscriptionUrl(pinId), HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(jwt)), PartnerPinSubscriptionResponse.class);

        ResponseEntity<PartnerPinResponse> pin = restTemplate.exchange(
                PIN_URL + "/" + pinId, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                PartnerPinResponse.class
        );

        assertThat(pin.getBody().highlighted()).isTrue();
    }

    // ──────────────── cancel ────────────────

    @Test
    void cancel_returns200AndMarksCancelAtPeriodEnd() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);
        restTemplate.exchange(subscriptionUrl(pinId), HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(jwt)), PartnerPinSubscriptionResponse.class);

        ResponseEntity<PartnerPinSubscriptionResponse> response = restTemplate.exchange(
                subscriptionUrl(pinId), HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(jwt)),
                PartnerPinSubscriptionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().cancelAtPeriodEnd()).isTrue();
        // stays ACTIVE: the pin remains highlighted until the paid period ends
        assertThat(response.getBody().status()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    // ──────────────── getSubscription ────────────────

    @Test
    void getSubscription_returnsCurrentSubscriptionState() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);
        restTemplate.exchange(subscriptionUrl(pinId), HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(jwt)), PartnerPinSubscriptionResponse.class);

        ResponseEntity<PartnerPinSubscriptionResponse> response = restTemplate.exchange(
                subscriptionUrl(pinId), HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                PartnerPinSubscriptionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().pinId()).isEqualTo(pinId);
        assertThat(response.getBody().status()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
}
