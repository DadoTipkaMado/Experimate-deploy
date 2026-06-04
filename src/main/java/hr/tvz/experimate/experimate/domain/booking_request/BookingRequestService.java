package hr.tvz.experimate.experimate.domain.booking_request;

import hr.tvz.experimate.experimate.domain.booking_request.dto.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.domain.booking_request.exception.BookingAlreadyRequestedException;
import hr.tvz.experimate.experimate.domain.booking_request.exception.BookingRequestNotFoundException;
import hr.tvz.experimate.experimate.domain.booking_request.response.BookingRequestResponse;
import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.domain.reservation.exception.GuestAlreadyBookedException;
import hr.tvz.experimate.experimate.domain.tour_listing.TourListing;
import hr.tvz.experimate.experimate.domain.tour_listing.TourListingRepo;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.HostAlreadyTakenException;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.TourListingAlreadyReservedException;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.TourListingExpiredException;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.TourListingNotFoundException;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.shared.DetailsMapper;
import hr.tvz.experimate.experimate.shared.event.BookingRequestAcceptedEvent;
import hr.tvz.experimate.experimate.shared.event.BookingRequestCreatedEvent;
import hr.tvz.experimate.experimate.shared.event.BookingRequestDeclinedEvent;
import hr.tvz.experimate.experimate.shared.event.TourListingDeletedEvent;
import hr.tvz.experimate.experimate.shared.event.TourListingsDeletedEvent;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
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
    private final DetailsMapper detailsMapper;

    public BookingRequestService(BookingRequestRepo bookingRequestRepo,
                                 UserRepo userRepo,
                                 TourListingRepo tourListingRepo,
                                 ReservationRepo reservationRepo,
                                 ApplicationEventPublisher publisher,
                                 DetailsMapper detailsMapper) {
        this.bookingRequestRepo = bookingRequestRepo;
        this.userRepo = userRepo;
        this.tourListingRepo = tourListingRepo;
        this.reservationRepo = reservationRepo;
        this.publisher = publisher;
        this.detailsMapper = detailsMapper;
    }

    /**
     * Creates a PENDING booking request from a guest for the given listing and notifies the host.
     *
     * <p>Rejects the request if the guest or host is already committed elsewhere in the
     * meeting window, the listing has expired or is full, the guest is the host, or the
     * guest already has a pending request for the same listing.
     *
     * @param dto     request body carrying the target listing ID
     * @param guestId ID of the guest sending the request
     * @return the created {@link BookingRequestResponse}
     * @throws TourListingNotFoundException        if no listing exists with the given ID
     * @throws UserNotFoundException               if no guest exists with the given ID
     * @throws GuestAlreadyBookedException         if the guest is already booked in the meeting window
     * @throws HostAlreadyTakenException           if the host is committed to a different listing in the window
     * @throws TourListingExpiredException         if the listing's meeting date has already passed
     * @throws IllegalArgumentException            if the guest is the listing's host
     * @throws TourListingAlreadyReservedException if the listing is already full
     * @throws BookingAlreadyRequestedException    if the guest already has a pending request for the listing
     */
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
        List<ReservationStatus> blockingStatuses = List.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE);
        boolean isGuestAlreadyReserved = reservationRepo.existsByGuest_IdAndTourListing_MeetingDateBetweenAndStatusIn(
                guest.getId(),
                windowStart,
                windowEnd,
                blockingStatuses
        );
        // only block if the host is committed to a *different* listing in this window — same listing allows multiple guests
        boolean isHostAlreadyReserved = reservationRepo.existsByHostOnDifferentListingInWindow(
                listing.getHost().getId(),
                listingId,
                windowStart,
                windowEnd,
                blockingStatuses
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

        // listing is full when all guest slots are confirmed or active
        if (reservationRepo.countByTourListing_IdAndStatusIn(listingId, blockingStatuses) >= listing.getMaxGuests()) {
            log.warn("TourListing with id {} is full. Cannot create new BookingRequests for it.", listingId);
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

        publisher.publishEvent(new BookingRequestCreatedEvent(listing.getHost().getId(), guest.getUsername()));

        return createBookingRequestResponse(request);
    }

    /**
     * Returns a page of the user's booking requests in the given direction and status.
     *
     * @param userId               ID of the user whose requests are listed
     * @param flowDirection        {@code "outgoing"} for requests the user sent as guest, otherwise incoming requests on the user's listings
     * @param status               only requests in this status are returned
     * @param requestDateDirection sort direction on the request date
     * @param meetingDateDirection sort direction on the listing's meeting date
     * @param pageable             pagination settings
     * @return a page of {@link BookingRequestResponse}
     */
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

    /**
     * Finds a single booking request by ID.
     *
     * @param id the booking request ID
     * @return the {@link BookingRequestResponse} if found, otherwise empty
     */
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

    /**
     * Deletes a booking request. Only the guest who created it may delete it.
     *
     * @param id       the booking request ID
     * @param callerId ID of the authenticated user requesting deletion
     * @throws BookingRequestNotFoundException if no request exists with the given ID
     * @throws ForbiddenActionException        if the caller is not the guest who created the request
     */
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

    /**
     * Accepts a booking request on behalf of the listing's host, which creates the reservation
     * (via {@link BookingRequestAcceptedEvent}). If accepting fills the listing, all remaining
     * pending requests for that listing are auto-declined and their guests notified.
     *
     * @param acceptedId ID of the booking request being accepted
     * @param callerId   ID of the authenticated user (must be the listing's host)
     * @return the accepted {@link BookingRequestResponse}
     * @throws BookingRequestNotFoundException if no request exists with the given ID
     * @throws ForbiddenActionException        if the caller is not the listing's host
     */
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

        updateBookingRequest(acceptedId, BookingRequestStatus.ACCEPTED);

        long confirmedCount = reservationRepo.countByTourListing_IdAndStatusIn(
                acceptedRequest.getListing().getId(), List.of(ReservationStatus.CONFIRMED));

        // listing just became full — auto-decline all remaining pending requests
        if (confirmedCount == acceptedRequest.getListing().getMaxGuests()) {
            List<Integer> declinedIds = bookingRequestRepo
                    .findBookingRequestIdsByTourListingIdAndStatus(
                            acceptedRequest.getListing().getId(), BookingRequestStatus.PENDING);
            updateBookingRequests(declinedIds, BookingRequestStatus.DECLINED);
            if (!declinedIds.isEmpty()) {
                bookingRequestRepo.findGuestIdsByIdIn(declinedIds)
                        .forEach(guestId -> publisher.publishEvent(new BookingRequestDeclinedEvent(guestId)));
            }
        }

        log.info("Booking request accepted with id {}", acceptedId);

        return createBookingRequestResponse(acceptedRequest);
    }

    /**
     * Declines a booking request on behalf of the listing's host and notifies the guest.
     *
     * @param id       ID of the booking request being declined
     * @param callerId ID of the authenticated user (must be the listing's host)
     * @return the declined {@link BookingRequestResponse}
     * @throws BookingRequestNotFoundException if no request exists with the given ID
     * @throws ForbiddenActionException        if the caller is not the listing's host
     */
    public BookingRequestResponse declineBookingRequest(Integer id, Integer callerId) {
        BookingRequest request = bookingRequestRepo.findById(id)
                .orElseThrow(() -> new BookingRequestNotFoundException(id));

        if (!request.getListing().getHost().getId().equals(callerId))
            throw new ForbiddenActionException("Only the host can decline booking requests.");

        request = updateBookingRequest(id, BookingRequestStatus.DECLINED);
        log.info("Booking request declined with id {}", id);

        publisher.publishEvent(new BookingRequestDeclinedEvent(request.getGuest().getId()));

        return createBookingRequestResponse(request);
    }

    @TransactionalEventListener(phase= TransactionPhase.BEFORE_COMMIT)
    void handleTourListingDeletedEvent(TourListingDeletedEvent event) {
        int count = bookingRequestRepo.deleteAllByListing_IdIn(List.of(event.listingId()));
        log.info("Deleted {} booking request(s) for listing with id {}", count, event.listingId());
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
        return new BookingRequestResponse(
                request.getId(),
                request.getStatus(),
                request.getRequestDate(),
                detailsMapper.mapListingDetails(request.getListing()),
                detailsMapper.mapUserDetails(request.getGuest())
        );
    }
}
