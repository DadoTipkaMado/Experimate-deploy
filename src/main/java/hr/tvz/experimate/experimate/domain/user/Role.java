package hr.tvz.experimate.experimate.domain.user;

/**
 * Authorization role assigned to every {@link User}.
 *
 * <p>The value is stored as a string in the {@code app_user.role} column and exposed
 * to Spring Security as a {@code ROLE_<NAME>} granted authority — see
 * {@link hr.tvz.experimate.experimate.security.AppUserDetails#getAuthorities()}.
 *
 * <p>{@link #USER} is the default for newly registered accounts. {@link #PARTNER}
 * is granted self-serve via {@code POST /api/partner/apply}. {@link #ADMIN} is
 * reserved for future use and is not currently issued by any flow.
 */
public enum Role {
    USER,
    PARTNER,
    ADMIN
}
