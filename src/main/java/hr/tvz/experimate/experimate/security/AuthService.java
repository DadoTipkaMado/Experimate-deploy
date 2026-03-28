package hr.tvz.experimate.experimate.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authManager,
                       JwtService jwtService) {
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    public String login(String username, String password) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        return jwtService.generateToken(auth.getName());
    }

}
