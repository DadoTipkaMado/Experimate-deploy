package hr.tvz.experimate.experimate.model.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RatingRepo extends JpaRepository<Rating, Integer> {

    @Query("SELECT AVG(score) FROM Rating WHERE rated.id = :userId")
    Double averageRatingScoreByUserId(Integer userId);

    boolean existsByRater_IdAndRated_Id(Integer raterId, Integer ratedId);

    void deleteAllByRater_IdOrRated_Id(Integer raterId, Integer ratedId);
}
