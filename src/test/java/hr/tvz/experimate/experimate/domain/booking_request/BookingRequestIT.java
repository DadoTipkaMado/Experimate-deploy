package hr.tvz.experimate.experimate.domain.booking_request;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.booking_request.dto.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.domain.booking_request.response.BookingRequestResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BookingRequestIT extends AbstractIntegrationTest {

    @Autowired private BookingRequestRepo bookingRequestRepo;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String createUserAndLogin(String username) {
        return loginAndGetTokens(username).get("accessToken");
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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);

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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);

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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);
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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);
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
     * When the host accepts a request and the listing becomes full (maxGuests=1),
     * all remaining pending requests must be automatically declined in the database.
     */
    @Test
    void acceptBookingRequest_competingRequests_areDeclinedInDatabase() {
        String hostJwt   = createUserAndLogin("host");
        String guest1Jwt = createUserAndLogin("guest1");
        String guest2Jwt = createUserAndLogin("guest2");
        // maxGuests=1 so accepting guest1 fills the listing and triggers auto-decline of guest2
        Integer listingId  = createListing(hostJwt, 7, ChronoUnit.DAYS, 1);
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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);
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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);
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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);
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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);
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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);
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
        Integer listingId = createListing(hostJwt, 7, ChronoUnit.DAYS);
        Integer requestId = sendBookingRequest(guestJwt, listingId);

        ResponseEntity<Void> response = restTemplate.exchange(
                BOOKING_URL + "/" + requestId, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(hostJwt)),
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
