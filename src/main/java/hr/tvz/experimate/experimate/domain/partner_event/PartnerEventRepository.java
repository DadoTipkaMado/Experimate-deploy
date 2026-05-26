package hr.tvz.experimate.experimate.domain.partner_event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PartnerEventRepository extends JpaRepository<PartnerEvent, Integer> {

    /**
     * Returns all events for the given pin, ordered by start time ascending.
     * Used by {@code GET /api/partner-pins/{pinId}/events}.
     */
    List<PartnerEvent> findAllByPartnerPin_IdOrderByStartDatetimeAsc(Integer partnerPinId);

    /**
     * Returns all events across all pins owned by the given user's partner profile.
     * Used by {@code GET /api/partner/listings}.
     */
    List<PartnerEvent> findByPartnerPin_PartnerProfile_UserId(Integer userId);

    /**
     * Counts future events across all pins owned by the given user's partner profile.
     * Used by {@code GET /api/partner/stats} to populate {@code activeEvents}.
     */
    long countByPartnerPin_PartnerProfile_UserIdAndStartDatetimeAfter(Integer userId, LocalDateTime after);
}
