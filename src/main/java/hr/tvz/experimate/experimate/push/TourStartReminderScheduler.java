package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.domain.reservation.Reservation;
import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.shared.event.ReservationCancelledEvent;
import hr.tvz.experimate.experimate.shared.event.ReservationConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schedules "tour starts in 1 hour" push notifications dynamically using {@link TaskScheduler}.
 *
 * <p>When a reservation is confirmed, one reminder task is registered for the guest and —
 * if not already registered for that listing — one for the host. The host deduplication
 * prevents N identical notifications when a tour has N confirmed guests.
 *
 * <p>Cancellation is handled by two maps ({@code cancelledReservations} and
 * {@code cancelledListings}) keyed by ID, with the scheduled reminder time as the value.
 * When a task fires it checks the relevant map: if the key is present the push is skipped.
 * A nightly cleanup job removes map entries whose reminder time is past, evicting any
 * entries that were never self-cleaned (e.g. reservation cancelled after the reminder fired).
 *
 * <p>On application startup all future confirmed reservations are re-scheduled so that
 * reminders survive server restarts.
 */
@Component
public class TourStartReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(TourStartReminderScheduler.class);

    private final TaskScheduler taskScheduler;
    private final ReservationRepo reservationRepo;
    private final PushNotificationService pushNotificationService;

    /** Listing IDs for which a host reminder has already been scheduled in this JVM lifetime. */
    private final Set<Integer> scheduledHostListings = ConcurrentHashMap.newKeySet();

    /**
     * Reservation IDs whose guest reminder should be suppressed.
     * Value is the time the reminder task was scheduled to fire, used by the cleanup job.
     */
    private final Map<Integer, Instant> cancelledReservations = new ConcurrentHashMap<>();

    /**
     * Listing IDs whose host reminder should be suppressed.
     * Value is the time the reminder task was scheduled to fire, used by the cleanup job.
     */
    private final Map<Integer, Instant> cancelledListings = new ConcurrentHashMap<>();

    public TourStartReminderScheduler(TaskScheduler taskScheduler,
                                      ReservationRepo reservationRepo,
                                      PushNotificationService pushNotificationService) {
        this.taskScheduler = taskScheduler;
        this.reservationRepo = reservationRepo;
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Fires after a reservation is committed to the database.
     * Schedules a guest reminder and a host reminder (only for the first confirmed guest
     * on a given listing, to avoid duplicate notifications to the host).
     *
     * <p>Uses {@code AFTER_COMMIT} because {@code createReservation} runs inside a
     * transaction — we only schedule after the data is visible in the DB.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        scheduleGuestReminder(event.reservationId(), event.guestId(), event.meetingDate());

        if (scheduledHostListings.add(event.listingId())) {
            scheduleHostReminder(event.listingId(), event.hostId(), event.meetingDate());
        }
    }

    /**
     * Fires when a reservation is cancelled. Marks the guest reminder for suppression.
     * If no confirmed guests remain on the listing, the host reminder is also suppressed.
     *
     * <p>Uses plain {@link EventListener} because {@code cancelTour} is not transactional.
     */
    @EventListener
    public void onReservationCancelled(ReservationCancelledEvent event) {
        Instant taskFiresAt = toInstant(event.meetingDate().minusHours(1));
        cancelledReservations.put(event.reservationId(), taskFiresAt);
        log.debug("Suppressed guest reminder for cancelled reservation {}", event.reservationId());

        long remaining = reservationRepo.countByTourListing_IdAndStatus(
                event.listingId(), ReservationStatus.CONFIRMED);
        if (remaining == 0) {
            cancelledListings.put(event.listingId(), taskFiresAt);
            scheduledHostListings.remove(event.listingId());
            log.debug("Suppressed host reminder for listing {} — no confirmed guests remain", event.listingId());
        }
    }

    /**
     * Runs once after the application is fully started.
     * Re-schedules reminders for all future confirmed reservations so that
     * tasks are not permanently lost after a server restart or redeploy.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        LocalDateTime oneHourFromNow = LocalDateTime.now().plusHours(1);
        List<Reservation> upcoming = reservationRepo.findAllByStatusInAndTourListing_MeetingDateAfter(
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE),
                oneHourFromNow
        );

        for (Reservation r : upcoming) {
            LocalDateTime meetingDate = r.getTourListing().getMeetingDate();
            scheduleGuestReminder(r.getId(), r.getGuest().getId(), meetingDate);

            if (scheduledHostListings.add(r.getTourListing().getId())) {
                scheduleHostReminder(r.getTourListing().getId(), r.getTourListing().getHost().getId(), meetingDate);
            }
        }

        log.info("Recovered {} tour start reminder(s) on startup", upcoming.size());
    }

    /**
     * Runs nightly at 01:00 and removes map entries whose reminder time is in the past.
     * This evicts the small number of entries that were not self-cleaned by the task lambda —
     * specifically, reservations cancelled after their reminder already fired.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void cleanupExpiredCancellations() {
        Instant now = Instant.now();
        int reservationsBefore = cancelledReservations.size();
        int listingsBefore = cancelledListings.size();

        cancelledReservations.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        cancelledListings.entrySet().removeIf(entry -> entry.getValue().isBefore(now));

        log.debug("Cleaned up {} reservation and {} listing cancellation entries",
                reservationsBefore - cancelledReservations.size(),
                listingsBefore - cancelledListings.size());
    }

    private void scheduleGuestReminder(Integer reservationId, Integer guestId, LocalDateTime meetingDate) {
        Instant reminderTime = toInstant(meetingDate.minusHours(1));
        taskScheduler.schedule(() -> {
            if (cancelledReservations.remove(reservationId) != null) {
                return;
            }
            pushNotificationService.sendToUser(
                    guestId,
                    "Tour starts in 1 hour!",
                    "Check the details and get ready.",
                    "/reservations"
            );
        }, reminderTime);
        log.debug("Scheduled guest reminder for reservation {} at {}", reservationId, reminderTime);
    }

    private void scheduleHostReminder(Integer listingId, Integer hostId, LocalDateTime meetingDate) {
        Instant reminderTime = toInstant(meetingDate.minusHours(1));
        taskScheduler.schedule(() -> {
            if (cancelledListings.remove(listingId) != null) {
                return;
            }
            scheduledHostListings.remove(listingId);
            pushNotificationService.sendToUser(
                    hostId,
                    "Tour starts in 1 hour!",
                    "Your guests are arriving soon.",
                    "/reservations"
            );
        }, reminderTime);
        log.debug("Scheduled host reminder for listing {} at {}", listingId, reminderTime);
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
