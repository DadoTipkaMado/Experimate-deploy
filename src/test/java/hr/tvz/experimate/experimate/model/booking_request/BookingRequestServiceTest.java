package hr.tvz.experimate.experimate.model.booking_request;

import hr.tvz.experimate.experimate.model.booking_request.CreateBookingRequestDto;
import hr.tvz.experimate.experimate.model.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.model.reservation.exception.GuestAlreadyBookedException;
import hr.tvz.experimate.experimate.model.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.model.tour_listing.TourListing;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingRepo;
import hr.tvz.experimate.experimate.model.tour_listing.exception.HostAlreadyTakenException;
import hr.tvz.experimate.experimate.user.User;
import hr.tvz.experimate.experimate.user.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingRequestServiceTest {

    @Mock private BookingRequestRepo bookingRequestRepo;
    @Mock private UserRepo userRepo;
    @Mock private TourListingRepo tourListingRepo;
    @Mock private ReservationRepo reservationRepo;
    @Mock private ApplicationEventPublisher publisher;

    @InjectMocks private BookingRequestService service;

    @Test
    void throwsIfTourListingHostNotEqualToGivenUser() {
        BookingRequest request = Mockito.mock(BookingRequest.class);
        User host = Mockito.mock(User.class);
        TourListing listing =  Mockito.mock(TourListing.class);

        Mockito.when(bookingRequestRepo.findById(1)).thenReturn(Optional.of(request));
        Mockito.when(request.getListing()).thenReturn(listing);
        Mockito.when(listing.getHost()).thenReturn(host);
        Mockito.when(host.getId()).thenReturn(2);

        assertThrows(ForbiddenActionException.class, () -> service.acceptBookingRequest(1,1));
    }

    @Test
    void throwsIfGuestAlreadyHasActiveReservation() {
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

        when(reservationRepo.existsByGuest_IdAndTourListing_MeetingDateBetween(
                any(), any(), any())).thenReturn(true);

        assertThrows(GuestAlreadyBookedException.class, () -> service.createBookingRequest(dto, guestId));
    }

    @Test
    void throwsIfHostAlreadyHasActiveReservation() {
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

        when(reservationRepo.existsByTourListing_Host_IdAndTourListing_MeetingDateBetween(
                any(), any(), any())).thenReturn(true);

        assertThrows(HostAlreadyTakenException.class, () -> service.createBookingRequest(dto, guestId));
    }
}