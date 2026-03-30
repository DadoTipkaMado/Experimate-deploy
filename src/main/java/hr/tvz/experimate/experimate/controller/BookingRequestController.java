package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.booking_request.BookingRequest;
import hr.tvz.experimate.experimate.model.booking_request.BookingRequestService;
import hr.tvz.experimate.experimate.model.booking_request.CreateBookingRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/booking-request")
public class BookingRequestController {

    private final BookingRequestService bookingRequestService;

    public BookingRequestController(BookingRequestService bookingRequestService) {
        this.bookingRequestService = bookingRequestService;
    }

    @PostMapping
    public ResponseEntity<BookingRequest> createBookingRequest(@RequestBody CreateBookingRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                bookingRequestService.createBookingRequest(dto)
        );
    }

    @GetMapping
    public ResponseEntity<List<BookingRequest>> getAllBookingRequests() {
        return ResponseEntity.ok(bookingRequestService.getAllBookingRequests());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<BookingRequest> getBookingRequestById(@PathVariable Integer id) {
        return bookingRequestService.getBookingRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteBookingRequest(@PathVariable Integer id) {
        bookingRequestService.deleteBookingRequest(id);
        return ResponseEntity.noContent().build();
    }
}
