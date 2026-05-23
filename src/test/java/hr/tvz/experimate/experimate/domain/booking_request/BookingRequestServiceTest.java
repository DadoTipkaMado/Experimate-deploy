package hr.tvz.experimate.experimate.domain.booking_request;

import hr.tvz.experimate.experimate.domain.booking_request.dto.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.domain.booking_request.exception.BookingRequestNotFoundException;
import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.domain.reservation.exception.GuestAlreadyBookedException;
import hr.tvz.experimate.experimate.shared.DetailsMapper;
import hr.tvz.experimate.experimate.shared.event.BookingRequestAcceptedEvent;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.domain.tour_listing.TourListing;
import hr.tvz.experimate.experimate.domain.tour_listing.TourListingRepo;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.HostAlreadyTakenException;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingRequestServiceTest {

    @Mock private BookingRequestRepo bookingRequestRepo;
    @Mock private UserRepo userRepo;
    @Mock private TourListingRepo tourListingRepo;
    @Mock private ReservationRepo reservationRepo;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private DetailsMapper detailsMapper;

    @InjectMocks private BookingRequestService service;

    @Test
    void createBookingRequest_throwsIfGuestAlreadyHasActiveReservation() {
        CreateBookingRequestDto dto = new CreateBookingRequestDto(5);
        Integer guestId = 10;
        Integer hostId = 99;

        User host = mock(User.class);
        TourListing listing = mock(TourListing.class);

        LocalDateTime meetingDate = LocalDateTime.now().plusDays(1);

        when(tourListingRepo.findById(5)).thenReturn(Optional.of(listing));
        when(userRepo.findById(guestId)).thenReturn(Optional.of(mock(User.class)));
        when(listing.getMeetingDate()).thenReturn(meetingDate);
        // host id differs from guestId so the "can't book yourself" check passes
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(hostId);

        when(reservationRepo.existsByGuest_IdAndTourListing_MeetingDateBetweenAndStatusIn(
                any(), any(), any(), any())).thenReturn(true);

        assertThrows(GuestAlreadyBookedException.class, () -> service.createBookingRequest(dto, guestId));
    }

    @Test
    void createBookingRequest_throwsIfHostAlreadyHasActiveReservation() {
        CreateBookingRequestDto dto = new CreateBookingRequestDto(5);
        Integer guestId = 10;
        Integer hostId = 99;

        User host = mock(User.class);
        TourListing listing = mock(TourListing.class);

        LocalDateTime meetingDate = LocalDateTime.now().plusDays(1);

        when(tourListingRepo.findById(5)).thenReturn(Optional.of(listing));
        when(userRepo.findById(guestId)).thenReturn(Optional.of(mock(User.class)));
        when(listing.getMeetingDate()).thenReturn(meetingDate);
        // host id differs from guestId so the "can't book yourself" check passes
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(hostId);

        // host has an active reservation on a *different* listing in the same window
        when(reservationRepo.existsByHostOnDifferentListingInWindow(
                any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(HostAlreadyTakenException.class, () -> service.createBookingRequest(dto, guestId));
    }

    // ──────────────── acceptBookingRequest ────────────────

    @Test
    void acceptBookingRequest_throwsIfRequestNotFound() {
        when(bookingRequestRepo.findById(42)).thenReturn(Optional.empty());

        assertThrows(BookingRequestNotFoundException.class,
                () -> service.acceptBookingRequest(42, 1));
    }

    @Test
    void acceptBookingRequest_happyPath_publishesEventAndAcceptsRequest() {
        Integer acceptedId = 1;
        Integer hostId = 7;
        Integer guestId = 11;
        Integer listingId = 5;

        BookingRequest request = mock(BookingRequest.class);
        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);
        User guest = mock(User.class);

        when(bookingRequestRepo.findById(acceptedId)).thenReturn(Optional.of(request));
        when(request.getListing()).thenReturn(listing);
        when(request.getGuest()).thenReturn(guest);
        when(listing.getHost()).thenReturn(host);
        when(listing.getId()).thenReturn(listingId);
        when(host.getId()).thenReturn(hostId);
        when(guest.getId()).thenReturn(guestId);
        // listing has 1 slot, now filled — triggers auto-decline check
        when(reservationRepo.countByTourListing_IdAndStatusIn(eq(listingId), eq(List.of(ReservationStatus.CONFIRMED)))).thenReturn(1L);
        when(listing.getMaxGuests()).thenReturn(1);
        // only the accepted request exists — no other pending requests to decline
        when(bookingRequestRepo.findBookingRequestIdsByTourListingIdAndStatus(listingId, BookingRequestStatus.PENDING))
                .thenReturn(List.of());

        service.acceptBookingRequest(acceptedId, hostId);

        // accepted event carries guest + listing
        ArgumentCaptor<BookingRequestAcceptedEvent> eventCaptor =
                ArgumentCaptor.forClass(BookingRequestAcceptedEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertEquals(guestId, eventCaptor.getValue().guestId());
        assertEquals(listingId, eventCaptor.getValue().listingId());

        // accepted request gets its status flipped to ACCEPTED and saved
        verify(request).setStatus(BookingRequestStatus.ACCEPTED);
        verify(bookingRequestRepo).save(request);

        // no competing requests → batch update called with empty list
        verify(bookingRequestRepo).updateStatusByIds(List.of(), BookingRequestStatus.DECLINED);
    }

    @Test
    void acceptBookingRequest_competingRequests_areBatchDeclined() {
        Integer acceptedId = 1;
        Integer hostId = 7;
        Integer listingId = 5;

        BookingRequest request = mock(BookingRequest.class);
        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);
        User guest = mock(User.class);

        when(bookingRequestRepo.findById(acceptedId)).thenReturn(Optional.of(request));
        when(request.getListing()).thenReturn(listing);
        when(request.getGuest()).thenReturn(guest);
        when(listing.getHost()).thenReturn(host);
        when(listing.getId()).thenReturn(listingId);
        when(host.getId()).thenReturn(hostId);
        when(guest.getId()).thenReturn(11);
        // listing has 1 slot, now filled — auto-decline is triggered
        when(reservationRepo.countByTourListing_IdAndStatusIn(eq(listingId), eq(List.of(ReservationStatus.CONFIRMED)))).thenReturn(1L);
        when(listing.getMaxGuests()).thenReturn(1);
        // IDs 2 and 3 are still PENDING — they must be auto-declined
        when(bookingRequestRepo.findBookingRequestIdsByTourListingIdAndStatus(listingId, BookingRequestStatus.PENDING))
                .thenReturn(List.of(2, 3));

        service.acceptBookingRequest(acceptedId, hostId);

        verify(bookingRequestRepo).updateStatusByIds(List.of(2, 3), BookingRequestStatus.DECLINED);
    }

    @Test
    void acceptBookingRequest_throwsIfCallerNotHost_andDoesNotPublishEvent() {
        BookingRequest request = mock(BookingRequest.class);
        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);

        when(bookingRequestRepo.findById(1)).thenReturn(Optional.of(request));
        when(request.getListing()).thenReturn(listing);
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(99);

        assertThrows(ForbiddenActionException.class,
                () -> service.acceptBookingRequest(1, 5));

        // no event fired, no status change, no batch decline
        verify(publisher, never()).publishEvent(any());
        verify(request, never()).setStatus(any());
        verify(bookingRequestRepo, never()).updateStatusByIds(any(), any());
    }

    @Test
    void acceptBookingRequest_listingNotYetFull_doesNotDeclinePendingRequests() {
        Integer acceptedId = 1;
        Integer hostId = 7;
        Integer listingId = 5;

        BookingRequest request = mock(BookingRequest.class);
        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);
        User guest = mock(User.class);

        when(bookingRequestRepo.findById(acceptedId)).thenReturn(Optional.of(request));
        when(request.getListing()).thenReturn(listing);
        when(request.getGuest()).thenReturn(guest);
        when(listing.getHost()).thenReturn(host);
        when(listing.getId()).thenReturn(listingId);
        when(host.getId()).thenReturn(hostId);
        when(guest.getId()).thenReturn(11);
        // 1 of 3 slots filled — listing is not yet full
        when(reservationRepo.countByTourListing_IdAndStatusIn(eq(listingId), eq(List.of(ReservationStatus.CONFIRMED)))).thenReturn(1L);
        when(listing.getMaxGuests()).thenReturn(3);

        service.acceptBookingRequest(acceptedId, hostId);

        // remaining pending requests must not be touched — spots are still available
        verify(bookingRequestRepo, never()).findBookingRequestIdsByTourListingIdAndStatus(any(), any());
        verify(bookingRequestRepo, never()).updateStatusByIds(any(), any());
    }

    // ──────────────── declineBookingRequest ────────────────

    @Test
    void declineBookingRequest_happyPath_setsStatusDeclined() {
        Integer requestId = 1;
        Integer hostId = 7;

        BookingRequest request = mock(BookingRequest.class);
        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);

        when(bookingRequestRepo.findById(requestId)).thenReturn(Optional.of(request));
        when(request.getListing()).thenReturn(listing);
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(hostId);
        when(bookingRequestRepo.save(request)).thenReturn(request);

        service.declineBookingRequest(requestId, hostId);

        verify(request).setStatus(BookingRequestStatus.DECLINED);
        verify(bookingRequestRepo).save(request);
        // declining a single request must not trigger the accept-side event
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void declineBookingRequest_throwsIfNotFound() {
        when(bookingRequestRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(BookingRequestNotFoundException.class,
                () -> service.declineBookingRequest(99, 1));
    }

    @Test
    void declineBookingRequest_throwsIfCallerNotHost() {
        BookingRequest request = mock(BookingRequest.class);
        TourListing listing = mock(TourListing.class);
        User host = mock(User.class);

        when(bookingRequestRepo.findById(1)).thenReturn(Optional.of(request));
        when(request.getListing()).thenReturn(listing);
        when(listing.getHost()).thenReturn(host);
        when(host.getId()).thenReturn(99);

        assertThrows(ForbiddenActionException.class,
                () -> service.declineBookingRequest(1, 5));

        verify(request, never()).setStatus(any());
        verify(bookingRequestRepo, never()).save(any());
    }
}
