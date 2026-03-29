package hr.tvz.experimate.experimate.model.rating;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepo extends JpaRepository<Rating, Integer> {

    List<Rating> findAllByRated_Id(Integer ratedId);

    boolean existsByRater_IdAndRated_Id(Integer raterId, Integer ratedId);

    void deleteAllByRater_IdOrRated_Id(Integer raterId, Integer ratedId);
}
