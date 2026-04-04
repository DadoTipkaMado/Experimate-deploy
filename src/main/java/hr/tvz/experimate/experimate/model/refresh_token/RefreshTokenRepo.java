package hr.tvz.experimate.experimate.model.refresh_token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Integer> {
    boolean existsByTokenAndUser_Id(String token, Integer userId);

    Optional<RefreshToken> findByUser_Id(Integer id);

    RefreshToken findByToken(String token);
}
