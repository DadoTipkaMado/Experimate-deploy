package hr.tvz.experimate.experimate.model.booking_request;

import hr.tvz.experimate.experimate.model.booking_request.exception.BookingAlreadyRequestedException;
import hr.tvz.experimate.experimate.model.booking_request.exception.BookingRequestNotFoundException;
import hr.tvz.experimate.experimate.model.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.model.reservation.exception.GuestAlreadyBookedException;
import hr.tvz.experimate.experimate.model.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.model.shared.TourListingDetails;
import hr.tvz.experimate.experimate.model.shared.UserDetails;
import hr.tvz.experimate.experimate.model.shared.event.BookingRequestAcceptedEvent;
import hr.tvz.experimate.experimate.model.shared.event.TourListingDeletedEvent;
import hr.tvz.experimate.experimate.model.shared.event.TourListingsDeletedEvent;
import hr.tvz.experimate.experimate.model.tour_listing.*;
import hr.tvz.experimate.experimate.model.tour_listing.exception.HostAlreadyTakenException;
import hr.tvz.experimate.experimate.model.tour_listing.exception.TourListingAlreadyReservedException;
import hr.tvz.experimate.experimate.model.tour_listing.exception.TourListingExpiredException;
import hr.tvz.experimate.experimate.model.tour_listing.exception.TourListingNotFoundException;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookingRequestService {

    private static final Logger log = LoggerFactory.getLogger(BookingRequestService.class);

    private final BookingRequestRepo bookingRequestRepo;
    private final UserRepo userRepo;
    private final TourListingRepo tourListingRepo;
    private final ReservationRepo reservationRepo;
    private final ApplicationEventPublisher publisher;

    public BookingRequestService(BookingRequestRepo bookingRequestRepo,
                                 UserRepo userRepo,
                                 TourListingRepo tourListingRepo,
                                 ReservationRepo reservationRepo,
                                 ApplicationEventPublisher publisher) {
        this.bookingRequestRepo = bookingRequestRepo;
        this.userRepo = userRepo;
        this.tourListingRepo = tourListingRepo;
        this.reservationRepo = reservationRepo;
        this.publisher = publisher;
    }

    public BookingRequestResponse createBookingRequest(CreateBookingRequestDto dto, Integer guestId) {
        Integer listingId = dto.listingId();

        TourListing listing = tourListingRepo.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Listing with id {} not found", listingId);
                    return new TourListingNotFoundException(listingId);
                });

        User guest = userRepo.findById(guestId)
                .orElseThrow(() -> new UserNotFoundException(guestId));

        LocalDateTime windowStart = listing.getMeetingDate().minusHours(12);
        LocalDateTime windowEnd = listing.getMeetingDate().plusHours(12);
        boolean isGuestAlreadyReserved = reservationRepo.existsByGuest_IdAndTourListing_MeetingDateBetween(
                guest.getId(),
                windowStart,
                windowEnd
        );
        boolean isHostAlreadyReserved = reservationRepo.existsByTourListing_Host_IdAndTourListing_MeetingDateBetween(
                listing.getHost().getId(),
                windowStart,
                windowEnd
        );
        if(isGuestAlreadyReserved){
            log.warn("Guest with id {} already reserved for date {}", guest.getId(), listing.getMeetingDate());
            throw new GuestAlreadyBookedException(guest.getId());
        }
        if(isHostAlreadyReserved){
            log.warn("Host with id {} already reserved for date {}", listing.getHost().getId(), listing.getMeetingDate());
            throw new HostAlreadyTakenException(listing.getHost().getId());
        }

        if(listing.getMeetingDate().isBefore(LocalDateTime.now())) {
            log.warn("Listing with id {} has been expired", listingId);
            throw new TourListingExpiredException(listing.getId());
        }

        if (guestId.equals(listing.getHost().getId())) {
            log.warn("Guest id {} matches host id — cannot send booking request to yourself", guestId);
            throw new IllegalArgumentException("Guest cannot send a booking request to themselves.");
        }

        if(listing.isReserved()) {
            log.warn("TourListing with id {} is already reserved. Cannot create new BookingRequests for it.", listingId);
            throw new TourListingAlreadyReservedException(listing.getId());
        }

        if(bookingRequestRepo.existsByGuestIdAndListingIdAndStatus(
                guestId,
                listingId,
                BookingRequestStatus.PENDING)
        ) {
            log.warn("Guest with id {} already has a pending request for listing with id {}.", guestId, listingId);
            throw new BookingAlreadyRequestedException(guestId, listingId);
        }

        BookingRequest request = bookingRequestRepo.save(new BookingRequest(guest, listing));
        log.info("Created booking request with id {}", request.getId());

        return createBookingRequestResponse(request);
    }

    public Page<BookingRequestResponse> getMyRequests(
            Integer userId,
            String flowDirection,
            BookingRequestStatus status,
            Sort.Direction requestDateDirection,
            Sort.Direction meetingDateDirection,
            Pageable pageable) {

        Sort sort = Sort.by(
                Sort.Order.by("requestDate").with(requestDateDirection),
                Sort.Order.by("listing.meetingDate").with(meetingDateDirection)
        );
        Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // route to the correct query based on whether the user is acting as guest or host
        Page<BookingRequest> results = "outgoing".equalsIgnoreCase(flowDirection)
                ? bookingRequestRepo.findAllByGuest_IdAndStatus(userId, status, pageableWithSort)
                : bookingRequestRepo.findAllByListing_Host_IdAndStatus(userId, status, pageableWithSort);

        return results.map(this::createBookingRequestResponse);
    }

    public List<BookingRequestResponse> getAllBookingRequests() {
        return bookingRequestRepo.findAll()
                .stream()
                .map(this::createBookingRequestResponse)
                .toList();
    }

    public Optional<BookingRequestResponse> getBookingRequestById(Integer id) {
        Optional<BookingRequest> request = bookingRequestRepo.findById(id);

        return request.map(this::createBookingRequestResponse);
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

    public void deleteBookingRequest(Integer id, Integer callerId) {
        BookingRequest request = bookingRequestRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Booking request not found with id {}", id);
                    return new BookingRequestNotFoundException(
                            "Booking request with id %d could not be deleted. Not found.".formatted(id));
                });

        if (!request.getGuest().getId().equals(callerId))
            throw new ForbiddenActionException("Only the guest who created the request can delete it.");

        bookingRequestRepo.deleteById(id);
        log.info("Booking request deleted with id {}", id);
    }

    @Transactional
    public BookingRequestResponse acceptBookingRequest(Integer acceptedId, Integer callerId) {
        BookingRequest acceptedRequest = bookingRequestRepo.findById(acceptedId)
                .orElseThrow(() -> new BookingRequestNotFoundException(acceptedId));

        if (!acceptedRequest.getListing().getHost().getId().equals(callerId))
            throw new ForbiddenActionException("Only the host can accept booking requests.");

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

        return createBookingRequestResponse(acceptedRequest);
    }

    public BookingRequestResponse declineBookingRequest(Integer id, Integer callerId) {
        BookingRequest request = bookingRequestRepo.findById(id)
                .orElseThrow(() -> new BookingRequestNotFoundException(id));

        if (!request.getListing().getHost().getId().equals(callerId))
            throw new ForbiddenActionException("Only the host can decline booking requests.");

        request = updateBookingRequest(id, BookingRequestStatus.DECLINED);
        log.info("Booking request declined with id {}", id);

        return createBookingRequestResponse(request);
    }

    @TransactionalEventListener(phase= TransactionPhase.BEFORE_COMMIT)
    void handleTourListingDeletedEvent(TourListingDeletedEvent event) {
        Optional<BookingRequest> request = bookingRequestRepo.findByListing_Id(event.listingId());

        if(request.isEmpty()) {
            log.debug("No booking requests for given event parameters");
            return;
        }

        bookingRequestRepo.deleteById(request.get().getId());
        log.info("Booking request deleted with id {}", request.get().getId());
    }

    @TransactionalEventListener(phase= TransactionPhase.BEFORE_COMMIT)
    void handleTourListingsDeletedEvent(TourListingsDeletedEvent event) {
        int count = bookingRequestRepo.deleteAllByListing_IdIn(event.listingIds());
        log.info("Deleted {} Booking Requests due to expired Tour Listings.", count);
    }

    //Batch updating
    private void updateBookingRequests(List<Integer> ids, BookingRequestStatus status) {
        Integer updatedCount = bookingRequestRepo.updateStatusByIds(ids, status);
        log.info("Updated {}/{} BookingRequest statuses to DECLINED.", updatedCount, ids.size());
    }

    private BookingRequestResponse createBookingRequestResponse(BookingRequest request){
        UserDetails hostDetails = new UserDetails(
                request.getListing().getHost().getFirstName(),
                request.getListing().getHost().getLastName(),
                request.getListing().getHost().getUsername()
        );

        UserDetails guestDetails = new UserDetails(
                request.getGuest().getFirstName(),
                request.getGuest().getLastName(),
                request.getGuest().getUsername()
        );

        TourListingDetails listingDetails = new TourListingDetails(
                request.getListing().getId(),
                request.getListing().getMeetingDate(),
                request.getListing().getCity(),
                hostDetails
        );

        return new BookingRequestResponse(
                request.getId(),
                request.getStatus(),
                request.getRequestDate(),
                listingDetails,
                guestDetails
        );
    }
}
