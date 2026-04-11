package hr.tvz.experimate.experimate.model.reservation;

import java.time.LocalDateTime;

public record EndTourResponse(Integer reservationId,
                              ReservationStatus status,
                              String endedByUsername,
                              LocalDateTime endTimestamp) {
}
