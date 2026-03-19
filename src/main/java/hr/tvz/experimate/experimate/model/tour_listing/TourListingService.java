package hr.tvz.experimate.experimate.model.tour_listing;

import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TourListingService {

    private static final Logger log = LoggerFactory.getLogger(TourListingService.class);

    private final TourListingRepo listingRepo;
    private final UserRepo userRepo;

    public TourListingService(TourListingRepo repo,
                              UserRepo userRepo) {
        this.listingRepo = repo;
        this.userRepo = userRepo;
    }

    public TourListing createListing(CreateTourListingDto dto) {
        User host = userRepo
                .findById(dto.hostId())
                .orElseThrow(() -> new UserNotFoundException(dto.hostId()));

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

        if (dto.meetingDate() != null) listing.setMeetingDate(dto.meetingDate());
        if (dto.tourDescription() != null) listing.setTourDescription(dto.tourDescription());

        TourListing saved = listingRepo.save(listing);
        log.info("Updated TourListing with id {}", listing.getId());

        return saved;
    }

    public void deleteListing(Integer id) {
        TourListing listing = listingRepo
                .findById(id)
                .orElseThrow(() -> {
                   log.warn("Could not find TourListing with id {}", id);
                   return new TourListingNotFoundException(id);
                });
        listingRepo.deleteById(id);
        log.info("Deleted TourListing with id {}", id);
    }
}
