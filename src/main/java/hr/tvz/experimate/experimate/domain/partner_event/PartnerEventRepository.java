package hr.tvz.experimate.experimate.domain.partner_event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Returns events with a future {@code startDatetime} across all pins owned by the given user's partner profile.
     * Used by {@code GET /api/partner/events?filter=upcoming}.
     */
    List<PartnerEvent> findByPartnerPin_PartnerProfile_UserIdAndStartDatetimeAfter(
            Integer userId, LocalDateTime after);

    /**
     * Returns all events with a future {@code startDatetime} across all partners, paginated.
     * Used by {@code GET /api/partner-events/upcoming}.
     */
    Page<PartnerEvent> findByStartDatetimeAfter(LocalDateTime after, Pageable pageable);

    /**
     * Counts future events across all pins owned by the given user's partner profile.
     * Used by {@code GET /api/partner/stats} to populate {@code activeEvents}.
     */
    long countByPartnerPin_PartnerProfile_UserIdAndStartDatetimeAfter(Integer userId, LocalDateTime after);

    /**
     * Deletes all events under any pin belonging to the given partner profile in a single query.
     * Used by {@link hr.tvz.experimate.experimate.domain.partner.PartnerService#leaveProgram}
     * when a partner leaves the program and their entire data tree is removed.
     */
    @Modifying
    @Query("DELETE FROM PartnerEvent e WHERE e.partnerPin.partnerProfile.id = :profileId")
    void deleteAllByPartnerProfileId(@Param("profileId") Integer profileId);
}
