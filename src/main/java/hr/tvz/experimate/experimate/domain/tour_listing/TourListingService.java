package hr.tvz.experimate.experimate.domain.tour_listing;

import hr.tvz.experimate.experimate.domain.partner_event.PartnerEvent;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventNotFoundException;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventRepository;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPin;
import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.domain.reservation.exception.IllegalReservationStateException;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.CreateListingFromEventRequest;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.CreateTourListingDto;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.UpdateTourListingDto;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.HostAlreadyTakenException;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.TourListingNotFoundException;
import hr.tvz.experimate.experimate.domain.tour_listing.response.TourListingResponse;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.shared.DetailsMapper;
import hr.tvz.experimate.experimate.shared.event.TourListingDeletedEvent;
import hr.tvz.experimate.experimate.shared.event.TourListingsDeletedEvent;
import hr.tvz.experimate.experimate.shared.event.TourListingsDeletedForHostEvent;
import hr.tvz.experimate.experimate.shared.event.TourStartedEvent;
import hr.tvz.experimate.experimate.shared.event.UserDeletedEvent;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.shared.util.DateTimeUtil;
import hr.tvz.experimate.experimate.shared.util.LocationObfuscator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TourListingService {

    private static final Logger log = LoggerFactory.getLogger(TourListingService.class);

    private final TourListingRepo listingRepo;
    private final UserRepo userRepo;
    private final ReservationRepo reservationRepo;
    private final PartnerEventRepository partnerEventRepo;
    private final ApplicationEventPublisher publisher;
    private final DetailsMapper detailsMapper;

    public TourListingService(TourListingRepo repo,
                              UserRepo userRepo,
                              ReservationRepo reservationRepo,
                              PartnerEventRepository partnerEventRepo,
                              ApplicationEventPublisher publisher,
                              DetailsMapper detailsMapper) {
        this.listingRepo = repo;
        this.userRepo = userRepo;
        this.reservationRepo = reservationRepo;
        this.partnerEventRepo = partnerEventRepo;
        this.publisher = publisher;
        this.detailsMapper = detailsMapper;
    }

    /**
     * Creates a new tour listing for the given host.
     *
     * @param dto    validated listing details (city, coordinates, meeting date, description, capacity)
     * @param hostId ID of the user creating the listing
     * @return the created {@link TourListingResponse}
     * @throws UserNotFoundException     if no user exists with the given ID
     * @throws HostAlreadyTakenException if the host already has a live reservation on that date
     */
    @Transactional
    public TourListingResponse createListing(CreateTourListingDto dto, Integer hostId) {
        User host = userRepo
                .findById(hostId)
                .orElseThrow(() -> new UserNotFoundException(hostId));

        if (!hostAvailableAtDate(host, dto.meetingDate().toLocalDate())) {
            log.warn("Host with id {} has already listed a listing on the date {}.",
                    host.getId(), dto.meetingDate().toLocalDate());
            throw new HostAlreadyTakenException(hostId);
        }

        TourListing saved = listingRepo.save(
                new TourListing(
                        host,
                        dto.city(),
                        dto.longitude(),
                        dto.latitude(),
                        dto.meetingDate(),
                        dto.tourDescription(),
                        dto.maxGuests()
                )
        );
        log.info("Created TourListing with id {}", saved.getId());

        return createListingResponse(saved, 0, true);
    }

    /**
     * Creates a {@link TourListing} pre-filled from an existing {@link PartnerEvent}.
     *
     * <p>Default values are pulled from the event and its parent pin:
     * <ul>
     *   <li>Meeting date → {@code event.startDatetime} (unless {@code req.overrideMeetingDate} is present)</li>
     *   <li>Latitude / longitude → pin coordinates (unless the caller provides overrides)</li>
     * </ul>
     *
     * <p>The city field is always taken from the request — {@code PartnerPin} stores only
     * coordinates so there is no city to inherit.
     *
     * @param hostId the authenticated user creating the listing
     * @param req    request body with required fields and optional overrides
     * @return the created listing response
     * @throws PartnerEventNotFoundException if no event exists with the given ID
     * @throws UserNotFoundException         if the host user does not exist
     * @throws HostAlreadyTakenException     if the host already has a live reservation that day
     */
    @Transactional
    public TourListingResponse createFromPartnerEvent(Integer hostId, CreateListingFromEventRequest req) {
        PartnerEvent event = partnerEventRepo.findById(req.partnerEventId())
                .orElseThrow(() -> new PartnerEventNotFoundException(req.partnerEventId()));

        PartnerPin pin = event.getPartnerPin();

        LocalDateTime meetingDate = req.overrideMeetingDate() != null
                ? req.overrideMeetingDate()
                : event.getStartDatetime();

        double latitude  = req.overrideLatitude()  != null ? req.overrideLatitude()  : pin.getLatitude();
        double longitude = req.overrideLongitude() != null ? req.overrideLongitude() : pin.getLongitude();

        CreateTourListingDto dto = new CreateTourListingDto(
                req.city(),
                longitude,
                latitude,
                meetingDate,
                req.tourDescription(),
                req.maxGuests()
        );

        return createListing(dto, hostId);
    }

    /**
     * Finds a single listing by ID. The exact meeting location is revealed only if the
     * viewer is allowed to see it (e.g. the host or a guest with a live reservation);
     * otherwise an obfuscated location is returned.
     *
     * @param listingId the listing ID
     * @param viewerId  ID of the user viewing the listing
     * @return the {@link TourListingResponse} if found, otherwise empty
     */
    public Optional<TourListingResponse> getListingById(Integer listingId, Integer viewerId) {
        return listingRepo.findById(listingId)
                .map(listing -> {
                    boolean exact = canViewExactLocation(listing, viewerId);
                    return createListingResponse(listing, 0, exact);
                });
    }

    /*private List<TourListingResponse> getListingsByHost(Integer hostId, Sort sort) {
        return listingRepo.findAllByHost_Id(hostId, sort)
                .stream()
                .map(l -> createListingResponse(l, 0, false))
                .toList();
    }*/

    /**
     * Returns a page of listings hosted by the given user, sorted by meeting date.
     * The host always sees the exact location of their own listings.
     *
     * @param userId    ID of the host whose listings are listed
     * @param filter    reserved for future filtering (currently unused)
     * @param direction sort direction on the meeting date
     * @param pageable  pagination settings
     * @return a page of {@link TourListingResponse}
     */
    public Page<TourListingResponse> getMyListings(Integer userId, String filter, Sort.Direction direction, Pageable pageable) {
        Sort sort = Sort.by(direction, "meetingDate");
        Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return listingRepo.findAllByHost_Id(userId, pageableWithSort)
                .map(listing -> createListingResponse(listing, 0, true));
    }

    /**
     * Returns a page of listings hosted by users other than the viewer, each enriched with
     * its active booked-guest count and an exact location only for listings the viewer has a
     * live reservation for.
     *
     * @param resourceOwnerId ID of the viewing user (their own listings are excluded)
     * @param pageable        pagination settings
     * @return a page of {@link TourListingResponse}
     */
    public Page<TourListingResponse> getAllListings(Integer resourceOwnerId, Pageable pageable) {
        Page<TourListing> page = listingRepo.findAllByHost_IdNot(resourceOwnerId, pageable);

        List<Integer> ids = page.stream().map(TourListing::getId).toList();
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE);

        Map<Integer, Long> bookedCounts = reservationRepo.countByListingIdsAndStatusIn(ids, activeStatuses)
                .stream()
                .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));

        // Single query: which listings on this page does the viewer have a reservation for?
        Set<Integer> revealedIds = Set.copyOf(
                reservationRepo.findListingIdsWithReservationByViewer(resourceOwnerId, ids, activeStatuses)
        );

        return page.map(listing -> createListingResponse(
                listing,
                bookedCounts.getOrDefault(listing.getId(), 0L).intValue(),
                revealedIds.contains(listing.getId())
        ));
    }

    /**
     * Updates a listing's meeting date and/or description. Only the host may update it.
     *
     * @param id       the listing ID
     * @param dto      fields to update (null fields are left unchanged)
     * @param callerId ID of the authenticated user requesting the update
     * @return the updated {@link TourListingResponse}
     * @throws TourListingNotFoundException if no listing exists with the given ID
     * @throws ForbiddenActionException     if the caller is not the listing's host
     */
    public TourListingResponse updateListing(Integer id, UpdateTourListingDto dto, Integer callerId) {
        TourListing listing = listingRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Could not find TourListing with id {}", id);
                    return new TourListingNotFoundException(id);
                });

        if (!listing.getHost().getId().equals(callerId))
            throw new ForbiddenActionException("Only the host can update their listing.");

        applyListingUpdate(listing, dto);
        TourListing saved = listingRepo.save(listing);
        log.info("Updated TourListing with id {}", saved.getId());
        return createListingResponse(saved, 0, true);
    }

    // For internal/event use — no ownership check needed (trusted system operation)
    TourListingResponse updateListing(Integer id, UpdateTourListingDto dto) {
        TourListing listing = listingRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Could not find TourListing with id {}", id);
                    return new TourListingNotFoundException(id);
                });

        applyListingUpdate(listing, dto);
        TourListing saved = listingRepo.save(listing);
        log.info("Updated TourListing with id {}", saved.getId());
        return createListingResponse(saved, 0, true);
    }

    private void applyListingUpdate(TourListing listing, UpdateTourListingDto dto) {
        if (dto.meetingDate() != null) listing.setMeetingDate(dto.meetingDate());
        if (dto.tourDescription() != null) listing.setTourDescription(dto.tourDescription());
    }

    /**
     * Deletes a listing and publishes a {@link TourListingDeletedEvent} so dependent
     * reservations and booking requests are cleaned up. Only the host may delete it.
     *
     * @param id       the listing ID
     * @param callerId ID of the authenticated user requesting deletion
     * @throws TourListingNotFoundException if no listing exists with the given ID
     * @throws ForbiddenActionException     if the caller is not the listing's host
     */
    @Transactional
    public void deleteListing(Integer id, Integer callerId) {
        TourListing listing = listingRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Could not find TourListing with id {} to delete.", id);
                    return new TourListingNotFoundException(id);
                });

        if (!listing.getHost().getId().equals(callerId))
            throw new ForbiddenActionException("Only the host can delete their listing.");

        publisher.publishEvent(new TourListingDeletedEvent(listing.getId()));

        listingRepo.deleteById(id);
        log.info("Deleted TourListing with id {}", id);
    }

    /**
     * Returns true if the host has no CONFIRMED or ACTIVE reservation on the given date.
     * A host may create a new listing even if they have an unreserved or already-closed
     * listing on the same day — only a live reservation blocks them.
     */
    private boolean hostAvailableAtDate(User host, LocalDate meetingDate) {
        List<ReservationStatus> blockingStatuses = List.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE);
        return !reservationRepo.existsByTourListing_Host_IdAndTourListing_MeetingDateBetweenAndStatusIn(
                host.getId(),
                DateTimeUtil.getStartOfDay(meetingDate),
                DateTimeUtil.getEndOfDay(meetingDate),
                blockingStatuses
        );
    }

    @Transactional
    @Scheduled(fixedRate = 1800000)
    public void cleanUpExpiredListings(){
        log.debug("Attempting to clean up expired listings");
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE);
        List<Integer> listingIds = listingRepo.findExpiredListingsWithNoActiveReservations(
                LocalDateTime.now(),
                activeStatuses
        ).stream()
                .map(TourListing::getId)
                .toList();

        publisher.publishEvent(new TourListingsDeletedEvent(listingIds));

        listingRepo.deleteAllById(listingIds);
        log.info("Deleted expired listings.");
    }

    @EventListener
    void handleUserDeleted(UserDeletedEvent event) {
        Integer hostId = event.userId();
        if (!listingRepo.existsByHost_Id(hostId)) {
            log.warn("Could not find a single TourListing associated with host of id {}.", hostId);
            return;
        }

        deleteListingsByHostId(hostId);
    }

    private void deleteListingsByHostId(Integer hostId) {
        publisher.publishEvent(
                new TourListingsDeletedForHostEvent(hostId)
        );
        int count = listingRepo.deleteAllByHost_Id(hostId);
        log.info("Deleted {} TourListing/s with by host id {}", count, hostId);
    }

    /**
     * Host killswitch to manually start the tour.
     * Requires the host to be checked in; ignores guest check-in state so the host
     * is never held hostage by no-shows. Publishes {@link TourStartedEvent} which
     * activates all already-checked-in reservations via {@code ReservationService}.
     *
     * @param listingId ID of the listing whose tour to start
     * @param hostId    ID of the authenticated caller (must be the listing host)
     * @throws TourListingNotFoundException     if the listing does not exist
     * @throws ForbiddenActionException         if the caller is not the host
     * @throws IllegalReservationStateException if the tour already started or the host has not checked in
     */
    @Transactional
    public void startTour(Integer listingId, Integer hostId) {
        TourListing listing = listingRepo.findById(listingId)
                .orElseThrow(() -> new TourListingNotFoundException(listingId));

        if (!listing.getHost().getId().equals(hostId))
            throw new ForbiddenActionException("Only the host can start the tour.");

        if (listing.isTourStarted())
            throw new IllegalReservationStateException("Tour already started.");

        // host must be present before manually starting
        if (!listing.isHostCheckedIn())
            throw new IllegalReservationStateException("Host must check in before starting the tour.");

        listing.startTour();
        listingRepo.save(listing);
        publisher.publishEvent(new TourStartedEvent(listingId));
        log.info("Tour manually started for listing {}", listingId);
    }

    /**
     * Returns true if {@code viewerId} may receive the exact coordinates of {@code listing}.
     * Exact coordinates are revealed when the viewer is the host, or holds a CONFIRMED/ACTIVE
     * reservation for that listing.
     */
    private boolean canViewExactLocation(TourListing listing, Integer viewerId) {
        if (listing.getHost().getId().equals(viewerId)) return true;
        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE);
        return reservationRepo.existsByGuest_IdAndTourListing_IdAndStatusIn(viewerId, listing.getId(), activeStatuses);
    }

    private TourListingResponse createListingResponse(TourListing listing, int bookedCount, boolean exact) {
        Double lat;
        Double lng;
        Integer radiusMeters;

        if (exact) {
            lat = listing.getLatitude();
            lng = listing.getLongitude();
            radiusMeters = null;
        } else {
            LocationObfuscator.ObfuscatedLocation fuzzed =
                    LocationObfuscator.obfuscate(listing.getLatitude(), listing.getLongitude(), listing.getId());
            lat = fuzzed.latitude();
            lng = fuzzed.longitude();
            radiusMeters = fuzzed.radiusMeters();
        }

        return new TourListingResponse(
                listing.getId(),
                listing.getCity(),
                lng,
                lat,
                radiusMeters,
                listing.getMeetingDate(),
                listing.getPostDate(),
                listing.getTourDescription(),
                listing.getMaxGuests(),
                bookedCount,
                detailsMapper.mapUserDetails(listing.getHost())
        );
    }
}
