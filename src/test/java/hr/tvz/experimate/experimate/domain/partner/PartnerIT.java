package hr.tvz.experimate.experimate.domain.partner;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEvent;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventRepository;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventResponse;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPin;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPinRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerIT extends AbstractIntegrationTest {

    private static final String APPLY_URL   = "/api/partner/apply";
    private static final String PROFILE_URL = "/api/partner/profile";
    private static final String STATS_URL   = "/api/partner/stats";
    private static final String EVENTS_URL  = "/api/partner/events";

    @Autowired
    private PartnerPinRepository partnerPinRepository;

    @Autowired
    private PartnerEventRepository partnerEventRepository;

    // ──────────────── apply ────────────────

    @Test
    void apply_returns201WithCompanyData() {
        String jwt = loginAndGetTokens("partner").get("accessToken");

        ResponseEntity<PartnerProfileResponse> response = restTemplate.exchange(
                APPLY_URL, HttpMethod.POST,
                new HttpEntity<>(new ApplyPartnerRequest("Test Corp", "test@corp.com", null), bearerHeaders(jwt)),
                PartnerProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().companyName()).isEqualTo("Test Corp");
        assertThat(response.getBody().contactEmail()).isEqualTo("test@corp.com");
    }

    // ──────────────── profile ────────────────

    @Test
    void getProfile_returns200WithStoredCompanyData() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);

        ResponseEntity<PartnerProfileResponse> response = restTemplate.exchange(
                PROFILE_URL, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                PartnerProfileResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().companyName()).isEqualTo("Test Corp");
        assertThat(response.getBody().contactEmail()).isEqualTo("test@corp.com");
    }

    // ──────────────── stats ────────────────

    @Test
    void getStats_returns200WithCorrectActiveEventsCount() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);
        createEvent(jwt, pinId, LocalDateTime.now().plusDays(1), "Test Event");

        ResponseEntity<PartnerStatsResponse> response = restTemplate.exchange(
                STATS_URL, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                PartnerStatsResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().activeEvents()).isEqualTo(1);
    }

    // ──────────────── events ────────────────

    @Test
    void getEvents_returnsPartnerOwnedEvents() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);
        createEvent(jwt, pinId, LocalDateTime.now().plusDays(1), "My Event");

        ResponseEntity<List<PartnerEventResponse>> response = restTemplate.exchange(
                EVENTS_URL, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).title()).isEqualTo("My Event");
        assertThat(response.getBody().get(0).partnerPinId()).isEqualTo(pinId);
    }

    @Test
    void getEvents_filterUpcoming_excludesPastEvents() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);
        createEvent(jwt, pinId, LocalDateTime.now().plusDays(1), "Future Event");

        // API validates @Future, so insert the past event directly via the repository
        PartnerPin pin = partnerPinRepository.findById(pinId).orElseThrow();
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        partnerEventRepository.save(new PartnerEvent(
                pin, "Past Event", null, null, yesterday, yesterday.plusHours(2), LocalDateTime.now()));

        ResponseEntity<List<PartnerEventResponse>> response = restTemplate.exchange(
                EVENTS_URL + "?filter=upcoming", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).title()).isEqualTo("Future Event");
    }
}
