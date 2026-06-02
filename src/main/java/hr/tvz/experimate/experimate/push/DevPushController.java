package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.security.AppUserDetails;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Development-only endpoint for manually triggering a push notification to yourself.
 *
 * <p>Annotated with {@code @Profile("local")} so the bean is created only when the
 * {@code local} Spring profile is active (the default — see {@code application.properties}).
 * In production the active profile is {@code prod}, so the bean never exists and the route
 * returns 404 — it cannot be abused to spam notifications.
 *
 * <p>Use it to confirm the full delivery path (VAPID keys, encryption, service worker
 * push handler, deep linking) works end-to-end before any real domain event is wired up:
 * subscribe in the browser, then POST to this endpoint and check the notification appears.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("local")
public class DevPushController {

    private final PushNotificationService pushNotificationService;

    public DevPushController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    /**
     * Sends a canned test notification to every device the calling user is subscribed from.
     * No-op (still returns 204) if the user has no active subscription.
     *
     * @param userDetails the authenticated user — the notification is sent to themselves
     */
    @PostMapping("/test-push")
    public ResponseEntity<Void> sendTestPush(@AuthenticationPrincipal AppUserDetails userDetails) {
        pushNotificationService.sendToUser(
                userDetails.getId(),
                "Test notification",
                "If you can read this, Web Push is working.",
                "/"
        );
        return ResponseEntity.noContent().build();
    }
}
