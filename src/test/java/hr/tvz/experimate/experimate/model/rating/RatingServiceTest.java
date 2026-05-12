package hr.tvz.experimate.experimate.model.rating;

import hr.tvz.experimate.experimate.model.reservation.Reservation;
import hr.tvz.experimate.experimate.model.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.model.reservation.ReservationStatus;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepo ratingRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private ReservationRepo reservationRepo;
    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private RatingService ratingService;

    @Test
    void throwsIfRaterEqualsRated() {
        User rater = mock(User.class);
        when(rater.getId()).thenReturn(1);

        CreateRatingDto dto = new CreateRatingDto(rater.getId(), 3, "SUPER");

        when(userRepo.findById(1)).thenReturn(Optional.of(rater));

        assertThrows(IllegalArgumentException.class, () -> {
            ratingService.createRating(dto, 1);
        });
    }

    @Test
    void createRating_ReturnsRatingResponse() {
        User rater = mock(User.class);
        User rated = mock(User.class);
        Rating rating = mock(Rating.class);

        when(userRepo.findById(1)).thenReturn(Optional.of(rater));
        when(userRepo.findById(2)).thenReturn(Optional.of(rated));

        when(rater.getId()).thenReturn(1);
        when(rated.getId()).thenReturn(2);

        CreateRatingDto dto = new CreateRatingDto(rated.getId(), 3, "SUPER");

        when(reservationRepo.findByGuest_IdAndTourListing_Host_IdAndStatus(
                rater.getId(), rated.getId(), ReservationStatus.CLOSED)
        )
                .thenReturn(Optional.of(mock(Reservation.class)));

        when(ratingRepo.existsByRater_IdAndRated_Id(rater.getId(), rated.getId()))
                .thenReturn(false);

        when(ratingRepo.save(Mockito.any(Rating.class))).thenReturn(rating);

        when(rating.getId()).thenReturn(1);
        when(rating.getScore()).thenReturn(3);
        when(rating.getReview()).thenReturn("SUPER");

        RatingResponse response = new RatingResponse(
                rating.getId(),
                rating.getScore(),
                rating.getReview()
        );

        assertEquals(response, ratingService.createRating(dto, rater.getId()));
    }
}