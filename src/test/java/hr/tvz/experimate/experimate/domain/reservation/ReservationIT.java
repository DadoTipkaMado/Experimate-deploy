package hr.tvz.experimate.experimate.domain.reservation;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.booking_request.dto.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.domain.booking_request.response.BookingRequestResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.CancelTourResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.EndTourResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.PresenceResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.ReservationResponse;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.CreateTourListingDto;
import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationIT extends AbstractIntegrationTest {

    private static final String LISTING_URL     = "/api/tour-listing";
    private static final String BOOKING_URL     = "/api/booking-request";
    private static final String RESERVATION_URL = "/api/reservation";
    private static final String DESCRIPTION     = "A".repeat(20);

    @Autowired
    private ReservationRepo reservationRepo;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HttpHeaders bearerHeaders(String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Creates a listing with a meetingDate 25 minutes from now so check-in
     * is immediately available (window opens 30 minutes before the meeting).
     */
    private Integer createListing(String hostJwt) {
        CreateTourListingDto dto = new CreateTourListingDto(
                "Zagreb", 15.966568, 45.815399,
                LocalDateTime.now().plusMinutes(25),
                DESCRIPTION.repeat(20)
        );
        return restTemplate.exchange(
                LISTING_URL, HttpMethod.POST,
                new HttpEntity<>(dto, bearerHeaders(hostJwt)),
                TourListingResponse.class
        ).getBody().id();
    }

    /**
     * Runs the full flow: create listing → send booking request → host accepts.
     * Returns the ID of the resulting reservation.
     */
    private Integer createReservation(String hostJwt, String guestJwt) {
        Integer listingId = createListing(hostJwt);
        Integer requestId = restTemplate.exchange(
                BOOKING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateBookingRequestDto(listingId), bearerHeaders(guestJwt)),
                BookingRequestResponse.class
        ).getBody().id();
        restTemplate.exchange(
                BOOKING_URL + "/accept/" + requestId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(hostJwt)), Void.class
        );
        return reservationRepo.findAll().getFirst().getId();
    }

    private void checkIn(String jwt, Integer reservationId) {
        restTemplate.exchange(
                RESERVATION_URL + "/check-in/" + reservationId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(jwt)), Void.class
        );
    }

    private ResponseEntity<CancelTourResponse> cancelTour(String jwt, Integer reservationId) {
        return restTemplate.exchange(
                RESERVATION_URL + "/cancel-tour/" + reservationId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(jwt)),
                CancelTourResponse.class
        );
    }

    private ResponseEntity<EndTourResponse> endTour(String jwt, Integer reservationId) {
        return restTemplate.exchange(
                RESERVATION_URL + "/end-tour/" + reservationId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(jwt)),
                EndTourResponse.class
        );
    }

    private ResponseEntity<PresenceResponse[]> getPresence(String jwt, Integer reservationId) {
        return restTemplate.exchange(
                RESERVATION_URL + "/" + reservationId + "/presence", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                PresenceResponse[].class
        );
    }

    // ── POST booking-request accept → reservation created ────────────────────

    @Test
    void getReservation_afterBookingRequestAccepted_returnsConfirmedWithGuestAndListingDetails() {
        Map<String, String> hostTokens  = loginAndGetTokens("host");
        Map<String, String> guestTokens = loginAndGetTokens("guest");
        Integer reservationId = createReservation(hostTokens.get("accessToken"), guestTokens.get("accessToken"));

        ResponseEntity<ReservationResponse> response = restTemplate.exchange(
                RESERVATION_URL + "/" + reservationId, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(guestTokens.get("accessToken"))),
                ReservationResponse.class
        );
        ReservationResponse body = response.getBody();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(body.guest()).isNotNull();
        assertThat(body.tourListing()).isNotNull();
    }

    // ── GET /{reservationId}/presence ─────────────────────────────────────────

    @Test
    void getPresence_afterGuestCheckIn_guestCheckedInHostNot() {
        Map<String, String> hostTokens  = loginAndGetTokens("host");
        Map<String, String> guestTokens = loginAndGetTokens("guest");
        Integer reservationId = createReservation(hostTokens.get("accessToken"), guestTokens.get("accessToken"));

        checkIn(guestTokens.get("accessToken"), reservationId);

        ResponseEntity<PresenceResponse[]> response = getPresence(guestTokens.get("accessToken"), reservationId);
        List<PresenceResponse> presence = List.of(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(presence).hasSize(2);
        assertThat(presence).anySatisfy(p -> {
            assertThat(p.isHost()).isFalse();
            assertThat(p.checkedIn()).isTrue();
        });
        assertThat(presence).anySatisfy(p -> {
            assertThat(p.isHost()).isTrue();
            assertThat(p.checkedIn()).isFalse();
        });
    }

    @Test
    void getPresence_afterBothCheckIn_bothCheckedInAndStatusActive() {
        Map<String, String> hostTokens  = loginAndGetTokens("host");
        Map<String, String> guestTokens = loginAndGetTokens("guest");
        Integer reservationId = createReservation(hostTokens.get("accessToken"), guestTokens.get("accessToken"));

        checkIn(guestTokens.get("accessToken"), reservationId);
        checkIn(hostTokens.get("accessToken"), reservationId);

        ResponseEntity<PresenceResponse[]> response = getPresence(guestTokens.get("accessToken"), reservationId);
        List<PresenceResponse> presence = List.of(response.getBody());
        Optional<Reservation> reservation = reservationRepo.findById(reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(presence).hasSize(2);
        assertThat(presence).allSatisfy(p -> assertThat(p.checkedIn()).isTrue());
        assertThat(reservation).isPresent();
        assertThat(reservation.get().getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    // ── PATCH /end-tour/{reservationId} ───────────────────────────────────────

    @Test
    void endTour_byGuest_returns200WithClosedStatus() {
        Map<String, String> hostTokens  = loginAndGetTokens("host");
        Map<String, String> guestTokens = loginAndGetTokens("guest");
        Integer reservationId = createReservation(hostTokens.get("accessToken"), guestTokens.get("accessToken"));

        checkIn(guestTokens.get("accessToken"), reservationId);
        checkIn(hostTokens.get("accessToken"), reservationId);

        ResponseEntity<EndTourResponse> response = endTour(guestTokens.get("accessToken"), reservationId);
        EndTourResponse body = response.getBody();
        Optional<Reservation> reservation = reservationRepo.findById(reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.reservationId()).isEqualTo(reservationId);
        assertThat(body.status()).isEqualTo(ReservationStatus.CLOSED);
        assertThat(body.endedByUsername()).isEqualTo("guest");
        assertThat(body.endTimestamp()).isNotNull();
        assertThat(reservation).isPresent();
        assertThat(reservation.get().getStatus()).isEqualTo(ReservationStatus.CLOSED);
    }

    // ── PATCH /cancel-tour/{reservationId} ────────────────────────────────────

    @Test
    void cancelTour_byGuestFromConfirmed_returns200WithCancelledStatus() {
        Map<String, String> hostTokens  = loginAndGetTokens("host");
        Map<String, String> guestTokens = loginAndGetTokens("guest");
        Integer reservationId = createReservation(hostTokens.get("accessToken"), guestTokens.get("accessToken"));

        ResponseEntity<CancelTourResponse> response = cancelTour(guestTokens.get("accessToken"), reservationId);
        CancelTourResponse body = response.getBody();
        Optional<Reservation> reservation = reservationRepo.findById(reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.reservationId()).isEqualTo(reservationId);
        assertThat(body.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(body.cancelledByUsername()).isEqualTo("guest");
        assertThat(body.cancelTimestamp()).isNotNull();
        assertThat(reservation).isPresent();
        assertThat(reservation.get().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancelTour_byHostFromConfirmed_returns200WithCancelledStatus() {
        Map<String, String> hostTokens  = loginAndGetTokens("host");
        Map<String, String> guestTokens = loginAndGetTokens("guest");
        Integer reservationId = createReservation(hostTokens.get("accessToken"), guestTokens.get("accessToken"));

        ResponseEntity<CancelTourResponse> response = cancelTour(hostTokens.get("accessToken"), reservationId);
        CancelTourResponse body = response.getBody();
        Optional<Reservation> reservation = reservationRepo.findById(reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.reservationId()).isEqualTo(reservationId);
        assertThat(body.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(body.cancelledByUsername()).isEqualTo("host");
        assertThat(body.cancelTimestamp()).isNotNull();
        assertThat(reservation).isPresent();
        assertThat(reservation.get().getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
}
