package hr.tvz.experimate.experimate.push;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link PushSubscription} persistence.
 *
 * <p>Push subscriptions are looked up by user ID when sending notifications
 * (one user may have multiple active devices), and deleted by endpoint on logout
 * or when the push service returns HTTP 410 Gone (subscription expired/revoked).
 */
public interface PushSubscriptionRepo extends JpaRepository<PushSubscription, Integer> {

    /** Returns all active subscriptions for the given user — one per browser/device. */
    List<PushSubscription> findByUserId(Integer userId);

    /** Deletes the subscription only if it belongs to the given user — prevents cross-user removal. */
    void deleteByEndpointAndUserId(String endpoint, Integer userId);

    /** Returns whether a subscription with the given endpoint already exists. */
    boolean existsByEndpoint(String endpoint);
}
