package hr.tvz.experimate.experimate.domain.partner;

import hr.tvz.experimate.experimate.domain.partner_event.PartnerEvent;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventRepository;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventResponse;
import hr.tvz.experimate.experimate.domain.user.Role;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for partner account management.
 *
 * <p>Handles self-serve onboarding ({@link #apply}), profile retrieval, dashboard stats,
 * and the partner's own event list (optionally filtered to upcoming events only).
 */
@Service
public class PartnerService {

    private final PartnerProfileRepository partnerProfileRepository;
    private final UserRepo userRepo;
    private final PartnerEventRepository partnerEventRepository;

    public PartnerService(PartnerProfileRepository partnerProfileRepository,
                          UserRepo userRepo,
                          PartnerEventRepository partnerEventRepository) {
        this.partnerProfileRepository = partnerProfileRepository;
        this.userRepo = userRepo;
        this.partnerEventRepository = partnerEventRepository;
    }

    /**
     * Upgrades a regular user to PARTNER role by creating a {@link PartnerProfile}
     * with the provided company info. A user who already has a partner profile receives
     * a 409 CONFLICT (via {@link IllegalStateException}, handled globally).
     *
     * @param userId the ID of the authenticated user requesting the upgrade
     * @param dto    company info collected during self-serve onboarding
     * @return the newly created partner profile
     * @throws UserNotFoundException  if the user ID does not exist
     * @throws IllegalStateException  if the user is already a partner
     */
    @Transactional
    public PartnerProfileResponse apply(Integer userId, ApplyPartnerRequest dto) {
        if (partnerProfileRepository.existsByUserId(userId)) {
            throw new IllegalStateException("User " + userId + " is already a partner.");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        PartnerProfile profile = new PartnerProfile(
                user,
                dto.companyName(),
                dto.contactEmail(),
                dto.website(),
                LocalDateTime.now()
        );
        partnerProfileRepository.save(profile);

        user.setRole(Role.PARTNER);
        userRepo.save(user);

        return toResponse(profile);
    }

    /**
     * Returns the partner profile for the given user.
     *
     * @param userId the authenticated partner's user ID
     * @return the partner's profile data
     * @throws IllegalStateException if no partner profile exists (should not happen
     *                               when the endpoint is guarded by {@code hasRole('PARTNER')})
     */
    public PartnerProfileResponse getProfile(Integer userId) {
        return partnerProfileRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalStateException("Partner profile not found for user " + userId));
    }

    /**
     * Returns dashboard statistics for the given partner.
     *
     * <p>{@code profileViews} and {@code bookings} are stubbed as 0 until view-tracking
     * and the {@code PartnerPin} event model are implemented. {@code activeEvents} counts
     * the partner's TourListings with a future meeting date as a temporary approximation.
     *
     * @param userId the authenticated partner's user ID
     * @return stats snapshot
     */
    @Transactional(readOnly = true)
    public PartnerStatsResponse getStats(Integer userId) {
        long activeEvents = partnerEventRepository
                .countByPartnerPin_PartnerProfile_UserIdAndStartDatetimeAfter(userId, LocalDateTime.now());
        return new PartnerStatsResponse(0, 0, activeEvents);
    }

    /**
     * Returns partner events for the given user's partner profile.
     * Pass {@code "upcoming"} as {@code filter} to restrict to events whose
     * {@code startDatetime} is in the future; any other value (including {@code null})
     * returns all events, including past ones.
     *
     * @param userId  the authenticated partner's user ID
     * @param filter  optional filter; {@code "upcoming"} returns only future events
     */
    @Transactional(readOnly = true)
    public List<PartnerEventResponse> getEvents(Integer userId, String filter) {
        List<PartnerEvent> events = "upcoming".equalsIgnoreCase(filter)
                ? partnerEventRepository.findByPartnerPin_PartnerProfile_UserIdAndStartDatetimeAfter(
                        userId, LocalDateTime.now())
                : partnerEventRepository.findByPartnerPin_PartnerProfile_UserId(userId);
        return events.stream().map(this::toPartnerEventResponse).toList();
    }

    /**
     * Returns the partner status for the given user.
     *
     * <p>Uses the role already loaded in {@code AppUserDetails} to avoid a database
     * round-trip for non-partners. The profile is fetched only when the user is a partner.
     *
     * @param userId the authenticated user's ID
     * @param role   the user's current role, taken from {@code AppUserDetails}
     * @return status with {@code isPartner=true} and a populated profile, or
     *         {@code isPartner=false} with a {@code null} profile
     */
    public PartnerStatusResponse getStatus(Integer userId, Role role) {
        if (role != Role.PARTNER) {
            return new PartnerStatusResponse(false, null);
        }
        PartnerProfileResponse profile = partnerProfileRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalStateException("Partner profile not found for user " + userId));
        return new PartnerStatusResponse(true, profile);
    }

    private PartnerProfileResponse toResponse(PartnerProfile profile) {
        return new PartnerProfileResponse(
                profile.getId(),
                profile.getCompanyName(),
                profile.getContactEmail(),
                profile.getWebsite(),
                profile.getCreatedAt()
        );
    }

    private PartnerEventResponse toPartnerEventResponse(PartnerEvent event) {
        return new PartnerEventResponse(
                event.getId(),
                event.getPartnerPin().getId(),
                event.getPartnerPin().getName(),
                event.getPartnerPin().getLatitude(),
                event.getPartnerPin().getLongitude(),
                event.getTitle(),
                event.getDescription(),
                event.getTicketVendorUrl(),
                event.getStartDatetime(),
                event.getEndDatetime(),
                event.getCreatedAt()
        );
    }
}
