package hr.tvz.experimate.experimate.domain.rating;

import hr.tvz.experimate.experimate.domain.rating.dto.CreateRatingDto;
import hr.tvz.experimate.experimate.domain.rating.dto.UpdateRatingDto;
import hr.tvz.experimate.experimate.domain.rating.response.RatingResponse;
import hr.tvz.experimate.experimate.domain.user.Role;
import hr.tvz.experimate.experimate.shared.DetailsMapper;
import hr.tvz.experimate.experimate.shared.UserDetails;

import hr.tvz.experimate.experimate.domain.reservation.Reservation;
import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.shared.event.RatingRecalculatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    @Mock
    private DetailsMapper mapper;

    @InjectMocks
    private RatingService ratingService;

    @Test
    void createRating_throwsIfRaterEqualsRated() {
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

        UserDetails raterDetails = new UserDetails("John", "Doe", "john.doe", Role.USER.toString(), "fakeUrl");
        UserDetails ratedDetails = new UserDetails("Jane", "Smith", "jane.smith", Role.USER.toString(), "fakeUrl");
        when(mapper.mapUserDetails(rater)).thenReturn(raterDetails);
        when(mapper.mapUserDetails(rated)).thenReturn(ratedDetails);

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
        when(rating.getRater()).thenReturn(rater);
        when(rating.getRated()).thenReturn(rated);

        RatingResponse response = new RatingResponse(
                rating.getId(),
                rating.getScore(),
                rating.getReview(),
                raterDetails,
                ratedDetails
        );

        assertEquals(response, ratingService.createRating(dto, rater.getId()));
    }

    // ──────────────── average recalculation ────────────────

    @Test
    void createRating_publishesRecalculatedEventWithRepoAverage() {
        Integer raterId = 1;
        Integer ratedId = 2;
        Double newAverage = 4.5;

        User rater = mock(User.class);
        User rated = mock(User.class);
        Rating saved = mock(Rating.class);

        when(userRepo.findById(raterId)).thenReturn(Optional.of(rater));
        when(userRepo.findById(ratedId)).thenReturn(Optional.of(rated));
        when(rater.getId()).thenReturn(raterId);
        when(rated.getId()).thenReturn(ratedId);
        when(reservationRepo.findByGuest_IdAndTourListing_Host_IdAndStatus(
                raterId, ratedId, ReservationStatus.CLOSED))
                .thenReturn(Optional.of(mock(Reservation.class)));
        when(ratingRepo.existsByRater_IdAndRated_Id(raterId, ratedId)).thenReturn(false);
        when(ratingRepo.save(any(Rating.class))).thenReturn(saved);
        when(saved.getRater()).thenReturn(rater);
        when(saved.getRated()).thenReturn(rated);
        // repo aggregate returns the new average — service must propagate it verbatim
        when(ratingRepo.averageRatingScoreByUserId(ratedId)).thenReturn(newAverage);

        ratingService.createRating(new CreateRatingDto(ratedId, 5, "Great host"), raterId);

        RatingRecalculatedEvent recalcEvent = captureRecalculatedEvent();
        assertEquals(ratedId, recalcEvent.userId());
        assertEquals(newAverage, recalcEvent.ratingScore());
    }

    @Test
    void updateRating_publishesRecalculatedEventWithNewAverage() {
        Integer ratingId = 5;
        Integer callerId = 1;
        Integer ratedId = 2;
        Double newAverage = 3.2;

        Rating rating = mock(Rating.class);
        User rater = mock(User.class);
        User rated = mock(User.class);

        when(ratingRepo.findById(ratingId)).thenReturn(Optional.of(rating));
        when(rating.getRater()).thenReturn(rater);
        when(rater.getId()).thenReturn(callerId);
        when(rating.getRated()).thenReturn(rated);
        when(rated.getId()).thenReturn(ratedId);
        when(ratingRepo.averageRatingScoreByUserId(ratedId)).thenReturn(newAverage);

        ratingService.updateRating(ratingId, new UpdateRatingDto(2, "meh"), callerId);

        RatingRecalculatedEvent recalcEvent = captureRecalculatedEvent();
        assertEquals(ratedId, recalcEvent.userId());
        assertEquals(newAverage, recalcEvent.ratingScore());
    }

    @Test
    void deleteRating_publishesRecalculatedEventWithRepoAverage() {
        Integer ratingId = 5;
        Integer callerId = 1;
        Integer ratedId = 2;
        Double newAverage = 4.0;

        Rating rating = mock(Rating.class);
        User rater = mock(User.class);
        User rated = mock(User.class);

        when(ratingRepo.findById(ratingId)).thenReturn(Optional.of(rating));
        when(rating.getRater()).thenReturn(rater);
        when(rater.getId()).thenReturn(callerId);
        when(rating.getRated()).thenReturn(rated);
        when(rated.getId()).thenReturn(ratedId);
        when(ratingRepo.averageRatingScoreByUserId(ratedId)).thenReturn(newAverage);

        ratingService.deleteRating(ratingId, callerId);

        verify(ratingRepo).deleteById(ratingId);

        RatingRecalculatedEvent recalcEvent = captureRecalculatedEvent();
        assertEquals(ratedId, recalcEvent.userId());
        assertEquals(newAverage, recalcEvent.ratingScore());
    }

    @Test
    void deleteRating_publishesEventWithNullAverageWhenNoRatingsLeft() {
        Integer ratingId = 5;
        Integer callerId = 1;
        Integer ratedId = 2;

        Rating rating = mock(Rating.class);
        User rater = mock(User.class);
        User rated = mock(User.class);

        when(ratingRepo.findById(ratingId)).thenReturn(Optional.of(rating));
        when(rating.getRater()).thenReturn(rater);
        when(rater.getId()).thenReturn(callerId);
        when(rating.getRated()).thenReturn(rated);
        when(rated.getId()).thenReturn(ratedId);
        // SELECT AVG over an empty set returns null in JPA — event must carry that null through
        when(ratingRepo.averageRatingScoreByUserId(ratedId)).thenReturn(null);

        ratingService.deleteRating(ratingId, callerId);

        RatingRecalculatedEvent recalcEvent = captureRecalculatedEvent();
        assertEquals(ratedId, recalcEvent.userId());
        assertNull(recalcEvent.ratingScore());
    }

    /**
     * createRating publishes two events (RatingCreated + RatingRecalculated),
     * updateRating/deleteRating publish one. This helper captures all and returns
     * the recalculation event so individual tests do not need to know how many
     * other events were fired alongside it.
     */
    private RatingRecalculatedEvent captureRecalculatedEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher, Mockito.atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(e -> e instanceof RatingRecalculatedEvent)
                .map(e -> (RatingRecalculatedEvent) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No RatingRecalculatedEvent was published"));
    }
}
