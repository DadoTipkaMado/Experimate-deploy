package hr.tvz.experimate.experimate.domain.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expirationDateTime < :now")
    void deleteAllByExpirationDateTimeBefore(@Param("now") LocalDateTime now);
}
