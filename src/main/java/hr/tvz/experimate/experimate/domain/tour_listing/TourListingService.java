package hr.tvz.experimate.experimate.domain.tour_listing;

import hr.tvz.experimate.experimate.domain.partner_event.PartnerEvent;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventNotFoundException;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventRepository;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPin;
import hr.tvz.experimate.experimate.domain.tour_listing.dto.*;
import hr.tvz.experimate.experimate.domain.tour_listing.response.*;

import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.shared.DetailsMapper;
import hr.tvz.experimate.experimate.shared.event.*;
import hr.tvz.experimate.experimate.shared.util.DateTimeUtil;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.domain.reservation.exception.IllegalReservationStateException;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.HostAlreadyTakenException;
import hr.tvz.experimate.experimate.domain.tour_listing.exception.TourListingNotFoundException;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
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

import hr.tvz.experimate.experimate.shared.util.LocationObfuscator;

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

    //TODO refraktoriraj ovo sa provjerenim podcaim iz dto
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

    public Page<TourListingResponse> getMyListings(Integer userId, String filter, Sort.Direction direction, Pageable pageable) {
        Sort sort = Sort.by(direction, "meetingDate");
        Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return listingRepo.findAllByHost_Id(userId, pageableWithSort)
                .map(listing -> createListingResponse(listing, 0, true));
    }

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

    //TODO dodaj metodu koja ce provjeravati jesu li dto atributi ispravni.

    //TODO dodaj helper metode koje ce extractati entitete iz dto
}
