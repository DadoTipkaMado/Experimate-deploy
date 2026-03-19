package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.reservation.CreateReservationDto;
import hr.tvz.experimate.experimate.model.reservation.Reservation;
import hr.tvz.experimate.experimate.model.reservation.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(value="/api/reservation")
class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<Reservation> createReservation(@RequestBody CreateReservationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                reservationService.createReservation(dto)
        );
    }

    @GetMapping
    public ResponseEntity<List<Reservation>> getAllReservations(){
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping(value="/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Integer id) {
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
