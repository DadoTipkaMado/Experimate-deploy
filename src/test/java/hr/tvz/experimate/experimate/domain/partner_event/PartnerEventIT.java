package hr.tvz.experimate.experimate.domain.partner_event;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerEventIT extends AbstractIntegrationTest {

    private static final String PIN_EVENTS_URL = "/api/partner-pins/";
    private static final String EVENT_URL      = "/api/partner-events/";

    // ──────────────── createEvent ────────────────

    @Test
    void createEvent_returns201WithEventData() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);

        ResponseEntity<PartnerEventResponse> response = restTemplate.exchange(
                PIN_EVENTS_URL + pinId + "/events", HttpMethod.POST,
                new HttpEntity<>(new CreatePartnerEventRequest(
                        "My Event", null, null,
                        LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2)),
                        bearerHeaders(jwt)),
                PartnerEventResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().title()).isEqualTo("My Event");
        assertThat(response.getBody().partnerPinId()).isEqualTo(pinId);
    }

    @Test
    void createEvent_returns403ForNonOwner() {
        String ownerJwt = loginAndGetTokens("owner").get("accessToken");
        String otherJwt = loginAndGetTokens("other").get("accessToken");
        applyAsPartner(ownerJwt);
        applyAsPartner(otherJwt);
        Integer pinId = createPin(ownerJwt, 45.0, 16.0);

        ResponseEntity<Void> response = restTemplate.exchange(
                PIN_EVENTS_URL + pinId + "/events", HttpMethod.POST,
                new HttpEntity<>(new CreatePartnerEventRequest(
                        "Hijacked", null, null,
                        LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2)),
                        bearerHeaders(otherJwt)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ──────────────── getEventsForPin ────────────────

    @Test
    void getEventsForPin_returnsEventsOrderedByStart() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId = createPin(jwt, 45.0, 16.0);
        createEvent(jwt, pinId, LocalDateTime.now().plusDays(2), "Later Event");
        createEvent(jwt, pinId, LocalDateTime.now().plusDays(1), "Earlier Event");

        ResponseEntity<List<PartnerEventResponse>> response = restTemplate.exchange(
                PIN_EVENTS_URL + pinId + "/events", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).title()).isEqualTo("Earlier Event");
    }

    // ──────────────── updateEvent ────────────────

    @Test
    void updateEvent_returns200WithUpdatedTitle() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId   = createPin(jwt, 45.0, 16.0);
        Integer eventId = createEvent(jwt, pinId, LocalDateTime.now().plusDays(1), "Old Title");

        ResponseEntity<PartnerEventResponse> response = restTemplate.exchange(
                EVENT_URL + eventId, HttpMethod.PUT,
                new HttpEntity<>(new UpdatePartnerEventRequest("New Title", null, null, null, null), bearerHeaders(jwt)),
                PartnerEventResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().title()).isEqualTo("New Title");
    }

    // ──────────────── deleteEvent ────────────────

    @Test
    void deleteEvent_returns204AndEventNoLongerVisible() {
        String jwt = loginAndGetTokens("partner").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId   = createPin(jwt, 45.0, 16.0);
        Integer eventId = createEvent(jwt, pinId, LocalDateTime.now().plusDays(1), "To Delete");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                EVENT_URL + eventId, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(jwt)),
                Void.class
        );

        ResponseEntity<List<PartnerEventResponse>> listResponse = restTemplate.exchange(
                PIN_EVENTS_URL + pinId + "/events", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(listResponse.getBody()).isEmpty();
    }
}
