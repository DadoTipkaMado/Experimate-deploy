package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.shared.event.BookingRequestAcceptedEvent;
import hr.tvz.experimate.experimate.shared.event.BookingRequestCreatedEvent;
import hr.tvz.experimate.experimate.shared.event.BookingRequestDeclinedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Translates domain events into Web Push notifications.
 *
 * <p>Each handler listens for a specific domain event and delegates to
 * {@link PushNotificationService#sendToUser} with a human-readable message.
 * Domain services have no knowledge of this class — the decoupling is intentional.
 *
 * <p>Handlers that listen on non-transactional events use {@link EventListener}.
 * Handlers for events published inside a {@code @Transactional} method must use
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} to ensure the push
 * is sent only after the DB change is visible to the push service infrastructure.
 */
@Component
public class PushNotificationListener {

    private final PushNotificationService pushNotificationService;

    public PushNotificationListener(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Notifies the host when a guest submits a new booking request.
     * Uses plain {@link EventListener} because {@code createBookingRequest} is not
     * transactional — the booking request is already committed before this fires.
     *
     * @param event carries the host's user ID and the guest's username
     */
    @EventListener
    public void onBookingRequestCreated(BookingRequestCreatedEvent event) {
        pushNotificationService.sendToUser(
                event.hostId(),
                "New booking request",
                event.guestUsername() + " wants to join your tour",
                "/requests"
        );
    }

    /**
     * Notifies the guest when their booking request is accepted.
     * Uses {@link TransactionalEventListener} with {@code AFTER_COMMIT} because
     * {@code acceptBookingRequest} is transactional — we send the push only after
     * the reservation is committed and visible in the database.
     *
     * @param event carries the guest's user ID
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingRequestAccepted(BookingRequestAcceptedEvent event) {
        pushNotificationService.sendToUser(
                event.guestId(),
                "Booking request accepted",
                "Your booking was accepted. See you on the tour!",
                "/reservations"
        );
    }

    /**
     * Notifies the guest when their booking request is declined.
     * Uses plain {@link EventListener} because {@code declineBookingRequest} is not
     * transactional — the status update is committed before this fires.
     *
     * @param event carries the guest's user ID
     */
    @EventListener
    public void onBookingRequestDeclined(BookingRequestDeclinedEvent event) {
        pushNotificationService.sendToUser(
                event.guestId(),
                "Booking request declined",
                "Unfortunately, your booking request was not accepted.",
                "/requests"
        );
    }
}
