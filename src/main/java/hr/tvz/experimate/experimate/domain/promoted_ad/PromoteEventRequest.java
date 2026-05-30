package hr.tvz.experimate.experimate.domain.promoted_ad;

/**
 * Request body for {@code POST /api/partner-events/{eventId}/promote}.
 *
 * <p>All three fields are optional overrides — when {@code null} the corresponding value is
 * snapshotted from the promoted event:
 * <ul>
 *   <li>{@code overrideTitle} — defaults to the event's title</li>
 *   <li>{@code overrideDescription} — defaults to the event's description</li>
 *   <li>{@code overrideLinkUrl} — defaults to the event's ticket vendor URL</li>
 * </ul>
 *
 * <p>The event ID is taken from the path, not this body. Scheduling boundaries are derived
 * automatically (the promotion runs until the event ends), so they are not part of the request.
 */
public record PromoteEventRequest(
        String overrideTitle,
        String overrideDescription,
        String overrideLinkUrl
) {}
