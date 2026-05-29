package hr.tvz.experimate.experimate.shared.event;

import java.time.LocalDateTime;

/**
 * Published when a reservation is cancelled by either the guest or the host.
 * Used to cancel scheduled "tour starts in 1 hour" push reminders so that
 * participants do not receive notifications for tours they are no longer attending.
 *
 * @param reservationId the ID of the cancelled reservation
 * @param listingId     the ID of the associated tour listing — used to decide
 *                      whether the host reminder should also be cancelled
 * @param meetingDate   the scheduled tour start time — used by the cleanup job to
 *                      determine when the corresponding reminder task would have fired
 */
public record ReservationCancelledEvent(Integer reservationId, Integer listingId, LocalDateTime meetingDate) {
}
