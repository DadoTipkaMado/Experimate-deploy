package hr.tvz.experimate.experimate.domain.reservation;

import hr.tvz.experimate.experimate.domain.reservation.exception.GuestAlreadyBookedException;
import hr.tvz.experimate.experimate.domain.reservation.exception.IllegalReservationStateException;
import hr.tvz.experimate.experimate.domain.reservation.exception.ReservationNotFoundException;
import hr.tvz.experimate.experimate.shared.Constraints;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.domain.reservation.response.CancelTourResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.CheckInResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.EndTourResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.PresenceResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.ReservationResponse;
import hr.tvz.experimate.experimate.shared.TourListingDetails;
import hr.tvz.experimate.experimate.shared.UserDetails;
import hr.tvz.experimate.experimate.shared.event.*;
import hr.tvz.experimate.experimate.shared.util.DateTimeUtil;
import hr.tvz.experimate.experimate.domain.tour_listing.*;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.*;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.TourListingNotFoundException;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepo reservationRepo;
    private final UserRepo userRepo;
    private final TourListingRepo tourListingRepo;
    private final ApplicationEventPublisher publisher;

    public ReservationService(ReservationRepo reservationRepo,
                              UserRepo userRepo,
                              TourListingRepo tourListingRepo,
                              ApplicationEventPublisher publisher) {
        this.reservationRepo = reservationRepo;
        this.userRepo = userRepo;
        this.tourListingRepo = tourListingRepo;
        this.publisher = publisher;
    }

    @Transactional
    public ReservationResponse createReservation(Integer guestId, Integer listingId) {
        User guest = userRepo.findById(guestId)
                .orElseThrow(() -> {
                    log.warn("User not found with id {}", guestId);
                    return new UserNotFoundException(guestId);
                });

        TourListing listing = tourListingRepo.findById(listingId)
                .orElseThrow(() -> {
                    log.warn("Tour listing not found with id {}", listingId);
                    return new TourListingNotFoundException(listingId);
                });

        //TODO tweakaj da se ne moze rezervirati na isti dan kada je i meetingDate ili da iskoci prompt da se hosta pita je li mu problem pricekati
        if(listing.getMeetingDate().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Cannot book a listing whose meeting date is in the past.");

        if (!guestAvailableAtDate(guest, listing.getMeetingDate().toLocalDate())) {
            log.warn("Guest with id {} has already booked a listing on the date {}.",
                    guest.getId(), listing.getMeetingDate().toLocalDate());
            throw new GuestAlreadyBookedException(guest.getId());
        }

        Reservation reservation = reservationRepo.save(
                new Reservation(
                        guest,
                        listing
                ));
        publisher.publishEvent(new TourListingReservedEvent(
                listingId,
                new UpdateTourListingDto(null, null, true)
        ));

        log.info("Created reservation with id {}", reservation.getId());

        return createReservationResponse(reservation);
    }

    public Page<ReservationResponse> getMyReservations(Integer userId, String filter, Sort.Direction direction, String timeframe, Pageable pageable) {
        Sort sort = Sort.by(direction, "tourListing.meetingDate");
        Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        LocalDateTime now = LocalDateTime.now();

        // route to the correct query based on user role (guest/host) and timeframe (past/upcoming)
        Page<Reservation> results = switch (filter) {
            case "hosted" -> switch (timeframe) {
                case "past" -> reservationRepo.findAllByTourListing_Host_IdAndTourListing_MeetingDateBefore(userId, now, pageableWithSort);
                default     -> reservationRepo.findAllByTourListing_Host_IdAndTourListing_MeetingDateAfter(userId, now, pageableWithSort);
            };
            default -> switch (timeframe) {
                case "past" -> reservationRepo.findAllByGuest_IdAndTourListing_MeetingDateBefore(userId, now, pageableWithSort);
                default     -> reservationRepo.findAllByGuest_IdAndTourListing_MeetingDateAfter(userId, now, pageableWithSort);
            };
        };

        return results.map(this::createReservationResponse);
    }

    public List<ReservationResponse> getAllReservations() {
        return reservationRepo.findAll()
                .stream()
                .map(this::createReservationResponse)
                .toList();
    }

    public Optional<ReservationResponse> getReservationById(Integer id) {
        return reservationRepo.findById(id)
                .map(this::createReservationResponse);
    }

    public void deleteReservation(Integer id, Integer callerId) {
        Reservation reservation = reservationRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Reservation not found with id {}", id);
                    return new ReservationNotFoundException(
                            "Reservation with id %d could not be deleted. Not found.".formatted(id));
                });

        boolean isGuest = reservation.getGuest().getId().equals(callerId);
        boolean isHost = reservation.getTourListing().getHost().getId().equals(callerId);
        if (!isGuest && !isHost)
            throw new ForbiddenActionException("Only a participant of the reservation can delete it.");

        reservationRepo.deleteById(id);
        log.info("Reservation deleted with id {}", id);
    }

    /**
     * Checks a participant (guest or host) into a confirmed reservation if the check-in
     * window is open. If both participants have checked in, the reservation transitions
     * to ACTIVE status.
     *
     * @param userId        ID of the user attempting to check in
     * @param reservationId ID of the target reservation
     * @return {@link CheckInResponse} reflecting the updated check-in state
     * @throws ReservationNotFoundException     if no reservation exists with the given ID
     * @throws IllegalReservationStateException if the reservation is not CONFIRMED or the check-in window has not opened yet
     * @throws IllegalArgumentException         if the user is not a participant of the reservation
     */
    public CheckInResponse checkUserIn(Integer userId, Integer reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found with id {}", reservationId);
                    return new ReservationNotFoundException(reservationId);
                });

        // reservation must be in CONFIRMED status to allow check-in
        if (!reservation.getStatus().equals(ReservationStatus.CONFIRMED))
            throw new IllegalReservationStateException("Users can be checked into reservation only during 'CONFIRMED' phase");

        LocalDateTime meetingDateTime = reservation.getTourListing().getMeetingDate();
        long minutesUntilMeeting = ChronoUnit.MINUTES.between(LocalDateTime.now(), meetingDateTime);
        // check-in window: user must be within MINS_DIFF_TO_CHECK_IN_MIN minutes of the meeting
        if (minutesUntilMeeting > Constraints.ReservationConstraints.MINS_DIFF_TO_CHECK_IN_MIN)
            throw new IllegalReservationStateException(
                    "Check-in opens %d minutes before the meeting"
                            .formatted(Constraints.ReservationConstraints.MINS_DIFF_TO_CHECK_IN_MIN));

        // check in guest or host depending on which participant is calling
        if (reservation.getGuest().getId().equals(userId)) {
            if(reservation.isGuestCheckedIn()) {
                log.warn("Guest already checked in.");
                throw new IllegalReservationStateException("Guest already checked in.");
            }
            reservation.checkGuestIn();
            log.info("Guest with id {} checked in for reservation with id {}", userId, reservationId);
        } else if (reservation.getTourListing().getHost().getId().equals(userId)) {
            if (reservation.isHostCheckedIn()) {
                log.warn("Host already checked in.");
                throw new IllegalReservationStateException("Host already checked in.");
            }
            reservation.checkHostIn();
            log.info("Host with id {} checked in for reservation with id {}", userId, reservationId);
        } else {
            log.warn("Not a participant, cannot check in.");
            throw new IllegalArgumentException("User with id %d is not a participant of reservation with id %d"
                    .formatted(userId, reservationId));
        }

        if (reservation.bothCheckedIn()) {
            reservation.activate();
            log.info("Reservation with id {} activated.", reservationId);
        }

        reservationRepo.save(reservation);
        log.info("User with id {} checked in for reservation with id {}", userId, reservation.getId());

        return new CheckInResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.isGuestCheckedIn(),
                reservation.isHostCheckedIn(),
                reservation.getGuestCheckInTimestamp(),
                reservation.getHostCheckInTimestamp(),
                reservation.getStartTimestamp()
        );
    }

    public EndTourResponse endTour(Integer userId, Integer reservationId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id {}", userId);
                    return new UserNotFoundException(userId);
                });

        Reservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found with id {}", reservationId);
                    return new ReservationNotFoundException(reservationId);
                });

        if (!reservation.getStatus().equals(ReservationStatus.ACTIVE))
            throw new IllegalReservationStateException("Tour can be ended only during 'ACTIVE' phase.");

        if (!reservation.getGuest().getId().equals(userId) &&
                !reservation.getTourListing().getHost().getId().equals(user.getId())) {
            log.warn("Not a participant, cannot end tour.");
            throw new IllegalArgumentException("User with id %d is not a participant of reservation with id %d"
                    .formatted(userId, reservationId));
        }

        reservation.endBy(user);
        reservationRepo.save(reservation);
        log.info("Reservation with id {} has ended.", reservation.getId());

        return new EndTourResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getEndedBy().getUsername(),
                reservation.getEndTimestamp()
        );
    }

    public CancelTourResponse cancelTour(Integer userId, Integer reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found with id {}", reservationId);
                    return new ReservationNotFoundException(reservationId);
                });

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with id {}", userId);
                    return new UserNotFoundException(userId);
                });

        log.debug("Attempting to cancel Reservation with status {}.",  reservation.getStatus());

        if (!(reservation.getStatus().equals(ReservationStatus.CONFIRMED) || reservation.getStatus().equals(ReservationStatus.ACTIVE)))
            throw new IllegalReservationStateException("Reservation can be cancelled only during 'CONFIRMED' or 'ACTIVE' phase.");

        if (!reservation.getGuest().getId().equals(user.getId()) &&
                !reservation.getTourListing().getHost().getId().equals(user.getId())) {
            log.warn("Not a participant, cannot cancel tour.");
            throw new IllegalArgumentException("User with id %d is not a participant of reservation with id %d"
                    .formatted(userId, reservationId));
        }

        reservation.cancel();
        reservationRepo.save(reservation);
        log.info("Reservation with id {} has been cancelled by user with id {}",
                reservation.getId(),
                user.getId());

        return new CancelTourResponse(
                reservation.getId(),
                reservation.getStatus(),
                user.getUsername(),
                reservation.getCancelTimeStamp()
        );
    }

    private record ValidatedReservationData(User guest, TourListing listing) {
    }

    private boolean guestAvailableAtDate(User guest, LocalDate meetingDate) {
        return !reservationRepo.existsByGuestAndTourListing_MeetingDateBetween(
                guest,
                DateTimeUtil.getStartOfDay(meetingDate),
                DateTimeUtil.getEndOfDay(meetingDate)
        );
    }

    private void deleteReservationsByHostId(Integer hostId) {
        int count = reservationRepo.deleteAllByTourListing_Host_Id(hostId);
        log.info("Deleted {} reservations for host with id {}", count, hostId);
    }

    private void deleteReservationsByGuestId(Integer guestId) {
        int count = reservationRepo.deleteAllByGuest_Id(guestId);
        log.info("Deleted {} reservations for guest with id {}", count, guestId);
    }

    @EventListener
    void handleListingsDeletedForHost(TourListingsDeletedForHostEvent event) {
        Integer hostId = event.hostId();
        if (!reservationRepo.existsByTourListing_Host_Id(hostId)) {
            log.warn("Cannot find a single reservation for host with id {}", hostId);
            return;
        }

        deleteReservationsByHostId(hostId);
    }

    @EventListener
    void handleUserDeleted(UserDeletedEvent event) {
        Integer guestId = event.userId();
        if (!reservationRepo.existsByGuest_Id(guestId)) {
            log.warn("Could not find a single reservation for guest with id {}", guestId);
            return;
        }
        List<Integer> tourListingIds = reservationRepo.findTourListingIdsByGuestId(guestId);
        deleteReservationsByGuestId(guestId);

        publisher.publishEvent(new ReservationsDeletedEvent(tourListingIds));
    }

    @EventListener
    void handleBookingRequestAccepted(BookingRequestAcceptedEvent event) {
        createReservation(event.guestId(), event.listingId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void handleRatingCreatedEvent(RatingCreatedEvent event) {
        Integer raterId = event.raterId();
        Integer ratedId = event.ratedId();
        //default dto check
        Optional<Reservation> reservation = reservationRepo.findByGuest_IdAndTourListing_Host_IdAndStatus(
                raterId, ratedId, ReservationStatus.CLOSED);

        reservation.ifPresent(value -> value.setHostRated(true));

        if(reservation.isEmpty())
            reservation = reservationRepo.findByGuest_IdAndTourListing_Host_IdAndStatus(
                    ratedId, raterId, ReservationStatus.CLOSED);

        reservation.ifPresent(value -> value.setGuestRated(true));

        if(reservation.isEmpty()) {
            log.warn("No reservation found for given event parameters.");
            throw new ReservationNotFoundException("No reservation found for given event parameters.");
        }

        if(reservation.get().guestRated() && reservation.get().hostRated()){
            reservation.get().complete();
            log.debug("Both users have been rated after the tour.");
        }

        reservationRepo.save(reservation.get());
        log.info("Reservation/tour completed with both ratings (guest: {}, {}).", event.raterId(), event.ratedId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void handleTourListingDeletedEvent(TourListingDeletedEvent event) {
        Optional<Reservation> reservation = reservationRepo.findByTourListing_Id(event.listingId());

        if(reservation.isEmpty()) {
            log.debug("No reservation found for given event parameters.");
            return;
        }

        reservationRepo.deleteById(reservation.get().getId());
        log.info("Reservation deleted by id {}", reservation.get().getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void handleTourListingsDeletedEvent(TourListingsDeletedEvent event) {
        int count = reservationRepo.deleteAllByTourListing_IdIn(event.listingIds());
        log.info("Deleted {} reservations due to expired tour listings.", count);
    }

    @Scheduled(fixedRate = 1800000)
    public void expireClosedReservations() {
        log.debug("Attempting to proccess expired reservations.");
        List<Reservation> expired = reservationRepo.findAllByStatusAndEndTimestampBefore(
                ReservationStatus.CLOSED,
                LocalDateTime.now().minusHours(48)
        );
        expired.forEach(Reservation::expire);
        reservationRepo.saveAll(expired);
        log.info("Processed {} closed reservations.",  expired.size());
    }

    /**
     * Returns presence information for all participants of a reservation.
     *
     * <p>Only the guest or host of the reservation may call this method.
     *
     * @param callerId      ID of the authenticated user making the request
     * @param reservationId ID of the target reservation
     * @return list of {@link PresenceResponse} for the guest and host
     * @throws ReservationNotFoundException if no reservation exists with the given ID
     * @throws ForbiddenActionException     if the caller is not a participant of the reservation
     */
    public List<PresenceResponse> getPresence(Integer callerId, Integer reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        User guest = reservation.getGuest();
        User host = reservation.getTourListing().getHost();

        // only participants may view presence
        boolean isParticipant = guest.getId().equals(callerId) || host.getId().equals(callerId);
        if (!isParticipant)
            throw new ForbiddenActionException("Only participants of the reservation can view presence.");

        PresenceResponse guestPresence = new PresenceResponse(
                guest.getUsername(),
                guest.getFirstName(),
                guest.getLastName(),
                guest.getProfilePhotoFilename(),
                reservation.isGuestCheckedIn(),
                false
        );

        PresenceResponse hostPresence = new PresenceResponse(
                host.getUsername(),
                host.getFirstName(),
                host.getLastName(),
                host.getProfilePhotoFilename(),
                reservation.isHostCheckedIn(),
                true
        );

        return List.of(guestPresence, hostPresence);
    }

    private ReservationResponse createReservationResponse(Reservation reservation) {
        TourListing tourListing = reservation.getTourListing();
        User host = tourListing.getHost();
        User guest = reservation.getGuest();

        UserDetails hostDetails = new UserDetails(
                host.getFirstName(),
                host.getLastName(),
                host.getUsername()
        );

        TourListingDetails listingDetails = new TourListingDetails(
                tourListing.getId(),
                tourListing.getMeetingDate(),
                tourListing.getCity(),
                hostDetails
        );

        UserDetails guestDetails = new UserDetails(
                guest.getFirstName(),
                guest.getLastName(),
                guest.getUsername()
        );

        return new ReservationResponse(
                reservation.getId(),
                reservation.getDateOfReservation(),
                listingDetails,
                guestDetails,
                reservation.getStatus()
        );
    }
}
