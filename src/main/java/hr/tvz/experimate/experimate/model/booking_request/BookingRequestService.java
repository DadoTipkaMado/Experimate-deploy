package hr.tvz.experimate.experimate.model.booking_request;

import hr.tvz.experimate.experimate.model.shared.event.BookingRequestAcceptedEvent;
import hr.tvz.experimate.experimate.model.shared.event.BookingRequestDeclinedEvent;
import hr.tvz.experimate.experimate.model.tour_listing.TourListing;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingAlreadyReservedException;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingNotFoundException;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingRepo;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookingRequestService {

    private static final Logger log = LoggerFactory.getLogger(BookingRequestService.class);

    private final BookingRequestRepo bookingRequestRepo;
    private final UserRepo userRepo;
    private final TourListingRepo tourListingRepo;
    private final ApplicationEventPublisher publisher;

    public BookingRequestService(BookingRequestRepo bookingRequestRepo,
                                 UserRepo userRepo,
                                 TourListingRepo tourListingRepo,
                                 ApplicationEventPublisher publisher) {
        this.bookingRequestRepo = bookingRequestRepo;
        this.userRepo = userRepo;
        this.tourListingRepo = tourListingRepo;
        this.publisher = publisher;
    }

    public BookingRequest createBookingRequest(CreateBookingRequestDto dto) {
        TourListing listing = tourListingRepo.findById(dto.listingId())
                .orElseThrow(() -> new TourListingNotFoundException(dto.listingId()));
        if(listing.isReserved()) {
            log.warn("TourListing with id {} is already reserved. Cannot create new BookingRequests for it.");
            throw new TourListingAlreadyReservedException(listing.getId());
        }

        User guest = userRepo.findById(dto.guestId())
                .orElseThrow(() -> new UserNotFoundException(dto.guestId()));

        if (dto.guestId().equals(listing.getHost().getId())) {
            log.warn("Guest id {} matches host id — cannot send booking request to yourself", dto.guestId());
            throw new IllegalArgumentException("Guest cannot send a booking request to themselves.");
        }

        BookingRequest request = bookingRequestRepo.save(new BookingRequest(guest, listing));
        log.info("Created booking request with id {}", request.getId());
        return request;
    }

    public List<BookingRequest> getAllBookingRequests() {
        return bookingRequestRepo.findAll();
    }

    public Optional<BookingRequest> getBookingRequestById(Integer id) {
        return bookingRequestRepo.findById(id);
    }

    //Cannot be updated directly from api endpoint, used only internally
    private BookingRequest updateBookingRequest(Integer id, BookingRequestStatus status) {
        BookingRequest request = bookingRequestRepo.findById(id)
                .orElseThrow(() -> new BookingRequestNotFoundException(id));

        request.setStatus(status);
        log.info("Updated booking request status with id {} to {} ",
                request.getId(),
                request.getStatus());
        return bookingRequestRepo.save(request);
    }

    public void deleteBookingRequest(Integer id) {
        if (bookingRequestRepo.existsById(id)) {
            bookingRequestRepo.deleteById(id);
            log.info("Booking request deleted with id {}", id);
        } else {
            log.warn("Booking request not found with id {}", id);
            throw new BookingRequestNotFoundException(
                    "Booking request with id %d could not be deleted. Not found.".formatted(id)
            );
        }
    }

    @Transactional
    public BookingRequestResponse acceptBookingRequest(Integer acceptedId) {
        BookingRequest acceptedRequest = bookingRequestRepo.findById(acceptedId)
                .orElseThrow(() -> new BookingRequestNotFoundException(acceptedId));

        publisher.publishEvent(new BookingRequestAcceptedEvent(
                acceptedRequest.getGuest().getId(),
                acceptedRequest.getListing().getId()
        ));

        //Updating status of the accepted acceptedRequest
        updateBookingRequest(acceptedId, BookingRequestStatus.ACCEPTED);

        //Updating the rest (automatically decline)
        List<Integer> declinedIds =
                bookingRequestRepo
                        .findBookingRequestIdsByTourListingId(acceptedRequest.getListing().getId())
                        .stream()
                        .filter(declinedId -> !declinedId.equals(acceptedId))
                        .toList();
        updateBookingRequests(declinedIds, BookingRequestStatus.DECLINED);

        log.info("Booking request accepted with id {}", acceptedId);

        return new  BookingRequestResponse(
                acceptedId,
                BookingRequestStatus.ACCEPTED);
    }

    public BookingRequestResponse declineBookingRequest(Integer id) {
        updateBookingRequest(id, BookingRequestStatus.DECLINED);
        log.info("Booking request declined with id {}", id);

        return new BookingRequestResponse(
                id,
                BookingRequestStatus.DECLINED
        );
    }

    //Batch updating
    private void updateBookingRequests(List<Integer> ids, BookingRequestStatus status) {
        Integer updatedCount = bookingRequestRepo.updateStatusByIds(ids, status);
        log.info("Updated {}/{} BookingRequest statuses to DECLINED.", updatedCount, ids.size());
    }
}
