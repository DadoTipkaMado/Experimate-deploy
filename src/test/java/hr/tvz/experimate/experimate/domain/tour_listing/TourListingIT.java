package hr.tvz.experimate.experimate.domain.tour_listing;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.CreateListingFromEventRequest;
import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TourListingIT extends AbstractIntegrationTest {

    private static final String FROM_EVENT_URL = "/api/tour-listing/from-partner-event";

    // ──────────────── createFromPartnerEvent ────────────────

    @Test
    void createFromPartnerEvent_returns201WithCoordinatesFromPin() {
        String jwt = loginAndGetTokens("testuser").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId   = createPin(jwt, 45.0, 16.0);
        Integer eventId = createEvent(jwt, pinId, LocalDateTime.now().plusDays(1), "Test Event");

        CreateListingFromEventRequest req = new CreateListingFromEventRequest(
                eventId, "Zagreb", DESCRIPTION, 5, null, null, null);

        ResponseEntity<TourListingResponse> response = restTemplate.exchange(
                FROM_EVENT_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(jwt)),
                TourListingResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().lat()).isEqualTo(45.0);
        assertThat(response.getBody().lng()).isEqualTo(16.0);
    }

    @Test
    void createFromPartnerEvent_returns201WithOverrideCoordinates() {
        String jwt = loginAndGetTokens("testuser").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId   = createPin(jwt, 45.0, 16.0);
        Integer eventId = createEvent(jwt, pinId, LocalDateTime.now().plusDays(1), "Test Event");

        CreateListingFromEventRequest req = new CreateListingFromEventRequest(
                eventId, "Zagreb", DESCRIPTION, 5, null, 46.0, 17.0);

        ResponseEntity<TourListingResponse> response = restTemplate.exchange(
                FROM_EVENT_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(jwt)),
                TourListingResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().lat()).isEqualTo(46.0);
        assertThat(response.getBody().lng()).isEqualTo(17.0);
    }

    @Test
    void createFromPartnerEvent_returns201WithMeetingDateFromEvent() {
        // withNano(0) — JSON round-trip drops sub-second precision
        LocalDateTime eventStart = LocalDateTime.now().plusDays(1).withNano(0);
        String jwt = loginAndGetTokens("testuser").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId   = createPin(jwt, 45.0, 16.0);
        Integer eventId = createEvent(jwt, pinId, eventStart, "title");

        CreateListingFromEventRequest req = new CreateListingFromEventRequest(
                eventId, "Zagreb", DESCRIPTION, 5, null, null, null);

        ResponseEntity<TourListingResponse> response = restTemplate.exchange(
                FROM_EVENT_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(jwt)),
                TourListingResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().meetingDate()).isEqualTo(eventStart);
    }

    @Test
    void createFromPartnerEvent_returns201WithOverrideMeetingDate() {
        LocalDateTime eventStart   = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime overrideDate = LocalDateTime.now().plusDays(3).withNano(0);
        String jwt = loginAndGetTokens("testuser").get("accessToken");
        applyAsPartner(jwt);
        Integer pinId   = createPin(jwt, 45.0, 16.0);
        Integer eventId = createEvent(jwt, pinId, eventStart, "title");

        CreateListingFromEventRequest req = new CreateListingFromEventRequest(
                eventId, "Zagreb", DESCRIPTION, 5, overrideDate, null, null);

        ResponseEntity<TourListingResponse> response = restTemplate.exchange(
                FROM_EVENT_URL, HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(jwt)),
                TourListingResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().meetingDate()).isEqualTo(overrideDate);
    }
}
