package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.booking_request.BookingRequestResponse;
import hr.tvz.experimate.experimate.model.booking_request.BookingRequestService;
import hr.tvz.experimate.experimate.model.booking_request.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/booking-request")
@Validated
public class BookingRequestController {

    private final BookingRequestService bookingRequestService;

    public BookingRequestController(BookingRequestService bookingRequestService) {
        this.bookingRequestService = bookingRequestService;
    }


    @PostMapping
    public ResponseEntity<BookingRequestResponse> createBookingRequest(@Valid @RequestBody CreateBookingRequestDto dto,
                                                                        @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                bookingRequestService.createBookingRequest(dto, userDetails.getId())
        );
    }

    @GetMapping
    public ResponseEntity<List<BookingRequestResponse>> getAllBookingRequests() {
        return ResponseEntity.ok(bookingRequestService.getAllBookingRequests());
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<BookingRequestResponse> getBookingRequestById(@Positive @PathVariable Integer id) {
        return bookingRequestService.getBookingRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteBookingRequest(@Positive @PathVariable Integer id,
                                                      @AuthenticationPrincipal AppUserDetails userDetails) {
        bookingRequestService.deleteBookingRequest(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/accept/{id}")
    public ResponseEntity<BookingRequestResponse> acceptBookingRequest(@Positive @PathVariable Integer id,
                                                                        @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(bookingRequestService.acceptBookingRequest(id, userDetails.getId()));
    }

    @PatchMapping(value = "/decline/{id}")
    public ResponseEntity<BookingRequestResponse> declineBookingRequest(@Positive @PathVariable Integer id,
                                                                         @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(bookingRequestService.declineBookingRequest(id, userDetails.getId()));
    }
}
