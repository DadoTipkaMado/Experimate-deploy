package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints supporting the browser-side Web Push subscription flow.
 *
 * <p>All endpoints require an authenticated user ({@code /api/**} rule in
 * {@link hr.tvz.experimate.experimate.config.SecurityConfig}) because push subscriptions
 * are tied to a user account — anonymous users cannot receive targeted notifications.
 */
@RestController
@RequestMapping("/api/push")
@Validated
public class PushController {

    private final VapidKeyProvider vapidKeyProvider;
    private final PushSubscriptionService pushSubscriptionService;

    public PushController(VapidKeyProvider vapidKeyProvider,
                          PushSubscriptionService pushSubscriptionService) {
        this.vapidKeyProvider = vapidKeyProvider;
        this.pushSubscriptionService = pushSubscriptionService;
    }

    /**
     * Returns the server's VAPID public key so the browser can pass it to
     * {@code PushManager.subscribe({applicationServerKey})}.
     *
     * @return JSON body {@code { "key": "<base64url public key>" }}
     */
    @GetMapping("/vapid-public-key")
    public ResponseEntity<VapidPublicKeyResponse> getVapidPublicKey() {
        return ResponseEntity.ok(new VapidPublicKeyResponse(vapidKeyProvider.getPublicKey()));
    }

    /**
     * Registers a browser's push subscription for the authenticated user.
     * Idempotent — sending the same endpoint twice is safe.
     *
     * @param dto         the subscription details from the browser's PushSubscription object
     * @param userDetails the authenticated user
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody SubscribeRequest dto,
                                          @AuthenticationPrincipal AppUserDetails userDetails) {
        pushSubscriptionService.subscribe(userDetails.getId(), dto.endpoint(), dto.p256dh(), dto.auth());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Removes the push subscription identified by the given endpoint.
     * Called on logout so the user stops receiving notifications after signing out.
     * Uses POST (not DELETE) because DELETE + body + keepalive is unreliable in Chromium.
     *
     * <p>The delete is scoped to the authenticated user so a user cannot remove
     * another user's subscription by sending a foreign endpoint URL.
     *
     * @param dto         the endpoint identifying the subscription to remove
     * @param userDetails the authenticated user
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@Valid @RequestBody UnsubscribeRequest dto,
                                            @AuthenticationPrincipal AppUserDetails userDetails) {
        pushSubscriptionService.unsubscribe(userDetails.getId(), dto.endpoint());
        return ResponseEntity.noContent().build();
    }

    /** Wrapper around the public key string; lets the response shape grow without breaking clients. */
    public record VapidPublicKeyResponse(String key) {}

    /** Subscription details sent by the browser after calling {@code PushManager.subscribe()}. */
    public record SubscribeRequest(@NotBlank String endpoint,
                                   @NotBlank String p256dh,
                                   @NotBlank String auth) {}

    /** Endpoint identifier sent on logout to remove the subscription. */
    public record UnsubscribeRequest(@NotBlank String endpoint) {}
}
