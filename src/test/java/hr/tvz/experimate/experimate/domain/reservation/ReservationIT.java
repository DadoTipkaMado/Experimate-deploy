package hr.tvz.experimate.experimate.domain.reservation;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.reservation.response.CancelTourResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.EndTourResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.PresenceResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.ReservationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

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
