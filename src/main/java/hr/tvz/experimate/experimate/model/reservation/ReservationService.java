package hr.tvz.experimate.experimate.model.reservation;

import hr.tvz.experimate.experimate.model.tour_listing.TourListing;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingAlreadyReservedException;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingNotFoundException;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingRepo;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepo reservationRepo;
    private final UserRepo userRepo;
    private final TourListingRepo tourListingRepo;

    public ReservationService(ReservationRepo reservationRepo,
                              UserRepo userRepo,
                              TourListingRepo tourListingRepo) {
        this.reservationRepo = reservationRepo;
        this.userRepo = userRepo;
        this.tourListingRepo = tourListingRepo;
    }

    @Transactional
    public Reservation createReservation(CreateReservationDto dto){
        ValidatedReservationData validatedData = validateCreationDto(dto);

        User validatedGuest = validatedData.guest();
        TourListing validatedListing = validatedData.listing();

        Reservation reservation = reservationRepo.save(
                new Reservation(
                validatedGuest,
                validatedListing
        ));
        validatedListing.setReserved(true);

        log.info("Created reservation with id {}", reservation.getId());

        return reservation;
    }

    public List<Reservation> getAllReservations(){
        return reservationRepo.findAll();
    }

    public Optional<Reservation> getReservationById(Integer id){
        return reservationRepo.findById(id);
    }

    public void deleteReservation(Integer id){
        if(reservationRepo.existsById(id)){
            reservationRepo.deleteById(id);
            log.info("Reservation deleted with id {}", id);
        }
        else {
            log.warn("Reservation not found with id {}", id);
            throw new ReservationNotFoundException(
                    "Reservation with id %d could not be deleted. Not found.".formatted(id
                    ));
        }
    }

    private ValidatedReservationData validateCreationDto(CreateReservationDto dto){
        User guest = extractUser(dto);
        TourListing listing = extractListing(dto);

        if(dto.guestId().equals(listing.getHost().getId())) {
            log.warn("Guest's id matches host's id {}", guest.getId());
            throw new IllegalArgumentException(
                    "Guest id cannot be the same as the tour listing's host id."
            );
        }

        if(listing.isReserved()){
            log.warn("Tour listing with id {} is already reserved!", listing.getId());
            throw new TourListingAlreadyReservedException(listing.getId());
        }

        return new ValidatedReservationData(guest, listing);
    }

    private User extractUser(CreateReservationDto dto){
        return userRepo.findById(dto.guestId())
                .orElseThrow(() -> new UserNotFoundException(dto.guestId()));

    }

    private TourListing extractListing(CreateReservationDto dto){
        return tourListingRepo.findById(dto.tourListingId())
                .orElseThrow(() -> new TourListingNotFoundException(dto.tourListingId()));
    }

    private record ValidatedReservationData(User guest, TourListing listing) {
    }
}
