package hr.tvz.experimate.experimate.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Replaces the async task executor with a synchronous one for integration tests.
 *
 * <p>Without this, {@code @Async} methods run in background threads that can outlive a
 * test. When the next test resets the database, a late-firing thread may find a newly
 * created user with the same ID and overwrite that test's verification token —
 * producing spurious {@code InvalidTokenException} failures in {@code AccountVerificationServiceIT}.
 *
 * <p>With {@link SyncTaskExecutor} every {@code @Async} call runs synchronously in the
 * calling thread, eliminating the race entirely and making test behaviour predictable.
 */
@TestConfiguration
public class TestAsyncConfig {

    @Bean
    @Primary
    public Executor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
