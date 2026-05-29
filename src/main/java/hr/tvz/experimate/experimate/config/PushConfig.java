package hr.tvz.experimate.experimate.config;

import hr.tvz.experimate.experimate.push.PushGateway;
import hr.tvz.experimate.experimate.push.VapidKeyProvider;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.security.GeneralSecurityException;
import java.security.Security;

/**
 * Configures the Web Push infrastructure beans.
 *
 * <p>{@link PushService} validates the VAPID key pair on construction — a misconfigured
 * key causes a clear startup failure rather than a silent runtime error on the first send.
 *
 * <p>{@link PushGateway} is the adapter that bridges our domain code to the library.
 * All business logic depends on {@link PushGateway}; only this config class knows about
 * {@link PushService} directly.
 */
@Configuration
public class PushConfig {

    /**
     * Overrides Spring Boot's single-threaded default with a 4-thread pool.
     * Scheduler threads are freed immediately when push sends are async, so 4 is enough
     * to prevent {@code @Scheduled} tasks from blocking each other.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("push-scheduler-");
        return scheduler;
    }

    /**
     * Dedicated executor for async push HTTP calls (see {@link hr.tvz.experimate.experimate.push.PushNotificationService#sendToUser}).
     * Keeping it separate from the scheduler pool means a burst of push sends cannot starve
     * {@code @Scheduled} tasks and vice versa.
     *
     * <p>Core 10 / max 50 / queue 500 handles up to 550 concurrent sends before rejecting —
     * far beyond realistic load for this application at its current scale.
     */
    @Bean
    public TaskExecutor pushNotificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("push-send-");
        return executor;
    }

    /**
     * Builds the low-level {@link PushService} from the nl.martijndwars library.
     * VAPID keys must be valid base64url-encoded EC P-256 values; see
     * {@link hr.tvz.experimate.experimate.push.VapidKeyGenerator} for key generation.
     *
     * @throws GeneralSecurityException if either VAPID key is malformed
     */
    @Bean
    public PushService pushService(VapidKeyProvider vapidKeyProvider) throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        return new PushService(
                vapidKeyProvider.getPublicKey(),
                vapidKeyProvider.getPrivateKey(),
                vapidKeyProvider.getSubject()
        );
    }

    /**
     * Wraps {@link PushService} behind the {@link PushGateway} interface.
     * Returns the HTTP status code so callers can handle 410 Gone without
     * importing Apache HttpClient types.
     */
    @Bean
    public PushGateway pushGateway(PushService pushService) {
        return (endpoint, p256dh, auth, payload) -> {
            var notification = new Notification(endpoint, p256dh, auth, payload);
            var response = pushService.send(notification);
            return response.getStatusLine().getStatusCode();
        };
    }
}
