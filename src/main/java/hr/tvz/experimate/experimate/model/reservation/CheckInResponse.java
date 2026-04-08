package hr.tvz.experimate.experimate.model.reservation;

import java.time.LocalDateTime;

public record CheckInResponse(Integer reservationId,
                              ReservationStatus status,
                              boolean guestCheckedIn,
                              boolean hostCheckedIn,
                              LocalDateTime guestCheckInTimestamp,
                              LocalDateTime hostCheckInTimestamp,
                              LocalDateTime statusTimestamp) {
}
