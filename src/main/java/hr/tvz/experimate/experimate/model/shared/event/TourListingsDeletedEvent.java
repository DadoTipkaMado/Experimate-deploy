package hr.tvz.experimate.experimate.model.shared.event;

import java.util.List;

public record TourListingsDeletedEvent(List<Integer> listingIds) {
}
