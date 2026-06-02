package hr.tvz.experimate.experimate.shared.event;

/**
 * Published when a host declines a guest's booking request.
 * The guest is notified via push so they can look for another tour.
 *
 * @param guestId the ID of the guest whose request was declined
 */
public record BookingRequestDeclinedEvent(Integer guestId) {
}
