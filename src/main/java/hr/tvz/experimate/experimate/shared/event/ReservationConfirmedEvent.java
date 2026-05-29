package hr.tvz.experimate.experimate.shared.event;

import java.time.LocalDateTime;

/**
 * Published when a reservation is created and confirmed.
 * Used to schedule push notification reminders one hour before the tour starts.
 *
 * @param reservationId the ID of the confirmed reservation — used to cancel guest reminders
 * @param guestId       the ID of the guest who confirmed the reservation
 * @param hostId        the ID of the tour host
 * @param listingId     the ID of the tour listing — used to deduplicate host reminders
 *                      when a listing has multiple confirmed guests
 * @param meetingDate   the scheduled start time of the tour
 */
public record ReservationConfirmedEvent(Integer reservationId, Integer guestId, Integer hostId, Integer listingId, LocalDateTime meetingDate) {
}
