package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.reservation.*;
import hr.tvz.experimate.experimate.model.reservation.response.CancelTourResponse;
import hr.tvz.experimate.experimate.model.reservation.response.CheckInResponse;
import hr.tvz.experimate.experimate.model.reservation.response.EndTourResponse;
import hr.tvz.experimate.experimate.model.reservation.response.PresenceResponse;
import hr.tvz.experimate.experimate.model.reservation.response.ReservationResponse;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/reservation")
@Validated
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/mine")
    public ResponseEntity<Page<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "joined") String filter,
            @RequestParam(required = false, defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false, defaultValue = "upcoming") String timeframe,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reservationService.getMyReservations(userDetails.getId(), filter, direction, timeframe, pageable));
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable @Positive Integer id) {
        return reservationService.getReservationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{reservationId}/presence")
    public ResponseEntity<List<PresenceResponse>> getPresence(@PathVariable @Positive Integer reservationId,
                                                              @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(reservationService.getPresence(userDetails.getId(), reservationId));
    }

    @PatchMapping(value = "/check-in/{reservationId}")
    public ResponseEntity<CheckInResponse> checkIn(@PathVariable @Positive Integer reservationId,
                                                   @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(reservationService.checkUserIn(userDetails.getId(), reservationId));
    }

    @PatchMapping(value = "/end-tour/{reservationId}")
    public ResponseEntity<EndTourResponse> endTour(@PathVariable @Positive Integer reservationId,
                                                   @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(reservationService.endTour(userDetails.getId(), reservationId));
    }

    @PatchMapping(value="/cancel-tour/{reservationId}")
    public ResponseEntity<CancelTourResponse> cancelTour(@PathVariable @Positive Integer reservationId,
                                                         @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(reservationService.cancelTour(userDetails.getId(), reservationId));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable @Positive Integer id,
                                                   @AuthenticationPrincipal AppUserDetails userDetails) {
        reservationService.deleteReservation(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
