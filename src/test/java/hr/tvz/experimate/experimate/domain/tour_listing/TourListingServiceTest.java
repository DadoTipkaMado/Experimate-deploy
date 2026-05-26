package hr.tvz.experimate.experimate.domain.tour_listing;

import hr.tvz.experimate.experimate.domain.partner_event.PartnerEvent;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventNotFoundException;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventRepository;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPin;
import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.exception.IllegalReservationStateException;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.CreateListingFromEventRequest;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourListingServiceTest {

    @Mock private TourListingRepo listingRepo;
    @Mock private UserRepo userRepo;
    @Mock private ReservationRepo reservationRepo;
    @Mock private PartnerEventRepository partnerEventRepo;
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

    // ──────────────── createFromPartnerEvent ────────────────

    @Test
    void createFromPartnerEvent_throwsWhenEventNotFound() {
        when(partnerEventRepo.findById(1)).thenReturn(Optional.empty());

        CreateListingFromEventRequest req = new CreateListingFromEventRequest(
                1, "Zagreb", "A tour description for testing.", 10, null, null, null);

        assertThrows(PartnerEventNotFoundException.class,
                () -> service.createFromPartnerEvent(10, req));
    }

    @Test
    void createFromPartnerEvent_usesEventStartDatetimeWhenNoOverride() {
        Integer hostId = 10;
        LocalDateTime eventStart = LocalDateTime.of(2026, 7, 1, 10, 0);

        PartnerEvent event = mock(PartnerEvent.class);
        PartnerPin pin = mock(PartnerPin.class);
        User host = mock(User.class);
        TourListing savedListing = mock(TourListing.class);

        when(partnerEventRepo.findById(1)).thenReturn(Optional.of(event));
        when(event.getPartnerPin()).thenReturn(pin);
        when(event.getStartDatetime()).thenReturn(eventStart);
        when(pin.getLatitude()).thenReturn(45.0);
        when(pin.getLongitude()).thenReturn(16.0);
        when(userRepo.findById(hostId)).thenReturn(Optional.of(host));
        when(host.getId()).thenReturn(hostId);
        when(reservationRepo.existsByTourListing_Host_IdAndTourListing_MeetingDateBetweenAndStatusIn(
                anyInt(), any(), any(), anyList())).thenReturn(false);
        when(listingRepo.save(any(TourListing.class))).thenReturn(savedListing);

        CreateListingFromEventRequest req = new CreateListingFromEventRequest(
                1, "Zagreb", "A tour description for testing.", 10, null, null, null);

        service.createFromPartnerEvent(hostId, req);

        ArgumentCaptor<TourListing> captor = ArgumentCaptor.forClass(TourListing.class);
        verify(listingRepo).save(captor.capture());
        assertEquals(eventStart, captor.getValue().getMeetingDate());
    }

    @Test
    void createFromPartnerEvent_usesPinCoordinatesWhenNoOverride() {
        Integer hostId = 10;
        double pinLat = 45.815399;
        double pinLng = 15.966568;

        PartnerEvent event = mock(PartnerEvent.class);
        PartnerPin pin = mock(PartnerPin.class);
        User host = mock(User.class);
        TourListing savedListing = mock(TourListing.class);

        when(partnerEventRepo.findById(1)).thenReturn(Optional.of(event));
        when(event.getPartnerPin()).thenReturn(pin);
        when(event.getStartDatetime()).thenReturn(LocalDateTime.of(2026, 7, 1, 10, 0));
        when(pin.getLatitude()).thenReturn(pinLat);
        when(pin.getLongitude()).thenReturn(pinLng);
        when(userRepo.findById(hostId)).thenReturn(Optional.of(host));
        when(host.getId()).thenReturn(hostId);
        when(reservationRepo.existsByTourListing_Host_IdAndTourListing_MeetingDateBetweenAndStatusIn(
                anyInt(), any(), any(), anyList())).thenReturn(false);
        when(listingRepo.save(any(TourListing.class))).thenReturn(savedListing);

        CreateListingFromEventRequest req = new CreateListingFromEventRequest(
                1, "Zagreb", "A tour description for testing.", 10, null, null, null);

        service.createFromPartnerEvent(hostId, req);

        ArgumentCaptor<TourListing> captor = ArgumentCaptor.forClass(TourListing.class);
        verify(listingRepo).save(captor.capture());
        assertEquals(pinLat, captor.getValue().getLatitude());
        assertEquals(pinLng, captor.getValue().getLongitude());
    }

    @Test
    void createFromPartnerEvent_appliesOverrideMeetingDateWhenProvided() {
        Integer hostId = 10;
        LocalDateTime overrideDate = LocalDateTime.of(2026, 8, 15, 9, 0);

        PartnerEvent event = mock(PartnerEvent.class);
        PartnerPin pin = mock(PartnerPin.class);
        User host = mock(User.class);
        TourListing savedListing = mock(TourListing.class);

        when(partnerEventRepo.findById(1)).thenReturn(Optional.of(event));
        when(event.getPartnerPin()).thenReturn(pin);
        // event.getStartDatetime() intentionally not stubbed — override takes precedence
        when(pin.getLatitude()).thenReturn(45.0);
        when(pin.getLongitude()).thenReturn(16.0);
        when(userRepo.findById(hostId)).thenReturn(Optional.of(host));
        when(host.getId()).thenReturn(hostId);
        when(reservationRepo.existsByTourListing_Host_IdAndTourListing_MeetingDateBetweenAndStatusIn(
                anyInt(), any(), any(), anyList())).thenReturn(false);
        when(listingRepo.save(any(TourListing.class))).thenReturn(savedListing);

        CreateListingFromEventRequest req = new CreateListingFromEventRequest(
                1, "Zagreb", "A tour description for testing.".repeat(10), 10, overrideDate, null, null);

        service.createFromPartnerEvent(hostId, req);

        ArgumentCaptor<TourListing> captor = ArgumentCaptor.forClass(TourListing.class);
        verify(listingRepo).save(captor.capture());
        assertEquals(overrideDate, captor.getValue().getMeetingDate());
    }

    @Test
    void createFromPartnerEvent_appliesOverrideCoordinatesWhenProvided() {
        Integer hostId = 10;
        double overrideLat = 46.0;
        double overrideLng = 17.0;

        PartnerEvent event = mock(PartnerEvent.class);
        PartnerPin pin = mock(PartnerPin.class);
        User host = mock(User.class);
        TourListing savedListing = mock(TourListing.class);

        when(partnerEventRepo.findById(1)).thenReturn(Optional.of(event));
        when(event.getPartnerPin()).thenReturn(pin);
        when(event.getStartDatetime()).thenReturn(LocalDateTime.of(2026, 7, 1, 10, 0));
        // pin.getLatitude() / getLongitude() intentionally not stubbed — overrides take precedence
        when(userRepo.findById(hostId)).thenReturn(Optional.of(host));
        when(host.getId()).thenReturn(hostId);
        when(reservationRepo.existsByTourListing_Host_IdAndTourListing_MeetingDateBetweenAndStatusIn(
                anyInt(), any(), any(), anyList())).thenReturn(false);
        when(listingRepo.save(any(TourListing.class))).thenReturn(savedListing);

        CreateListingFromEventRequest req = new CreateListingFromEventRequest(
                1, "Zagreb", "A tour description for testing.", 10, null, overrideLat, overrideLng);

        service.createFromPartnerEvent(hostId, req);

        ArgumentCaptor<TourListing> captor = ArgumentCaptor.forClass(TourListing.class);
        verify(listingRepo).save(captor.capture());
        assertEquals(overrideLat, captor.getValue().getLatitude());
        assertEquals(overrideLng, captor.getValue().getLongitude());
    }
}
