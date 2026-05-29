package hr.tvz.experimate.experimate.shared.event;

/**
 * Published when a guest submits a new booking request.
 * The host is notified via push so they can promptly accept or decline.
 *
 * @param hostId        the ID of the tour host who should receive the notification
 * @param guestUsername the username of the guest who submitted the request
 */
public record BookingRequestCreatedEvent(Integer hostId, String guestUsername) {
}
