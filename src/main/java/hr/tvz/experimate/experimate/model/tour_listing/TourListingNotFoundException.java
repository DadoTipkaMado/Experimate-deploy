package hr.tvz.experimate.experimate.model.tour_listing;

public class TourListingNotFoundException extends RuntimeException {
    public TourListingNotFoundException(Integer id) {
        super("Listing not found with id: " + id);
    }
}
