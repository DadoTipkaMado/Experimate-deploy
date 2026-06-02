package hr.tvz.experimate.experimate.domain.promoted_ad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PromotedAdRepository extends JpaRepository<PromotedAd, Integer> {

    /**
     * Returns all ads owned by the given partner profile.
     * Used by {@code GET /api/promoted-ads/mine}.
     */
    List<PromotedAd> findAllByPartnerProfile_Id(Integer partnerProfileId);

    /**
     * Returns whether the given event already has a promotion.
     * Used to reject duplicate promotion of the same event.
     */
    boolean existsByPartnerEvent_Id(Integer partnerEventId);

    /**
     * Returns the ad promoting the given event, if any.
     * Used to remove an event's promotion (unpromote).
     */
    Optional<PromotedAd> findByPartnerEvent_Id(Integer partnerEventId);

    /**
     * Returns all currently active ads that fall within their scheduling window at {@code now}.
     *
     * <p>An ad is considered active when:
     * <ul>
     *   <li>{@code active = true}</li>
     *   <li>{@code activeFrom} is null (no start boundary) OR {@code activeFrom <= now}</li>
     *   <li>{@code activeUntil} is null (no end boundary) OR {@code activeUntil > now}</li>
     * </ul>
     *
     * @param now the current timestamp used to evaluate scheduling boundaries
     */
    @Query("""
        SELECT a FROM PromotedAd a
        WHERE a.active = true
        AND (a.activeFrom IS NULL OR a.activeFrom <= :now)
        AND (a.activeUntil IS NULL OR a.activeUntil > :now)
        """)
    List<PromotedAd> findAllActiveBetween(@Param("now") LocalDateTime now);
}
