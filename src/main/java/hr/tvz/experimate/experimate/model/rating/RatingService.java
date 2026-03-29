package hr.tvz.experimate.experimate.model.rating;

import hr.tvz.experimate.experimate.model.shared.event.UserDeletedEvent;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public RatingService(RatingRepo ratingRepo, UserRepo userRepo) {
        this.ratingRepo = ratingRepo;
        this.userRepo = userRepo;
    }

    public Rating createRating(CreateRatingDto dto) {
        User rater = userRepo.findById(dto.raterId())
                .orElseThrow(() -> new UserNotFoundException(dto.raterId()));
        User rated = userRepo.findById(dto.ratedId())
                .orElseThrow(() -> new UserNotFoundException(dto.ratedId()));

        Rating rating = new Rating(rater, rated, dto.score(), dto.review());
        ratingRepo.save(rating);

        recalculateRating(rated);

        log.info("Rating created with id {}", rating.getId());
        return rating;
    }

    public Optional<Rating> getRatingById(Integer id) {
        return ratingRepo.findById(id);
    }

    public List<Rating> getAllRatings() {
        return ratingRepo.findAll();
    }

    public Rating updateRating(Integer id, UpdateRatingDto dto) {
        Rating rating = ratingRepo.findById(id)
                .orElseThrow(() -> new RatingNotFoundException(id));

        if (dto.score() != null)
            rating.setScore(dto.score());
        if (dto.review() != null)
            rating.setReview(dto.review());

        ratingRepo.save(rating);
        recalculateRating(rating.getRated());

        log.info("Rating updated with id {}", id);
        return rating;
    }

    @Transactional
    public void deleteRating(Integer id, Integer raterId) {
        Rating rating = ratingRepo.findById(id)
                .orElseThrow(() -> new RatingNotFoundException(id));

        if (!rating.getRater().getId().equals(raterId))
            throw new IllegalArgumentException("Only the author of the rating can delete it");

        User rated = rating.getRated();
        ratingRepo.deleteById(id);
        recalculateRating(rated);

        log.info("Rating deleted with id {}", id);
    }

    @Transactional
    @EventListener
    public void handleUserDeleted(UserDeletedEvent event) {
        ratingRepo.deleteAllByRater_IdOrRated_Id(event.userId(), event.userId());
        log.info("Deleted all ratings for user with id {}", event.userId());
    }

    private void recalculateRating(User rated) {
        List<Rating> ratings = ratingRepo.findAllByRated_Id(rated.getId());

        double average = ratings.isEmpty() ? 0.0
                : ratings.stream().mapToInt(Rating::getScore).average().orElse(0.0);

        rated.setRating(average);
        userRepo.save(rated);
    }
}
