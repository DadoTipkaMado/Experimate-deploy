package hr.tvz.experimate.experimate.domain.reservation.response;

import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;

import java.time.LocalDateTime;

public record CheckInResponse(Integer reservationId,
                              ReservationStatus status,
                              boolean guestCheckedIn,
                              boolean hostCheckedIn,
                              LocalDateTime guestCheckInTimestamp,
                              LocalDateTime hostCheckInTimestamp,
                              LocalDateTime statusTimestamp) {
}
