package hr.tvz.experimate.experimate.model.tour_listing;

public class TourListingAlreadyReservedException extends RuntimeException {
    public TourListingAlreadyReservedException(Integer id) {
        super("Tour listing with id %s is already reserved!");
    }
}
