package hr.tvz.experimate.experimate.push;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock private PushGateway pushGateway;
    @Mock private PushSubscriptionRepo pushSubscriptionRepo;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private PushNotificationService service;

    @Test
    void sendToUser_doesNothingIfUserHasNoSubscriptions() throws Exception{
        when(pushSubscriptionRepo.findByUserId(1)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.sendToUser(1, "title", "body", "/url"));

        verify(pushGateway, never()).send(any(), any(), any(), any());
    }

    @Test
    void sendToUser_deletesSubscriptionOn410Gone() throws Exception {
        PushSubscription sub = stubSubscription("https://push.example.com/sub");
        when(pushSubscriptionRepo.findByUserId(1)).thenReturn(List.of(sub));
        when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        when(pushGateway.send(any(), any(), any(), any())).thenReturn(410);

        service.sendToUser(1, "title", "body", "/url");

        verify(pushSubscriptionRepo).deleteByEndpointAndUserId("https://push.example.com/sub", 1);
    }

    @Test
    void sendToUser_doesNotPropagateExceptionWhenSendFails() throws Exception {
        PushSubscription sub = stubSubscription("https://push.example.com/sub");
        when(pushSubscriptionRepo.findByUserId(1)).thenReturn(List.of(sub));
        when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());
        when(pushGateway.send(any(), any(), any(), any())).thenThrow(new IOException("push service unreachable"));

        assertDoesNotThrow(() -> service.sendToUser(1, "title", "body", "/url"));

        verify(pushSubscriptionRepo, never()).deleteByEndpointAndUserId(any(), any());
    }

    private PushSubscription stubSubscription(String endpoint) {
        PushSubscription sub = mock(PushSubscription.class);
        when(sub.getEndpoint()).thenReturn(endpoint);
        when(sub.getP256dh()).thenReturn("p256dh");
        when(sub.getAuth()).thenReturn("auth");
        return sub;
    }
}
