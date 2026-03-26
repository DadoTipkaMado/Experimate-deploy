package hr.tvz.experimate.experimate.model.shared.events;

import java.util.List;

public record ReservationsDeletedEvent (List<Integer> tourListingIds) {
}
