package hr.tvz.experimate.experimate.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserDetails implements UserDetails {
    private Integer id;
    private String username;
    private String password;

    public AppUserDetails(Integer id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    //For differentiating perimum users from regular users
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
    
    public Integer getId(){
        return id;
    }
}
