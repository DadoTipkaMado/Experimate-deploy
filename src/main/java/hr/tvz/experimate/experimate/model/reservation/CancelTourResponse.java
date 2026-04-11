package hr.tvz.experimate.experimate.model.reservation;

import java.time.LocalDateTime;

public record CancelTourResponse(Integer reservationId,
                                 ReservationStatus status,
                                 String cancelledByUsername,
                                 LocalDateTime cancelTimestamp) {
}
