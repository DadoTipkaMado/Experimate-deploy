package hr.tvz.experimate.experimate.model.rating;

import hr.tvz.experimate.experimate.model.rating.exception.DuplicateRatingException;
import hr.tvz.experimate.experimate.model.rating.exception.RatingNotFoundException;
import hr.tvz.experimate.experimate.model.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.model.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.model.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.model.shared.event.RatingCreatedEvent;
import hr.tvz.experimate.experimate.model.shared.event.RatingRecalculatedEvent;
import hr.tvz.experimate.experimate.model.shared.event.UserDeletedEvent;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingService.class);

    private final RatingRepo ratingRepo;
    private final UserRepo userRepo;
    private final ReservationRepo reservationRepo;
    private final ApplicationEventPublisher publisher;

    public RatingService(RatingRepo ratingRepo, UserRepo userRepo, ReservationRepo reservationRepo, ApplicationEventPublisher publisher) {
        this.ratingRepo = ratingRepo;
        this.userRepo = userRepo;
        this.reservationRepo = reservationRepo;
        this.publisher = publisher;
    }

    @Transactional
    public RatingResponse createRating(CreateRatingDto dto, Integer raterId) {
        User rater = userRepo.findById(raterId)
                .orElseThrow(() -> new UserNotFoundException(raterId));
        User rated = userRepo.findById(dto.ratedId())
                .orElseThrow(() -> new UserNotFoundException(dto.ratedId()));

        if(rater.getId().equals(rated.getId()))
            throw new IllegalArgumentException("User cannot rate themselves.");

        boolean sharedTour =
                reservationRepo.findByGuest_IdAndTourListing_Host_IdAndStatus(raterId, rated.getId(), ReservationStatus.CLOSED).isPresent()
                || reservationRepo.findByGuest_IdAndTourListing_Host_IdAndStatus(rated.getId(), raterId, ReservationStatus.CLOSED).isPresent();
        if (!sharedTour)
            throw new ForbiddenActionException("You can only rate someone you have completed a tour with.");

        if(ratingRepo.existsByRater_IdAndRated_Id(rater.getId(), rated.getId()))
            throw new DuplicateRatingException(rater.getId(), rated.getId());

        Rating rating = new Rating(rater, rated, dto.score(), dto.review());
        Rating saved = ratingRepo.save(rating);

        log.info("Rating created with id {}", rating.getId());

        RatingResponse response = new RatingResponse(
                saved.getId(),
                saved.getScore(),
                saved.getReview()
        );

        publisher.publishEvent(new RatingCreatedEvent(
                rater.getId(),
                rated.getId())
        );

        publisher.publishEvent(new RatingRecalculatedEvent(
                rated.getId(),
                ratingRepo.averageRatingScoreByUserId(rated.getId())
        ));

        return response;
    }

    public Optional<RatingResponse> getRatingById(Integer id) {
        Optional<Rating> rating = ratingRepo.findById(id);
        return rating.map(r -> new RatingResponse(
                r.getId(),
                r.getScore(),
                r.getReview()
        ));
    }

    public List<RatingResponse> getAllRatings() {
        return ratingRepo.findAll()
                .stream()
                .map(rating -> new RatingResponse(
                        rating.getId(),
                        rating.getScore(),
                        rating.getReview()
                ))
                .toList();
    }

    @Transactional
    public RatingResponse updateRating(Integer id, UpdateRatingDto dto, Integer callerId) {
        Rating rating = ratingRepo.findById(id)
                .orElseThrow(() -> new RatingNotFoundException(id));

        if (!rating.getRater().getId().equals(callerId))
            throw new ForbiddenActionException("Only the author of the rating can edit it.");

        if (dto.score() != null)
            rating.setScore(dto.score());
        if (dto.review() != null)
            rating.setReview(dto.review());

        ratingRepo.save(rating);

        log.info("Rating updated with id {}", id);

        publisher.publishEvent(new RatingRecalculatedEvent(
                rating.getRated().getId(),
                ratingRepo.averageRatingScoreByUserId(rating.getRated().getId())
        ));

        return new RatingResponse(
                rating.getId(),
                rating.getScore(),
                rating.getReview()
        );
    }

    @Transactional
    public void deleteRating(Integer id, Integer raterId) {
        Rating rating = ratingRepo.findById(id)
                .orElseThrow(() -> new RatingNotFoundException(id));

        if (!rating.getRater().getId().equals(raterId))
            throw new IllegalArgumentException("Only the author of the rating can delete it");

        User rated = rating.getRated();
        ratingRepo.deleteById(id);

        publisher.publishEvent(new RatingRecalculatedEvent(
                rated.getId(),
                ratingRepo.averageRatingScoreByUserId(rated.getId())
        ));

        log.info("Rating deleted with id {}", id);
    }

    @Transactional
    @EventListener
    public void handleUserDeleted(UserDeletedEvent event) {
        ratingRepo.deleteAllByRater_IdOrRated_Id(event.userId(), event.userId());
        log.info("Deleted all ratings for user with id {}", event.userId());
    }

    //TODO popravi kaj si zdrko na poslu tu, provjeri logiku i flow ratinga sa userima

}
