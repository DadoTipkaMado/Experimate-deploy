package hr.tvz.experimate.experimate.domain.rating;

import hr.tvz.experimate.experimate.domain.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rating")
public class Rating {

    private static final int MINIMUM_SCORE = 1;
    private static final int MAXIMUM_SCORE = 5;
    private static final int MAXIMUM_REVIEW_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "rater_id")
    private User rater;

    @ManyToOne
    @JoinColumn(name = "rated_id")
    private User rated;

    private Integer score;
    private String review;
    private LocalDateTime datePosted;

    //For hibernate
    protected Rating() {}

    public Rating(User rater, User rated, Integer score, String review) {
        this.rater = validateRater(rater);
        this.rated = validateRated(rated);
        this.score = validateScore(score);
        this.review = validateReview(review);
        this.datePosted = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public User getRater() {
        return rater;
    }

    public User getRated() {
        return rated;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = validateScore(score);
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = validateReview(review);
    }

    public LocalDateTime getDatePosted() {
        return datePosted;
    }

    private User validateRater(User rater) {
        if (rater == null)
            throw new IllegalArgumentException("Rater cannot be null");
        return rater;
    }

    private User validateRated(User rated) {
        if (rated == null)
            throw new IllegalArgumentException("Rated user cannot be null");
        return rated;
    }

    private Integer validateScore(Integer score) {
        if (score == null || score < MINIMUM_SCORE || score > MAXIMUM_SCORE)
            throw new IllegalArgumentException("Score must be between 1 and 5");
        return score;
    }

    private String validateReview(String review) {
        if (review == null || review.isBlank())
            throw new IllegalArgumentException("Review cannot be blank");
        if (review.length() > MAXIMUM_REVIEW_LENGTH)
            throw new IllegalArgumentException("Review cannot be longer than 500 characters");
        return review;
    }
}
