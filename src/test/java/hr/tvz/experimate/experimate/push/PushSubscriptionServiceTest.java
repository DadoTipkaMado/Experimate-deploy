package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushSubscriptionServiceTest {

    @Mock private PushSubscriptionRepo pushSubscriptionRepo;
    @Mock private UserRepo userRepo;

    @InjectMocks private PushSubscriptionService service;

    @Test
    void subscribe_throwsIfUserNotFound() {
        when(pushSubscriptionRepo.existsByEndpoint("https://push.example.com/sub")).thenReturn(false);
        when(userRepo.findById(99)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.subscribe(99, "https://push.example.com/sub", "p256dh", "auth"));

        verify(pushSubscriptionRepo, never()).save(any());
    }

    @Test
    void subscribe_doesNotSaveIfEndpointAlreadyRegistered() {
        when(pushSubscriptionRepo.existsByEndpoint("https://push.example.com/sub")).thenReturn(true);

        service.subscribe(1, "https://push.example.com/sub", "p256dh", "auth");

        verify(userRepo, never()).findById(any());
        verify(pushSubscriptionRepo, never()).save(any());
    }

    /**
     * #6 — delete must be scoped to the calling user's ID so a user
     * cannot remove another user's subscription by guessing the endpoint.
     */
    @Test
    void unsubscribe_scopesDeleteToCallingUserIdAndEndpoint() {
        service.unsubscribe(99, "https://push.example.com/sub");

        verify(pushSubscriptionRepo).deleteByEndpointAndUserId("https://push.example.com/sub", 99);
    }
}
