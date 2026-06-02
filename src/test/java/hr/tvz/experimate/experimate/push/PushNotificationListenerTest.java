package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.shared.event.BookingRequestCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PushNotificationListenerTest {

    @Mock private PushNotificationService pushNotificationService;

    @InjectMocks private PushNotificationListener listener;

    @Test
    void onBookingRequestCreated_sendsNotificationToHost() {
        BookingRequestCreatedEvent event = new BookingRequestCreatedEvent(42, "john");

        listener.onBookingRequestCreated(event);

        verify(pushNotificationService).sendToUser(
                42,
                "New booking request",
                "john wants to join your tour",
                "/requests"
        );
    }
}
