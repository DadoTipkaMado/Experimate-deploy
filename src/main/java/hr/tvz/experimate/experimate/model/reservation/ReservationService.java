package hr.tvz.experimate.experimate.model.reservation;

import hr.tvz.experimate.experimate.model.shared.TourListingDetails;
import hr.tvz.experimate.experimate.model.shared.UserDetails;
import hr.tvz.experimate.experimate.model.shared.event.*;
import hr.tvz.experimate.experimate.model.shared.util.DateTimeUtil;
import hr.tvz.experimate.experimate.model.tour_listing.*;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    protected ReservationResponse createReservation(Integer guestId, Integer listingId) {
        User guest = userRepo.findById(guestId)
                .orElseThrow(() -> {
                    log.warn("User not found with id {}", guestId);
                    return new UserNotFoundException(guestId);
                });

        TourListing listing = tourListingRepo.findById(guestId)
                .orElseThrow(() -> {
                    log.warn("User not found with id {}", listingId);
                    return new TourListingNotFoundException(listingId);
                });

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

    public void deleteReservation(Integer id) {
        if (reservationRepo.existsById(id)) {
            reservationRepo.deleteById(id);
            log.info("Reservation deleted with id {}", id);
        } else {
            log.warn("Reservation not found with id {}", id);
            throw new ReservationNotFoundException(
                    "Reservation with id %d could not be deleted. Not found.".formatted(id
                    ));
        }
    }

    public CheckInResponse checkUserIn(Integer userId, Integer reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found with id {}", reservationId);
                    return new ReservationNotFoundException(reservationId);
                });

        if (!reservation.getStatus().equals(ReservationStatus.CONFIRMED))
            throw new IllegalReservationStateException("Users can be checked into reservation only during 'CONFIRMED' phase");

        if (reservation.getGuest().getId().equals(userId)
                && !reservation.isGuestCheckedIn()) {
            reservation.checkGuestIn();
            log.info("Guest with id {} checked in for reservation with id {}", userId, reservationId);
        } else if (reservation.getTourListing().getHost().getId().equals(userId)
                && !reservation.isHostCheckedIn()) {
            reservation.checkHostIn();
            log.info("Host with id {} checked in for reservation with id {}", userId, reservationId);
        } else {
            log.warn("Not a participant, cannot check in.");
            throw new IllegalArgumentException("User with id %d is not a participant of reservation with id %d"
                    .formatted(userId, reservationId));
        }

        if (reservation.bothCheckedIn()) {
            reservation.activate();
            log.info("Reservation with id {} activated.",  reservationId);
        }

        reservationRepo.save(reservation);
        log.info("User with id {} checked in for reservation with id {}", userId,  reservation.getId());

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

        if (!reservation.getStatus().equals(ReservationStatus.CONFIRMED))
            throw new IllegalReservationStateException("Reservation can be cancelled only during 'CONFIRMED' phase.");

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
                guestDetails
        );
    }
}
