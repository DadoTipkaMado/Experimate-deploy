package hr.tvz.experimate.experimate.domain.reservation;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.booking_request.dto.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.domain.booking_request.response.BookingRequestResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.CancelTourResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.EndTourResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.PresenceResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.ReservationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationIT extends AbstractIntegrationTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ResponseEntity<CancelTourResponse> cancelTour(String jwt, Integer reservationId) {
        return restTemplate.exchange(
                RESERVATION_URL + "/cancel-tour/" + reservationId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(jwt)),
                CancelTourResponse.class
        );
    }

    private ResponseEntity<PresenceResponse[]> getPresence(String jwt, Integer reservationId) {
        return restTemplate.exchange(
                RESERVATION_URL + "/" + reservationId + "/presence", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(jwt)),
                PresenceResponse[].class
        );
    }

    /** Sends a booking request for the given listing and immediately accepts it as the host. */
    private void bookAndAccept(String guestJwt, String hostJwt, Integer listingId) {
        Integer requestId = restTemplate.exchange(
                BOOKING_URL, HttpMethod.POST,
                new HttpEntity<>(new CreateBookingRequestDto(listingId), bearerHeaders(guestJwt)),
                BookingRequestResponse.class
        ).getBody().id();
        restTemplate.exchange(
                BOOKING_URL + "/accept/" + requestId, HttpMethod.PATCH,
                new HttpEntity<>(bearerHeaders(hostJwt)), Void.class
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

    @Test
    void getPresence_withTwoGuests_hostSeesAllThreeParticipants() {
        Map<String, String> hostTokens  = loginAndGetTokens("host");
        Map<String, String> guest1Tokens = loginAndGetTokens("guest1");
        Map<String, String> guest2Tokens = loginAndGetTokens("guest2");

        Integer listingId = createListing(hostTokens.get("accessToken"), 25, ChronoUnit.MINUTES, 3);
        bookAndAccept(guest1Tokens.get("accessToken"), hostTokens.get("accessToken"), listingId);
        bookAndAccept(guest2Tokens.get("accessToken"), hostTokens.get("accessToken"), listingId);

        Integer anyReservationId = reservationRepo.findAllByTourListing_Id(listingId).get(0).getId();

        ResponseEntity<PresenceResponse[]> response = getPresence(hostTokens.get("accessToken"), anyReservationId);
        List<PresenceResponse> presence = List.of(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(presence).hasSize(3);
        assertThat(presence).filteredOn(PresenceResponse::isHost).hasSize(1);
        assertThat(presence).filteredOn(p -> !p.isHost()).hasSize(2);
        assertThat(presence).extracting(PresenceResponse::username)
                .containsExactlyInAnyOrder("host", "guest1", "guest2");
    }

    @Test
    void getPresence_guest2UsingOwnReservationId_seesAllThreeParticipants() {
        Map<String, String> hostTokens  = loginAndGetTokens("host");
        Map<String, String> guest1Tokens = loginAndGetTokens("guest1");
        Map<String, String> guest2Tokens = loginAndGetTokens("guest2");

        Integer listingId = createListing(hostTokens.get("accessToken"), 25, ChronoUnit.MINUTES, 3);
        bookAndAccept(guest1Tokens.get("accessToken"), hostTokens.get("accessToken"), listingId);
        // capture guest1's reservation before guest2 books — safe to use findByTourListing_Id here (single result)
        bookAndAccept(guest2Tokens.get("accessToken"), hostTokens.get("accessToken"), listingId);

        Integer guest2ReservationId = reservationRepo.findAllByTourListing_Id(listingId)
                .stream()
                .filter(r -> r.getGuest().getUsername().equals("guest2"))
                .findFirst().orElseThrow().getId();

        ResponseEntity<PresenceResponse[]> response = getPresence(guest2Tokens.get("accessToken"), guest2ReservationId);
        List<PresenceResponse> presence = List.of(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(presence).hasSize(3);
        assertThat(presence).extracting(PresenceResponse::username)
                .containsExactlyInAnyOrder("host", "guest1", "guest2");
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
