package hr.tvz.experimate.experimate.model.reservation;

import java.time.LocalDateTime;

public record ReservationResponse(
        Integer id,
        LocalDateTime dateOfReservation,
        LocalDateTime meetingDate,
        String city,
        String hostFirstName,
        String hostLastName,
        String hostUsername,
        String guestFirstName,
        String guestLastName,
        String guestUsername
) {
}
