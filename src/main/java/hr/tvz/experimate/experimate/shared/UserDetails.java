package hr.tvz.experimate.experimate.shared;

/**
 * Lightweight public projection of a {@link hr.tvz.experimate.experimate.domain.user.User},
 * embedded inside API responses wherever a host or participant is referenced.
 *
 * <p>{@code role} is consumed by the map frontend to decide whether to render a
 * partner-style pin (large blue marker with logo) for a listing — it checks
 * {@code host.role === 'PARTNER'}. {@code profilePhotoUrl} supplies the logo image
 * for that marker; {@code null} if the user has no profile photo.
 */
public record UserDetails(
        String firstName,
        String lastName,
        String username,
        String role,
        String profilePhotoUrl
) {
}
