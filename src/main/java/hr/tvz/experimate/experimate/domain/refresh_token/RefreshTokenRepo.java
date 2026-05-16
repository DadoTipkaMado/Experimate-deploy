package hr.tvz.experimate.experimate.domain.refresh_token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Integer> {
    boolean existsByTokenAndUser_Id(String token, Integer userId);

    Optional<RefreshToken> findByUser_Id(Integer id);

    Optional<RefreshToken> findByToken(String token);

   @Modifying
   @Query("DELETE FROM RefreshToken rt WHERE rt.token = :token")
    void deleteByToken(@Param("token") String token);
}
