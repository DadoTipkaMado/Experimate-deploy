package hr.tvz.experimate.experimate.model.tour_listing;

public record TourListingResponse(
        Integer id,
        String city,
        Double longitude,
        Double latitude
) {
}
