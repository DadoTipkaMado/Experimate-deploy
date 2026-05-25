package hr.tvz.experimate.experimate.security;

import hr.tvz.experimate.experimate.domain.user.Role;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security principal built from a {@link hr.tvz.experimate.experimate.domain.user.User}.
 * Constructed per-request in {@link AppUserDetailsService} so the role is always fresh from the DB.
 */
public class AppUserDetails implements UserDetails {

    private final Integer id;
    private final String username;
    private final String password;
    private final Role role;

    public AppUserDetails(Integer id, String username, String password, Role role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     * Returns a single-element list containing the user's role as a Spring Security
     * authority in {@code ROLE_<NAME>} format (e.g., {@code ROLE_PARTNER}).
     * Used by {@code @PreAuthorize("hasRole('PARTNER')")} checks in controllers.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public Integer getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }
}
