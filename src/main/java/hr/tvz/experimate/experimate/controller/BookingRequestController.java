package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.booking_request.BookingRequest;
import hr.tvz.experimate.experimate.model.booking_request.BookingRequestResponse;
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
    public ResponseEntity<BookingRequestResponse> createBookingRequest(@RequestBody CreateBookingRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                bookingRequestService.createBookingRequest(dto)
        );
    }

    @GetMapping
    public ResponseEntity<List<BookingRequestResponse>> getAllBookingRequests() {
        return ResponseEntity.ok(bookingRequestService.getAllBookingRequests());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<BookingRequestResponse> getBookingRequestById(@PathVariable Integer id) {
        return bookingRequestService.getBookingRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteBookingRequest(@PathVariable Integer id) {
        bookingRequestService.deleteBookingRequest(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/accept/{id}")
    public ResponseEntity<BookingRequestResponse> acceptBookingRequest(@PathVariable Integer id) {
        return ResponseEntity
                .ok(bookingRequestService.acceptBookingRequest(id));
    }

    @PatchMapping(value = "/decline/{id}")
    public ResponseEntity<BookingRequestResponse> declineBookingRequest(@PathVariable Integer id) {
        return ResponseEntity
                .ok(bookingRequestService.declineBookingRequest(id));
    }
}
