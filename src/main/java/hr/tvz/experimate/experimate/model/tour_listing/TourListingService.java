package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.shared.event.ReservationsDeletedEvent;
import hr.tvz.experimate.experimate.model.shared.event.TourListingDeletedEvent;
import hr.tvz.experimate.experimate.model.shared.event.TourListingsDeletedForHostEvent;
import hr.tvz.experimate.experimate.model.shared.event.UserDeletedEvent;
import hr.tvz.experimate.experimate.model.shared.util.DateTimeUtil;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
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
    public TourListing createListing(CreateTourListingDto dto) {
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
                        dto.meetingDate(),
                        dto.tourDescription()
                )
        );
        log.info("Created TourListing with id {}", saved.getId());

        return saved;
    }

    public Optional<TourListing> getListingById(Integer id) {
        return listingRepo.findById(id);
    }

    public List<TourListing> getAllListings() {
        return listingRepo.findAll();
    }

    public TourListing updateListing(Integer id, UpdateTourListingDto dto) {
        TourListing listing = listingRepo
                .findById(id)
                .orElseThrow(() -> {
                    log.warn("Could not find TourListing with id {}", id);
                    return new TourListingNotFoundException(id);
                });

        //TODO napravi privatnu metodu koja validira ove atribute
        if (dto.meetingDate() != null) listing.setMeetingDate(dto.meetingDate());
        if (dto.tourDescription() != null) listing.setTourDescription(dto.tourDescription());
        listing.setReserved(dto.reservedStatus());

        TourListing saved = listingRepo.save(listing);
        log.info("Updated TourListing with id {}", listing.getId());

        return saved;
    }

    @Transactional
    public void deleteListing(Integer id) {
        TourListing listing = listingRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Could not find TourListing with id {} to delete.", id);
                    return new TourListingNotFoundException(id);
                });

        if (listing.isReserved())
            publisher.publishEvent(new TourListingDeletedEvent(id));

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

    private void deleteListingsByHostId(Integer hostId) {
        publisher.publishEvent(
                new TourListingsDeletedForHostEvent(hostId)
        );
        int count = listingRepo.deleteAllByHost_Id(hostId);
        log.info("Deleted {} TourListing/s with by host id {}", count, hostId);
    }

    //TODO dodaj metodu koja ce provjeravati jesu li dto atributi ispravni.

    //TODO dodaj helper metode koje ce extractati entitete iz dto
}
