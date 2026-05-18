package hr.tvz.experimate.experimate.domain.tour_listing;

import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.exception.IllegalReservationStateException;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.TourListingNotFoundException;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.shared.DetailsMapper;
import hr.tvz.experimate.experimate.shared.event.TourStartedEvent;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourListingServiceTest {

    @Mock private TourListingRepo listingRepo;
    @Mock private UserRepo userRepo;
    @Mock private ReservationRepo reservationRepo;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private DetailsMapper detailsMapper;

    @InjectMocks private TourListingService service;

    // ──────────────── startTour ────────────────

    @Test
    void startTour_throwsIfListingNotFound() {
        when(listingRepo.findById(1)).thenReturn(Optional.empty());

        assertThrows(TourListingNotFoundException.class,
                () -> service.startTour(1, 10));
    }

    @Test
    void startTour_throwsIfCallerNotHost() {
        Integer listingId = 1;
        Integer nonHostId = 42;

        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);

        when(listingRepo.findById(listingId)).thenReturn(Optional.of(listing));
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(99);

        assertThrows(ForbiddenActionException.class,
                () -> service.startTour(listingId, nonHostId));

        verify(listing, never()).startTour();
    }

    @Test
    void startTour_throwsIfTourAlreadyStarted() {
        Integer listingId = 1;
        Integer hostId = 10;

        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);

        when(listingRepo.findById(listingId)).thenReturn(Optional.of(listing));
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(hostId);
        // tour already in progress
        when(listing.isTourStarted()).thenReturn(true);

        assertThrows(IllegalReservationStateException.class,
                () -> service.startTour(listingId, hostId));

        verify(listing, never()).startTour();
    }

    @Test
    void startTour_throwsIfHostNotCheckedIn() {
        Integer listingId = 1;
        Integer hostId = 10;

        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);

        when(listingRepo.findById(listingId)).thenReturn(Optional.of(listing));
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(hostId);
        when(listing.isTourStarted()).thenReturn(false);
        // host has not checked in yet → cannot start
        when(listing.isHostCheckedIn()).thenReturn(false);

        assertThrows(IllegalReservationStateException.class,
                () -> service.startTour(listingId, hostId));

        verify(listing, never()).startTour();
    }

    @Test
    void startTour_happyPath_startsAndPublishesEvent() {
        Integer listingId = 1;
        Integer hostId = 10;

        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);

        when(listingRepo.findById(listingId)).thenReturn(Optional.of(listing));
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(hostId);
        when(listing.isTourStarted()).thenReturn(false);
        // host checked in — killswitch requires only this, no guest state needed
        when(listing.isHostCheckedIn()).thenReturn(true);

        service.startTour(listingId, hostId);

        verify(listing).startTour();
        verify(listingRepo).save(listing);

        ArgumentCaptor<TourStartedEvent> captor = ArgumentCaptor.forClass(TourStartedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertEquals(listingId, captor.getValue().listingId());
    }
}
