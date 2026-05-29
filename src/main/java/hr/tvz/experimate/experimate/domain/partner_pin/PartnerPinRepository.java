package hr.tvz.experimate.experimate.domain.partner_pin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerPinRepository extends JpaRepository<PartnerPin, Integer> {

    /**
     * Returns all pins belonging to the given partner profile.
     * Used by {@code GET /api/partner-pins/mine} to show a partner their own pins.
     */
    List<PartnerPin> findAllByPartnerProfile_Id(Integer partnerProfileId);

    /**
     * Returns all currently active pins across all partners.
     * Used by {@code GET /api/partner-pins} to populate the public map.
     */
    List<PartnerPin> findAllByActiveTrue();
}
