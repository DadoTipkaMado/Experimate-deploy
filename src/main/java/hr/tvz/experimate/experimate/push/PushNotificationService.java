package hr.tvz.experimate.experimate.push;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core service for sending Web Push notifications to users.
 *
 * <p>Each call to {@link #sendToUser} delivers the notification to every active
 * browser/device the user has subscribed from. Failed deliveries are logged but
 * never propagated — a push failure must not break the originating business operation
 * (e.g. creating a reservation should succeed even if the push cannot be sent).
 *
 * <p>Subscriptions that return HTTP 410 Gone are automatically deleted: this signals
 * that the browser revoked permission or the subscription expired, and keeping the
 * record would cause every future send attempt to fail silently.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private static final int HTTP_GONE = 410;

    private final PushGateway pushGateway;
    private final PushSubscriptionRepo pushSubscriptionRepo;
    private final ObjectMapper objectMapper;

    public PushNotificationService(PushGateway pushGateway,
                                   PushSubscriptionRepo pushSubscriptionRepo,
                                   ObjectMapper objectMapper) {
        this.pushGateway = pushGateway;
        this.pushSubscriptionRepo = pushSubscriptionRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a push notification to all active devices of the given user.
     * Runs on the {@code pushNotificationExecutor} thread pool so the calling thread
     * (scheduler or event listener) is freed immediately — the HTTP round-trip to the
     * push service does not block any other scheduled work.
     *
     * <p>The {@code url} field is used by the service worker for deep linking —
     * when the user taps the notification, the browser navigates to that URL.
     *
     * @param userId the target user's ID
     * @param title  the notification title shown in the OS notification tray
     * @param body   the notification body text
     * @param url    the in-app URL to open when the notification is tapped
     */
    @Async("pushNotificationExecutor")
    public void sendToUser(Integer userId, String title, String body, String url) {
        List<PushSubscription> subscriptions = pushSubscriptionRepo.findByUserId(userId);
        for (PushSubscription subscription : subscriptions) {
            sendToSubscription(subscription, userId, title, body, url);
        }
    }

    private void sendToSubscription(PushSubscription subscription, Integer userId, String title, String body, String url) {
        try {
            byte[] payload = buildPayload(title, body, url);
            int statusCode = pushGateway.send(
                    subscription.getEndpoint(),
                    subscription.getP256dh(),
                    subscription.getAuth(),
                    payload
            );
            if (statusCode == HTTP_GONE) {
                log.info("Push subscription expired (410 Gone), removing endpoint: {}", subscription.getEndpoint());
                pushSubscriptionRepo.deleteByEndpointAndUserId(subscription.getEndpoint(), userId);
            }
        } catch (Exception e) {
            log.warn("Failed to send push notification to endpoint {}: {}",
                    subscription.getEndpoint(), e.getMessage());
        }
    }

    /**
     * Serializes the notification fields into the JSON payload the service worker expects.
     * Shape: {@code {"title":"...","body":"...","url":"..."}}
     */
    private byte[] buildPayload(String title, String body, String url) throws JacksonException {
        return objectMapper.writeValueAsBytes(new PushPayload(title, body, url));
    }

    private record PushPayload(String title, String body, String url) {}
}
