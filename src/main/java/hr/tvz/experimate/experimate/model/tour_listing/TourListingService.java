package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.shared.UserDetails;
import hr.tvz.experimate.experimate.model.shared.event.*;
import hr.tvz.experimate.experimate.model.shared.util.DateTimeUtil;
import hr.tvz.experimate.experimate.model.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.model.tour_listing.exception.HostAlreadyTakenException;
import hr.tvz.experimate.experimate.model.tour_listing.exception.TourListingNotFoundException;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
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
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TourListingService {

    private static final Logger log = LoggerFactory.getLogger(TourListingService.class);

    private final TourListingRepo listingRepo;
    private final UserRepo userRepo;
    private final ApplicationEventPublisher publisher;

    public TourListingService(TourListingRepo repo,
                              UserRepo userRepo,
                              ApplicationEventPublisher publisher) {
        this.listingRepo = repo;
        this.userRepo = userRepo;
        this.publisher = publisher;
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
                        dto.tourDescription()
                )
        );
        log.info("Created TourListing with id {}", saved.getId());

        return createListingResponse(saved);
    }

    public Optional<TourListingResponse> getListingById(Integer id) {
        return listingRepo.findById(id)
                .map(this::createListingResponse);
    }

    private List<TourListingResponse> getListingsByHost(Integer hostId, Sort sort) {
        return listingRepo.findAllByHost_Id(hostId, sort)
                .stream()
                .map(this::createListingResponse)
                .toList();
    }

    public Page<TourListingResponse> getMyListings(Integer userId, String filter, Sort.Direction direction, Pageable pageable) {
        Sort sort = Sort.by(direction, "meetingDate");
        Pageable pageableWithSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return listingRepo.findAllByHost_Id(userId, pageableWithSort)
                .map(this::createListingResponse);
    }

    public Page<TourListingResponse> getAllListings(Integer resourceOwnerId, Pageable pageable) {
        return listingRepo.findAllByHost_IdNot(resourceOwnerId, pageable)
                .map(this::createListingResponse);
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
        return createListingResponse(saved);
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
        return createListingResponse(saved);
    }

    private void applyListingUpdate(TourListing listing, UpdateTourListingDto dto) {
        if (dto.meetingDate() != null) listing.setMeetingDate(dto.meetingDate());
        if (dto.tourDescription() != null) listing.setTourDescription(dto.tourDescription());
        listing.setReserved(dto.isReserved());
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

    private boolean hostAvailableAtDate(User host, LocalDate meetingDate) {
        return !listingRepo.existsByHostAndMeetingDateBetween(
                host,
                DateTimeUtil.getStartOfDay(meetingDate),
                DateTimeUtil.getEndOfDay(meetingDate)
        );
    }

    @Transactional
    @Scheduled(fixedRate = 1800000)
    public void cleanUpExpiredListings(){
        log.debug("Attempting to clean up expired listings");
        List<Integer> listingIds = listingRepo.findAllByReservedAndMeetingDateBefore(
                false,
                LocalDateTime.now()
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

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void handleReservationsDeletedEvent(ReservationsDeletedEvent event) {
        log.info("IDS SENT BY EVENT {} ", event.tourListingIds());
        AtomicInteger count = new AtomicInteger(0);
        UpdateTourListingDto updateDto = new UpdateTourListingDto(
                null,
                null,
                false
        );
        //update reserved attribute to false for each listing
        event.tourListingIds()
                .forEach(id -> {
                    updateListing(id, updateDto);
                    count.getAndIncrement();
                });

        log.info("{} listings flagged as not reserved.", count);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void handleTourListingReservedEvent(TourListingReservedEvent event) {
        updateListing(event.listingId(), event.updateDetails());
        log.info("Tour listing with id {} is now reserved.", event.listingId());
    }

    private void deleteListingsByHostId(Integer hostId) {
        publisher.publishEvent(
                new TourListingsDeletedForHostEvent(hostId)
        );
        int count = listingRepo.deleteAllByHost_Id(hostId);
        log.info("Deleted {} TourListing/s with by host id {}", count, hostId);
    }

    private TourListingResponse createListingResponse(TourListing listing){
        User host = listing.getHost();
        UserDetails hostDetails = new UserDetails(
                host.getFirstName(),
                host.getLastName(),
                host.getUsername()
        );

        return new TourListingResponse(
                listing.getId(),
                listing.getCity(),
                listing.getLongitude(),
                listing.getLatitude(),
                listing.getMeetingDate(),
                listing.getPostDate(),
                listing.getTourDescription(),
                listing.isReserved(),
                hostDetails
        );
    }

    //TODO dodaj metodu koja ce provjeravati jesu li dto atributi ispravni.

    //TODO dodaj helper metode koje ce extractati entitete iz dto
}
