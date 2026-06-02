package hr.tvz.experimate.experimate.push;

/**
 * Port (adapter interface) over the Web Push send operation.
 *
 * <p>Follows the same pattern as {@code JavaMailSender} — the external infrastructure
 * concern (HTTP to a push service) sits behind an interface so that business logic in
 * {@link PushNotificationService} depends on an abstraction, not a library class.
 *
 * <p>The real implementation is a lambda wired in
 * {@link hr.tvz.experimate.experimate.config.PushConfig}. Tests inject a mock.
 */
@FunctionalInterface
public interface PushGateway {

    /**
     * Sends an encrypted payload to the given push subscription endpoint.
     *
     * @param endpoint the push service URL from the browser's PushSubscription object
     * @param p256dh   the browser's EC P-256 public key used to encrypt the payload
     * @param auth     the browser's random auth secret used in key derivation
     * @param payload  the JSON-encoded notification payload (title, body, url)
     * @return HTTP status code returned by the push service
     *         (201 = delivered, 410 = subscription expired/revoked)
     * @throws Exception if the push service is unreachable or encryption fails
     */
    int send(String endpoint, String p256dh, String auth, byte[] payload) throws Exception;
}
