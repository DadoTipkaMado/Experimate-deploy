package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages Web Push subscriptions for authenticated users.
 *
 * <p>Subscribe is idempotent — calling it twice with the same endpoint is safe.
 * Unsubscribe is a no-op if the endpoint is not found, which handles the case
 * where logout is called after the subscription was already cleaned up (e.g. 410 Gone).
 */
@Service
public class PushSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(PushSubscriptionService.class);

    private final PushSubscriptionRepo pushSubscriptionRepo;
    private final UserRepo userRepo;

    public PushSubscriptionService(PushSubscriptionRepo pushSubscriptionRepo, UserRepo userRepo) {
        this.pushSubscriptionRepo = pushSubscriptionRepo;
        this.userRepo = userRepo;
    }

    /**
     * Saves a new push subscription for the given user, or does nothing if the endpoint
     * is already registered (prevents duplicates when the frontend re-subscribes).
     *
     * @param userId   the authenticated user's ID
     * @param endpoint the push service URL from the browser's PushSubscription object
     * @param p256dh   the browser's EC P-256 public key used to encrypt the payload
     * @param auth     the browser's random auth secret used in key derivation
     */
    @Transactional
    public void subscribe(Integer userId, String endpoint, String p256dh, String auth) {
        if (pushSubscriptionRepo.existsByEndpoint(endpoint)) {
            return;
        }
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        pushSubscriptionRepo.save(new PushSubscription(user, endpoint, p256dh, auth));
        log.info("Registered push subscription for user {}, endpoint: {}", userId, endpoint);
    }

    /**
     * Removes the push subscription identified by the given endpoint, scoped to the calling user.
     * Called on logout (decision A). No-op if the endpoint is not found or belongs to a different user.
     *
     * <p>Scoping the delete to {@code userId} prevents a user from removing another user's
     * subscription by guessing or replaying a foreign endpoint URL.
     *
     * @param userId   the authenticated user's ID — delete is restricted to their subscriptions
     * @param endpoint the push service URL identifying the subscription to remove
     */
    @Transactional
    public void unsubscribe(Integer userId, String endpoint) {
        pushSubscriptionRepo.deleteByEndpointAndUserId(endpoint, userId);
        log.info("Removed push subscription for user {}, endpoint: {}", userId, endpoint);
    }
}
