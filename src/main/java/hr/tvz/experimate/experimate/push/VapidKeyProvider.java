package hr.tvz.experimate.experimate.push;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Centralized accessor for the application's VAPID (Voluntary Application Server Identification) configuration.
 *
 * <p>The VAPID key pair authenticates this server to Web Push services (Mozilla autopush,
 * Google FCM endpoint, Apple Push) on every push request. The public key is also exposed
 * to the frontend so the browser can include it when creating a {@code PushSubscription}.
 *
 * <p>Keys are EC P-256 (secp256r1) encoded as base64url without padding. Generate a fresh
 * pair via {@link VapidKeyGenerator}; never commit the private key to the repo.
 *
 * <p>The {@code subject} is a {@code mailto:} or {@code https:} URI per RFC 8292 — push
 * services use it to contact the operator if a server misbehaves.
 */
@Component
public class VapidKeyProvider {

    private final String publicKey;
    private final String privateKey;
    private final String subject;

    public VapidKeyProvider(@Value("${push.vapid.public-key}") String publicKey,
                            @Value("${push.vapid.private-key}") String privateKey,
                            @Value("${push.vapid.subject}") String subject) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    /** @return base64url-encoded VAPID public key, shared with the frontend for subscription creation. */
    public String getPublicKey() {
        return publicKey;
    }

    /** @return base64url-encoded VAPID private key, used server-side to sign push requests. Never expose. */
    public String getPrivateKey() {
        return privateKey;
    }

    /** @return contact URI (e.g. {@code mailto:ops@example.com}) sent in the VAPID JWT {@code sub} claim. */
    public String getSubject() {
        return subject;
    }
}
