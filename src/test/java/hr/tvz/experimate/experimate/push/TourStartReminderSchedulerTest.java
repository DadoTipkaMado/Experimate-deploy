package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.domain.reservation.ReservationRepo;
import hr.tvz.experimate.experimate.domain.reservation.ReservationStatus;
import hr.tvz.experimate.experimate.shared.event.ReservationCancelledEvent;
import hr.tvz.experimate.experimate.shared.event.ReservationConfirmedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourStartReminderSchedulerTest {

    @Mock private TaskScheduler taskScheduler;
    @Mock private ReservationRepo reservationRepo;
    @Mock private PushNotificationService pushNotificationService;

    @InjectMocks private TourStartReminderScheduler scheduler;

    /**
     * #3 — kad je jedina rezervacija za listing otkazana, ni guest ni host
     * ne smiju dobiti "tura počinje za sat" push.
     */
    @Test
    void onReservationCancelled_whenNoGuestsRemain_neitherGuestNorHostReceivesPush() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        LocalDateTime meetingDate = LocalDateTime.now().plusHours(3);
        scheduler.onReservationConfirmed(new ReservationConfirmedEvent(100, 10, 20, 5, meetingDate));

        when(reservationRepo.countByTourListing_IdAndStatus(5, ReservationStatus.CONFIRMED)).thenReturn(0L);
        scheduler.onReservationCancelled(new ReservationCancelledEvent(100, 5, meetingDate));

        verify(taskScheduler, times(2)).schedule(runnableCaptor.capture(), any(Instant.class));
        runnableCaptor.getAllValues().forEach(Runnable::run);

        verify(pushNotificationService, never()).sendToUser(any(), any(), any(), any());
    }

    /**
     * #3 — guest koji je otkazao ne dobiva push, ali host još uvijek dobiva jer
     * ostali gosti imaju aktivne rezervacije.
     */
    @Test
    void onReservationCancelled_whenOtherGuestsConfirmed_hostReminderIsNotCancelled() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        LocalDateTime meetingDate = LocalDateTime.now().plusHours(3);
        scheduler.onReservationConfirmed(new ReservationConfirmedEvent(100, 10, 20, 5, meetingDate));

        when(reservationRepo.countByTourListing_IdAndStatus(5, ReservationStatus.CONFIRMED)).thenReturn(1L);
        scheduler.onReservationCancelled(new ReservationCancelledEvent(100, 5, meetingDate));

        verify(taskScheduler, times(2)).schedule(runnableCaptor.capture(), any(Instant.class));
        runnableCaptor.getAllValues().forEach(Runnable::run);

        verify(pushNotificationService, never()).sendToUser(eq(10), any(), any(), any());
        verify(pushNotificationService).sendToUser(eq(20), any(), any(), any());
    }
}
