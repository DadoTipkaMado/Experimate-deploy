package hr.tvz.experimate.experimate.domain.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartnerProfileRepository extends JpaRepository<PartnerProfile, Integer> {

    Optional<PartnerProfile> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);
}
