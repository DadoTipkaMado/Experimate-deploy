package hr.tvz.experimate.experimate.domain.reservation;

import hr.tvz.experimate.experimate.domain.reservation.exception.IllegalReservationStateException;
import hr.tvz.experimate.experimate.domain.reservation.exception.ReservationNotFoundException;
import hr.tvz.experimate.experimate.domain.reservation.response.CancelTourResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.CheckInResponse;
import hr.tvz.experimate.experimate.domain.reservation.response.EndTourResponse;
import hr.tvz.experimate.experimate.domain.tour_listing.TourListing;
import hr.tvz.experimate.experimate.domain.tour_listing.TourListingRepo;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.shared.DetailsMapper;
import hr.tvz.experimate.experimate.shared.event.TourStartedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepo reservationRepo;
    @Mock private UserRepo userRepo;
    @Mock private TourListingRepo tourListingRepo;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private DetailsMapper detailsMapper;

    @InjectMocks private ReservationService service;

    // ──────────────── checkUserIn ────────────────

    @Test
    void checkUserIn_throwsIfReservationNotFound() {
        when(reservationRepo.findById(1)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class,
                () -> service.checkUserIn(10, 1));
    }

    @Test
    void checkUserIn_throwsIfStatusNotConfirmed() {
        Reservation reservation = mock(Reservation.class);
        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.ACTIVE);

        assertThrows(IllegalReservationStateException.class,
                () -> service.checkUserIn(10, 1));
    }

    @Test
    void checkUserIn_throwsIfStatusIsExpired() {
        Reservation reservation = mock(Reservation.class);
        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.EXPIRED);

        assertThrows(IllegalReservationStateException.class,
                () -> service.checkUserIn(10, 1));
    }

    @Test
    void checkUserIn_throwsIfMeetingTooFarAway() {
        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        // 60 minutes is well outside the 30-minute check-in window
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(60));

        assertThrows(IllegalReservationStateException.class,
                () -> service.checkUserIn(10, 1));
    }

    @Test
    void checkUserIn_throwsIfJustOutsideCheckInWindow() {
        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        // safely outside the 30-minute window — avoids truncation issues with ChronoUnit.MINUTES.between
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(45));

        assertThrows(IllegalReservationStateException.class,
                () -> service.checkUserIn(10, 1));
    }

    @Test
    void checkUserIn_guestSucceeds_59MinutesAfterMeetingStart() {
        Integer guestId = 10;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        // meeting was 59 minutes ago — minutesUntilMeeting = -59, so the window check passes
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().minusMinutes(59));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(guestId);
        // first call: guard check (not yet checked in); second call: building the response (now checked in)
        when(reservation.isGuestCheckedIn()).thenReturn(false, true);
        // host not yet in → tryAutoStartTour short-circuits; response sources host check-in from listing
        when(listing.isHostCheckedIn()).thenReturn(false);

        CheckInResponse response = service.checkUserIn(guestId, 1);

        verify(reservation).checkGuestIn();
        verify(reservationRepo).save(reservation);
        assertTrue(response.guestCheckedIn());
        assertFalse(response.hostCheckedIn());
    }

    @Test
    void checkUserIn_guestHappyPath_checksGuestInAndKeepsStatusConfirmed() {
        Integer guestId = 10;
        Integer hostId = 99;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);
        User host = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(guestId);
        when(reservation.isGuestCheckedIn()).thenReturn(false);
        // host not yet in → tryAutoStartTour short-circuits
        when(listing.isHostCheckedIn()).thenReturn(false);

        CheckInResponse response = service.checkUserIn(guestId, 1);

        verify(reservation).checkGuestIn();
        verify(listing, never()).checkHostIn();
        verify(reservation, never()).activate();
        verify(reservationRepo).save(reservation);
    }

    @Test
    void checkUserIn_hostHappyPath_checksHostInAndKeepsStatusConfirmed() {
        Integer guestId = 10;
        Integer hostId = 99;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);
        User host = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(guestId);
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(hostId);
        // host not yet checked in → guard passes, then after checkHostIn no guests in → no auto-start
        when(listing.isHostCheckedIn()).thenReturn(false);

        service.checkUserIn(hostId, 1);

        verify(listing).checkHostIn();
        verify(reservation, never()).checkGuestIn();
        verify(reservation, never()).activate();
        verify(reservationRepo).save(reservation);
    }

    @Test
    void checkUserIn_lastGuestTriggersAutoStart() {
        Integer guestId = 10;
        Integer listingId = 5;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(guestId);
        when(reservation.isGuestCheckedIn()).thenReturn(false);
        // tour not yet started, host already in
        when(listing.isTourStarted()).thenReturn(false);
        when(listing.isHostCheckedIn()).thenReturn(true);
        when(listing.getId()).thenReturn(listingId);
        // all 1 confirmed guest is now checked in → auto-start condition met
        when(reservationRepo.countByTourListing_IdAndStatus(listingId, ReservationStatus.CONFIRMED)).thenReturn(1L);
        when(reservationRepo.countByListingIdAndStatusAndGuestCheckedIn(listingId, ReservationStatus.CONFIRMED, true)).thenReturn(1L);

        service.checkUserIn(guestId, 1);

        verify(reservation).checkGuestIn();
        verify(listing).startTour();
        verify(tourListingRepo).save(listing);
        verify(publisher).publishEvent(any(TourStartedEvent.class));
    }

    @Test
    void checkUserIn_throwsIfGuestAlreadyCheckedIn() {
        Integer guestId = 10;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(guestId);
        when(reservation.isGuestCheckedIn()).thenReturn(true);

        assertThrows(IllegalReservationStateException.class,
                () -> service.checkUserIn(guestId, 1));

        verify(reservation, never()).checkGuestIn();
        verify(reservationRepo, never()).save(reservation);
    }

    @Test
    void checkUserIn_throwsIfHostAlreadyCheckedIn() {
        Integer guestId = 10;
        Integer hostId = 99;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);
        User host = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(guestId);
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(hostId);
        // host already checked in → guard throws
        when(listing.isHostCheckedIn()).thenReturn(true);

        assertThrows(IllegalReservationStateException.class,
                () -> service.checkUserIn(hostId, 1));

        verify(listing, never()).checkHostIn();
        verify(reservationRepo, never()).save(reservation);
    }

    @Test
    void checkUserIn_throwsIfCallerNotParticipant() {
        Integer outsiderId = 42;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);
        User host = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(10);
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(99);

        assertThrows(IllegalArgumentException.class,
                () -> service.checkUserIn(outsiderId, 1));

        verify(reservation, never()).checkGuestIn();
        verify(listing, never()).checkHostIn();
    }

    @Test
    void checkUserIn_hostNotCheckedIn_doesNotAutoStart() {
        Integer guestId = 10;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(guestId);
        when(reservation.isGuestCheckedIn()).thenReturn(false);
        when(listing.isTourStarted()).thenReturn(false);
        // host not yet checked in → tryAutoStartTour short-circuits before querying counts
        when(listing.isHostCheckedIn()).thenReturn(false);

        service.checkUserIn(guestId, 1);

        verify(listing, never()).startTour();
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void checkUserIn_someGuestsNotCheckedIn_doesNotAutoStart() {
        Integer guestId = 10;
        Integer listingId = 5;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(guestId);
        when(reservation.isGuestCheckedIn()).thenReturn(false);
        when(listing.isTourStarted()).thenReturn(false);
        when(listing.isHostCheckedIn()).thenReturn(true);
        when(listing.getId()).thenReturn(listingId);
        // 3 confirmed guests, only 1 checked in → condition not met
        when(reservationRepo.countByTourListing_IdAndStatus(listingId, ReservationStatus.CONFIRMED)).thenReturn(3L);
        when(reservationRepo.countByListingIdAndStatusAndGuestCheckedIn(listingId, ReservationStatus.CONFIRMED, true)).thenReturn(1L);

        service.checkUserIn(guestId, 1);

        verify(listing, never()).startTour();
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void checkUserIn_lateGuestAfterManualStart_activatesImmediately() {
        Integer guestId = 10;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User guest = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getMeetingDate()).thenReturn(LocalDateTime.now().plusMinutes(10));
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(guestId);
        when(reservation.isGuestCheckedIn()).thenReturn(false);
        // tour already started by host killswitch → late guest activates immediately
        when(listing.isTourStarted()).thenReturn(true);

        service.checkUserIn(guestId, 1);

        verify(reservation).checkGuestIn();
        verify(reservation).activate();
        // tryAutoStartTour short-circuits because tour is already started
        verify(listing, never()).startTour();
    }

    @Test
    void handleTourStartedEvent_activatesCheckedInConfirmedReservations() {
        Integer listingId = 5;

        Reservation checkedInConfirmed = mock(Reservation.class);
        Reservation notCheckedIn = mock(Reservation.class);
        Reservation alreadyActive = mock(Reservation.class);

        when(reservationRepo.findAllByTourListing_Id(listingId))
                .thenReturn(List.of(checkedInConfirmed, notCheckedIn, alreadyActive));

        // only this one meets the condition
        when(checkedInConfirmed.isGuestCheckedIn()).thenReturn(true);
        when(checkedInConfirmed.getStatus()).thenReturn(ReservationStatus.CONFIRMED);

        when(notCheckedIn.isGuestCheckedIn()).thenReturn(false);

        when(alreadyActive.isGuestCheckedIn()).thenReturn(true);
        when(alreadyActive.getStatus()).thenReturn(ReservationStatus.ACTIVE);

        service.handleTourStartedEvent(new TourStartedEvent(listingId));

        verify(checkedInConfirmed).activate();
        verify(reservationRepo).save(checkedInConfirmed);
        verify(notCheckedIn, never()).activate();
        verify(alreadyActive, never()).activate();
    }

    // ──────────────── endTour ────────────────

    @Test
    void endTour_throwsIfUserNotFound() {
        when(userRepo.findById(10)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.endTour(10, 1));
    }

    @Test
    void endTour_throwsIfReservationNotFound() {
        when(userRepo.findById(10)).thenReturn(Optional.of(mock(User.class)));
        when(reservationRepo.findById(1)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class,
                () -> service.endTour(10, 1));
    }

    @Test
    void endTour_throwsIfStatusNotActive() {
        Reservation reservation = mock(Reservation.class);

        when(userRepo.findById(10)).thenReturn(Optional.of(mock(User.class)));
        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);

        assertThrows(IllegalReservationStateException.class,
                () -> service.endTour(10, 1));

        verify(reservation, never()).endBy(any());
    }

    @Test
    void endTour_throwsIfCallerNotParticipant() {
        Integer outsiderId = 42;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User outsider = mock(User.class);
        User guest = mock(User.class);
        User host = mock(User.class);

        when(userRepo.findById(outsiderId)).thenReturn(Optional.of(outsider));
        when(outsider.getId()).thenReturn(outsiderId);
        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.ACTIVE);
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(10);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(99);

        assertThrows(IllegalArgumentException.class,
                () -> service.endTour(outsiderId, 1));

        verify(reservation, never()).endBy(any());
    }

    @Test
    void endTour_happyPath_closesReservation() {
        Integer userId = 10;

        Reservation reservation = mock(Reservation.class);
        User caller = mock(User.class);
        User guest = mock(User.class);

        when(userRepo.findById(userId)).thenReturn(Optional.of(caller));
        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(reservation.getStatus()).thenReturn(ReservationStatus.ACTIVE);
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(userId);
        // response builder reads the resulting endedBy
        when(reservation.getEndedBy()).thenReturn(caller);
        when(caller.getUsername()).thenReturn("david");

        EndTourResponse response = service.endTour(userId, 1);

        verify(reservation).endBy(caller);
        verify(reservationRepo).save(reservation);
        assertEquals("david", response.endedByUsername());
    }

    // ──────────────── cancelTour ────────────────

    @Test
    void cancelTour_throwsIfReservationNotFound() {
        when(reservationRepo.findById(1)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class,
                () -> service.cancelTour(10, 1));
    }

    @Test
    void cancelTour_throwsIfUserNotFound() {
        when(reservationRepo.findById(1)).thenReturn(Optional.of(mock(Reservation.class)));
        when(userRepo.findById(10)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.cancelTour(10, 1));
    }

    @Test
    void cancelTour_throwsIfStatusNotCancellable() {
        Reservation reservation = mock(Reservation.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(userRepo.findById(10)).thenReturn(Optional.of(mock(User.class)));
        // only CONFIRMED and ACTIVE allow cancel — CLOSED must throw
        when(reservation.getStatus()).thenReturn(ReservationStatus.CLOSED);

        assertThrows(IllegalReservationStateException.class,
                () -> service.cancelTour(10, 1));

        verify(reservation, never()).cancel();
    }

    @Test
    void cancelTour_throwsIfCallerNotParticipant() {
        Integer outsiderId = 42;

        Reservation reservation = mock(Reservation.class);
        TourListing listing = mock(TourListing.class);
        User outsider = mock(User.class);
        User guest = mock(User.class);
        User host = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(userRepo.findById(outsiderId)).thenReturn(Optional.of(outsider));
        when(outsider.getId()).thenReturn(outsiderId);
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(10);
        when(reservation.getTourListing()).thenReturn(listing);
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(99);

        assertThrows(IllegalArgumentException.class,
                () -> service.cancelTour(outsiderId, 1));

        verify(reservation, never()).cancel();
    }

    @Test
    void cancelTour_confirmedHappyPath_cancelsReservation() {
        Integer userId = 10;

        Reservation reservation = mock(Reservation.class);
        User caller = mock(User.class);
        User guest = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(userRepo.findById(userId)).thenReturn(Optional.of(caller));
        when(caller.getId()).thenReturn(userId);
        when(reservation.getStatus()).thenReturn(ReservationStatus.CONFIRMED);
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(userId);
        when(caller.getUsername()).thenReturn("david");

        CancelTourResponse response = service.cancelTour(userId, 1);

        verify(reservation).cancel();
        verify(reservationRepo).save(reservation);
        assertEquals("david", response.cancelledByUsername());
    }

    @Test
    void cancelTour_activeHappyPath_cancelsReservation() {
        Integer userId = 10;

        Reservation reservation = mock(Reservation.class);
        User caller = mock(User.class);
        User guest = mock(User.class);

        when(reservationRepo.findById(1)).thenReturn(Optional.of(reservation));
        when(userRepo.findById(userId)).thenReturn(Optional.of(caller));
        when(caller.getId()).thenReturn(userId);
        // ACTIVE is also a valid pre-cancel state
        when(reservation.getStatus()).thenReturn(ReservationStatus.ACTIVE);
        when(reservation.getGuest()).thenReturn(guest);
        when(guest.getId()).thenReturn(userId);

        service.cancelTour(userId, 1);

        verify(reservation).cancel();
        verify(reservationRepo).save(reservation);
    }

    // ──────────────── expireConfirmedReservations ────────────────

    @Test
    void expireConfirmedReservations_doesNothingWhenNoExpiredReservationsFound() {
        when(reservationRepo.findAllByStatusAndTourListing_MeetingDateBefore(
                eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        service.expireConfirmedReservations();

        verify(reservationRepo, never()).saveAll(any());
    }

    @Test
    void expireConfirmedReservations_expiresOneReservation() {
        Reservation reservation = mock(Reservation.class);

        when(reservationRepo.findAllByStatusAndTourListing_MeetingDateBefore(
                eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of(reservation));

        service.expireConfirmedReservations();

        verify(reservation).expire();
        verify(reservationRepo).saveAll(List.of(reservation));
    }

    @Test
    void expireConfirmedReservations_expiresAllExpiredReservations() {
        Reservation r1 = mock(Reservation.class);
        Reservation r2 = mock(Reservation.class);

        when(reservationRepo.findAllByStatusAndTourListing_MeetingDateBefore(
                eq(ReservationStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(List.of(r1, r2));

        service.expireConfirmedReservations();

        verify(r1).expire();
        verify(r2).expire();
        verify(reservationRepo).saveAll(List.of(r1, r2));
    }
}
