package hr.tvz.experimate.experimate.domain.booking_request;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.booking_request.dto.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.domain.booking_request.response.BookingRequestResponse;
import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.CreateTourListingDto;
import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRequestIT extends AbstractIntegrationTest {

    private static final String LISTING_URL = "/api/tour-listing";
    private static final String BOOKING_URL = "/api/booking-request";

    // Minimum length required by CreateTourListingDto validation
    private static final String DESCRIPTION = "A".repeat(20);

    @Autowired private BookingRequestRepo bookingRequestRepo;
    @Autowired private ReservationRepo reservationRepo;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String createUserAndLogin(String username) {
        return loginAndGetTokens(username).get("accessToken");
    }

    private HttpHeaders bearerHeaders(String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Integer createListing(String hostJwt) {
        CreateTourListingDto dto = new CreateTourListingDto(
                "Zagreb", 15.966568, 45.815399,
                LocalDateTime.now().plusDays(7), DESCRIPTION.repeat(20)
        );
        return restTemplate.exchange(
                LISTING_URL, HttpMethod.POST,
                new HttpEntity<>(dto, bearerHeaders(hostJwt)),
                TourListingResponse.class
        ).getBody().id();
    }

    private Integer sendBookingRequest(String guestJwt, Integer listingId) {
        return restTemplate.exchange(
                BOOKING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateBookingRequestDto(listingId), bearerHeaders(guestJwt)),
                BookingRequestResponse.class
        ).getBody().id();
    }

    // ── POST /api/booking-request ─────────────────────────────────────────

    @Test
    void createBookingRequest_authenticatedGuest_returns201WithPendingStatus() {
        String hostJwt  = createUserAndLogin("host");
        String guestJwt = createUserAndLogin("guest");
        Integer listingId = createListing(hostJwt);

        ResponseEntity<BookingRequestResponse> response = restTemplate.exchange(
                BOOKING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateBookingRequestDto(listingId), bearerHeaders(guestJwt)),
                BookingRequestResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().status()).isEqualTo(BookingRequestStatus.PENDING);
    }

    @Test
    void createBookingRequest_unauthenticated_returns401() {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                BOOKING_URL, new CreateBookingRequestDto(1), Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createBookingRequest_listingNotFound_returns404() {
        String guestJwt = createUserAndLogin("guest");

        ResponseEntity<Void> response = restTemplate.exchange(
                BOOKING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateBookingRequestDto(999), bearerHeaders(guestJwt)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createBookingRequest_guestIsHost_returns4xxClientError() {
        String hostJwt = createUserAndLogin("host");
        Integer listingId = createListing(hostJwt);

        ResponseEntity<Void> response = restTemplate.exchange(
                BOOKING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateBookingRequestDto(listingId), bearerHeaders(hostJwt)),
                Void.class
        );

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void createBookingRequest_duplicatePendingRequest_returns409() {
        String hostJwt  = createUserAndLogin("host");
        String guestJwt = createUserAndLogin("guest");
        Integer listingId = createListing(hostJwt);
        sendBookingRequest(guestJwt, listingId);

        ResponseEntity<Void> response = restTemplate.exchange(
                BOOKING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateBookingRequestDto(listingId), bearerHeaders(guestJwt)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── PATCH /api/booking-request/accept/{id} ────────────────────────────

    @Test
    void acceptBookingRequest_host_returns200WithAcceptedStatus() {
        String hostJwt  = createUserAndLogin("host");
        String guestJwt = createUserAndLogin("guest");
        Integer listingId = createListing(hostJwt);
        Integer requestId = sendBookingRequest(guestJwt, listingId);

        ResponseEntity<BookingRequestResponse> response = restTemplate.exchange(
                BOOKING_URL + "/accept/" + requestId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(hostJwt)),
                BookingRequestResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(BookingRequestStatus.ACCEPTED);
    }

    /**
     * When the host accepts one request, all other pending requests for the same listing
     * must be automatically declined in the database.
     */
    @Test
    void acceptBookingRequest_competingRequests_areDeclinedInDatabase() {
        String hostJwt   = createUserAndLogin("host");
        String guest1Jwt = createUserAndLogin("guest1");
        String guest2Jwt = createUserAndLogin("guest2");
        Integer listingId  = createListing(hostJwt);
        Integer acceptedId = sendBookingRequest(guest1Jwt, listingId);
        Integer declinedId = sendBookingRequest(guest2Jwt, listingId);

        restTemplate.exchange(
                BOOKING_URL + "/accept/" + acceptedId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(hostJwt)), Void.class
        );

        assertThat(bookingRequestRepo.findById(declinedId))
                .hasValueSatisfying(r -> assertThat(r.getStatus()).isEqualTo(BookingRequestStatus.DECLINED));
    }

    /**
     * Accepting a request publishes a BookingRequestAcceptedEvent; the
     * ReservationService listener must handle it and persist a Reservation.
     */
    @Test
    void acceptBookingRequest_createsReservationViaEvent() {
        String hostJwt  = createUserAndLogin("host");
        String guestJwt = createUserAndLogin("guest");
        Integer listingId = createListing(hostJwt);
        Integer requestId = sendBookingRequest(guestJwt, listingId);

        restTemplate.exchange(
                BOOKING_URL + "/accept/" + requestId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(hostJwt)), Void.class
        );

        assertThat(reservationRepo.findAll()).hasSize(1);
    }

    @Test
    void acceptBookingRequest_nonHost_returns403() {
        String hostJwt  = createUserAndLogin("host");
        String guestJwt = createUserAndLogin("guest");
        Integer listingId = createListing(hostJwt);
        Integer requestId = sendBookingRequest(guestJwt, listingId);

        ResponseEntity<Void> response = restTemplate.exchange(
                BOOKING_URL + "/accept/" + requestId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(guestJwt)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── PATCH /api/booking-request/decline/{id} ───────────────────────────

    @Test
    void declineBookingRequest_host_returns200WithDeclinedStatus() {
        String hostJwt  = createUserAndLogin("host");
        String guestJwt = createUserAndLogin("guest");
        Integer listingId = createListing(hostJwt);
        Integer requestId = sendBookingRequest(guestJwt, listingId);

        ResponseEntity<BookingRequestResponse> response = restTemplate.exchange(
                BOOKING_URL + "/decline/" + requestId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(hostJwt)),
                BookingRequestResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(BookingRequestStatus.DECLINED);
    }

    @Test
    void declineBookingRequest_nonHost_returns403() {
        String hostJwt  = createUserAndLogin("host");
        String guestJwt = createUserAndLogin("guest");
        Integer listingId = createListing(hostJwt);
        Integer requestId = sendBookingRequest(guestJwt, listingId);

        ResponseEntity<Void> response = restTemplate.exchange(
                BOOKING_URL + "/decline/" + requestId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(guestJwt)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── DELETE /api/booking-request/{id} ─────────────────────────────────

    @Test
    void deleteBookingRequest_guest_returns204AndRemovedFromDatabase() {
        String hostJwt  = createUserAndLogin("host");
        String guestJwt = createUserAndLogin("guest");
        Integer listingId = createListing(hostJwt);
        Integer requestId = sendBookingRequest(guestJwt, listingId);

        ResponseEntity<Void> response = restTemplate.exchange(
                BOOKING_URL + "/" + requestId, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(guestJwt)), Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(bookingRequestRepo.findById(requestId)).isEmpty();
    }

    @Test
    void deleteBookingRequest_nonGuest_returns403() {
        String hostJwt  = createUserAndLogin("host");
        String guestJwt = createUserAndLogin("guest");
        Integer listingId = createListing(hostJwt);
        Integer requestId = sendBookingRequest(guestJwt, listingId);

        ResponseEntity<Void> response = restTemplate.exchange(
                BOOKING_URL + "/" + requestId, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(hostJwt)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
