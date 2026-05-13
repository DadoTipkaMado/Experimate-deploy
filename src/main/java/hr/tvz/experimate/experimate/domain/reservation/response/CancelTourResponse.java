package hr.tvz.experimate.experimate.domain.reservation.response;

import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;

import java.time.LocalDateTime;

public record CancelTourResponse(Integer reservationId,
                                 ReservationStatus status,
                                 String cancelledByUsername,
                                 LocalDateTime cancelTimestamp) {
}
