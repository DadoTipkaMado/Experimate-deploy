package hr.tvz.experimate.experimate.domain.reservation.response;

import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;

import java.time.LocalDateTime;

public record EndTourResponse(Integer reservationId,
                              ReservationStatus status,
                              String endedByUsername,
                              LocalDateTime endTimestamp) {
}
