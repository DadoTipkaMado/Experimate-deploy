package hr.tvz.experimate.experimate.shared.event;

import java.util.List;

public record TourListingsDeletedEvent(List<Integer> listingIds) {
}
