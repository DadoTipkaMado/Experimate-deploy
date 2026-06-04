package hr.tvz.experimate.experimate.domain.booking_request;

import hr.tvz.experimate.experimate.domain.booking_request.dto.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.domain.booking_request.response.BookingRequestResponse;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/mine")
    public ResponseEntity<Page<BookingRequestResponse>> getMyRequests(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false, defaultValue = "incoming") String flowDirection,
            @RequestParam(required = false, defaultValue = "PENDING") BookingRequestStatus status,
            @RequestParam(required = false, defaultValue = "DESC") Sort.Direction requestDateDirection,
            @RequestParam(required = false, defaultValue = "ASC") Sort.Direction meetingDateDirection,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                bookingRequestService.getMyRequests(
                        userDetails.getId(), flowDirection, status, requestDateDirection, meetingDateDirection, pageable)
        );
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
