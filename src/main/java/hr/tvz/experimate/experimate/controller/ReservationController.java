package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.reservation.ReservationResponse;
import hr.tvz.experimate.experimate.model.reservation.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/api/reservation")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAllReservations(){
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping(value="/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Integer id) {
        return reservationService.getReservationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value="/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Integer id){
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}
