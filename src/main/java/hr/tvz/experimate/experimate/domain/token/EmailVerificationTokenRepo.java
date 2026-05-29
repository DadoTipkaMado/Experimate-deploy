package hr.tvz.experimate.experimate.domain.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepo extends JpaRepository<EmailVerificationToken, Integer> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    /** Returns all tokens that have not yet expired — i.e. users who still haven't verified their email. */
    List<EmailVerificationToken> findAllByExpirationDateTimeAfter(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expirationDateTime < :now")
    void deleteAllByExpirationDateTimeBefore(@Param("now") LocalDateTime now);
}
