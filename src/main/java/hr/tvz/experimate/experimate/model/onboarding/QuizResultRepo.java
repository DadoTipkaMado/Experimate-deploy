package hr.tvz.experimate.experimate.model.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link QuizResult} entities.
 */
public interface QuizResultRepo extends JpaRepository<QuizResult, Integer> {

    Optional<QuizResult> findByUser_Id(Integer userId);

    boolean existsByUser_Id(Integer userId);
}
