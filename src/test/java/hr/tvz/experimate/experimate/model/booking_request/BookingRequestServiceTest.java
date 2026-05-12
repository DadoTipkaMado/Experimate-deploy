package hr.tvz.experimate.experimate.model.booking_request;

import hr.tvz.experimate.experimate.model.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.model.tour_listing.TourListing;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingRepo;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BookingRequestServiceTest {

    @Mock private BookingRequestRepo bookingRequestRepo;
    @Mock private UserRepo userRepo;
    @Mock private TourListingRepo tourListingRepo;
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
}