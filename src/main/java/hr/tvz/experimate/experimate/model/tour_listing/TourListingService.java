package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.shared.UserDetails;
import hr.tvz.experimate.experimate.model.shared.event.*;
import hr.tvz.experimate.experimate.model.shared.util.DateTimeUtil;
import hr.tvz.experimate.experimate.model.tour_listing.exception.HostAlreadyTakenException;
import hr.tvz.experimate.experimate.model.tour_listing.exception.TourListingNotFoundException;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    public TourListingResponse createListing(CreateTourListingDto dto) {
        User host = userRepo
                .findById(dto.hostId())
                .orElseThrow(() -> new UserNotFoundException(dto.hostId()));

        if (!hostAvailableAtDate(host, dto.meetingDate().toLocalDate())) {
            log.warn("Host with id {} has already listed a listing on the date {}.",
                    host.getId(), dto.meetingDate().toLocalDate());
            throw new HostAlreadyTakenException(dto.hostId());
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
                .map(listing -> createListingResponse(listing));
    }

    public List<TourListingResponse> getAllListings() {
        return listingRepo.findAll()
                .stream()
                .map(listing -> createListingResponse(listing))
                .toList();
    }

    public TourListingResponse updateListing(Integer id, UpdateTourListingDto dto) {
        TourListing listing = listingRepo
                .findById(id)
                .orElseThrow(() -> {
                    log.warn("Could not find TourListing with id {}", id);
                    return new TourListingNotFoundException(id);
                });

        //TODO napravi privatnu metodu koja validira ove atribute
        if (dto.meetingDate() != null) listing.setMeetingDate(dto.meetingDate());
        if (dto.tourDescription() != null) listing.setTourDescription(dto.tourDescription());
        listing.setReserved(dto.isReserved());

        TourListing saved = listingRepo.save(listing);
        log.info("Updated TourListing with id {}", saved.getId());

        return createListingResponse(saved);
    }

    @Transactional
    public void deleteListing(Integer id) {
        TourListing listing = listingRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Could not find TourListing with id {} to delete.", id);
                    return new TourListingNotFoundException(id);
                });

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
