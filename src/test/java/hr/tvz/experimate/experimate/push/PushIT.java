package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.push.PushController.SubscribeRequest;
import hr.tvz.experimate.experimate.push.PushController.UnsubscribeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PushIT extends AbstractIntegrationTest {

    private static final String SUBSCRIBE_URL   = "/api/push/subscribe";
    private static final String UNSUBSCRIBE_URL = "/api/push/unsubscribe";
    private static final String TEST_ENDPOINT   = "https://push.example.com/test-sub-1";

    @Autowired
    PushSubscriptionRepo pushSubscriptionRepo;

    @Test
    void unsubscribe_whenAuthenticated_returns204AndDeletesSubscription() {
        Map<String, String> tokens = loginAndGetTokens("pushuser");
        String jwt = tokens.get("accessToken");

        restTemplate.exchange(SUBSCRIBE_URL, HttpMethod.POST,
                new HttpEntity<>(new SubscribeRequest(TEST_ENDPOINT, "p256dh-test", "auth-test"), bearerHeaders(jwt)),
                Void.class);
        assertThat(pushSubscriptionRepo.existsByEndpoint(TEST_ENDPOINT)).isTrue();

        ResponseEntity<Void> response = restTemplate.exchange(UNSUBSCRIBE_URL, HttpMethod.POST,
                new HttpEntity<>(new UnsubscribeRequest(TEST_ENDPOINT), bearerHeaders(jwt)),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(pushSubscriptionRepo.existsByEndpoint(TEST_ENDPOINT)).isFalse();
    }

    @Test
    void unsubscribe_whenUnauthenticated_returns401() {
        ResponseEntity<Void> response = restTemplate.exchange(UNSUBSCRIBE_URL, HttpMethod.POST,
                new HttpEntity<>(new UnsubscribeRequest(TEST_ENDPOINT)),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unsubscribe_whenEndpointMissing_returns400() {
        Map<String, String> tokens = loginAndGetTokens("pushuser2");
        String jwt = tokens.get("accessToken");

        ResponseEntity<Void> response = restTemplate.exchange(UNSUBSCRIBE_URL, HttpMethod.POST,
                new HttpEntity<>("{}", bearerHeaders(jwt)),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
