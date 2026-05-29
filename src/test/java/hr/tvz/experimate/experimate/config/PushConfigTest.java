package hr.tvz.experimate.experimate.config;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PushConfigTest {

    /**
     * #5 — scheduler must have more than 1 thread so @Scheduled tasks
     * (expireReservations, cleanupCancellations, etc.) do not block each other.
     */
    @Test
    void taskScheduler_isNotSingleThreaded() {
        PushConfig config = new PushConfig();
        ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) config.taskScheduler();
        scheduler.initialize();

        ScheduledThreadPoolExecutor underlying = (ScheduledThreadPoolExecutor) scheduler.getScheduledExecutor();
        assertTrue(underlying.getCorePoolSize() > 1);
        scheduler.shutdown();
    }

    /**
     * #5 — async executor for push HTTP calls must have a large enough pool
     * so bursts of simultaneous reminders do not block scheduler threads.
     */
    @Test
    void pushNotificationExecutor_isNotSingleThreaded() {
        PushConfig config = new PushConfig();
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.pushNotificationExecutor();
        executor.initialize();

        assertTrue(executor.getThreadPoolExecutor().getCorePoolSize() > 1);

        executor.shutdown();
    }
}
