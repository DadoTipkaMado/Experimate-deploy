package hr.tvz.experimate.experimate.domain.rating;

import hr.tvz.experimate.experimate.domain.rating.dto.CreateRatingDto;
import hr.tvz.experimate.experimate.domain.rating.dto.UpdateRatingDto;
import hr.tvz.experimate.experimate.domain.rating.exception.DuplicateRatingException;
import hr.tvz.experimate.experimate.domain.rating.exception.RatingNotFoundException;
import hr.tvz.experimate.experimate.domain.rating.response.RatingResponse;
import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.shared.DetailsMapper;
import hr.tvz.experimate.experimate.shared.UserDetails;
import hr.tvz.experimate.experimate.shared.event.RatingCreatedEvent;
import hr.tvz.experimate.experimate.shared.event.RatingRecalculatedEvent;
import hr.tvz.experimate.experimate.shared.event.UserDeletedEvent;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
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
    private final DetailsMapper detailsMapper;

    public RatingService(RatingRepo ratingRepo, UserRepo userRepo, ReservationRepo reservationRepo,
                         ApplicationEventPublisher publisher, DetailsMapper detailsMapper) {
        this.ratingRepo = ratingRepo;
        this.userRepo = userRepo;
        this.reservationRepo = reservationRepo;
        this.publisher = publisher;
        this.detailsMapper = detailsMapper;
    }

    /**
     * Creates a rating from one user about another and recomputes the rated user's average score.
     * Both users must have completed a shared tour, and a user may rate the same person only once.
     *
     * @param dto     rating details (rated user ID, score, optional review)
     * @param raterId ID of the user submitting the rating
     * @return the created {@link RatingResponse}
     * @throws UserNotFoundException    if either the rater or the rated user does not exist
     * @throws IllegalArgumentException if a user attempts to rate themselves
     * @throws ForbiddenActionException if the two users have not completed a shared tour
     * @throws DuplicateRatingException if the rater has already rated this user
     */
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

        RatingResponse response = toResponse(saved);

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

    /**
     * Finds a single rating by ID.
     *
     * @param id the rating ID
     * @return the {@link RatingResponse} if found, otherwise empty
     */
    public Optional<RatingResponse> getRatingById(Integer id) {
        Optional<Rating> rating = ratingRepo.findById(id);
        return rating.map(this::toResponse);
    }

    /**
     * Returns all ratings in the system.
     *
     * @return list of all {@link RatingResponse}
     */
    public List<RatingResponse> getAllRatings() {
        return ratingRepo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Updates the score and/or review of a rating and recomputes the rated user's average.
     * Only the rating's author may edit it.
     *
     * @param id       the rating ID
     * @param dto      fields to update (null fields are left unchanged)
     * @param callerId ID of the authenticated user requesting the update
     * @return the updated {@link RatingResponse}
     * @throws RatingNotFoundException  if no rating exists with the given ID
     * @throws ForbiddenActionException if the caller is not the rating's author
     */
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

        return toResponse(rating);
    }

    /**
     * Deletes a rating and recomputes the rated user's average score.
     * Only the rating's author may delete it.
     *
     * @param id      the rating ID
     * @param raterId ID of the authenticated user requesting deletion
     * @throws RatingNotFoundException  if no rating exists with the given ID
     * @throws IllegalArgumentException if the caller is not the rating's author
     */
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

    /**
     * Event handler that deletes every rating authored by or about a deleted user.
     *
     * @param event the {@link UserDeletedEvent} carrying the deleted user's ID
     */
    @Transactional
    @EventListener
    public void handleUserDeleted(UserDeletedEvent event) {
        ratingRepo.deleteAllByRater_IdOrRated_Id(event.userId(), event.userId());
        log.info("Deleted all ratings for user with id {}", event.userId());
    }

    private RatingResponse toResponse(Rating rating) {
        UserDetails rater = detailsMapper.mapUserDetails(rating.getRater());
        UserDetails rated = detailsMapper.mapUserDetails(rating.getRated());
        return new RatingResponse(rating.getId(), rating.getScore(), rating.getReview(), rater, rated);
    }
}
