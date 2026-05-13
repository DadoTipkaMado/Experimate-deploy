package hr.tvz.experimate.experimate.security;

import hr.tvz.experimate.experimate.domain.refresh_token.RefreshTokenService;
import hr.tvz.experimate.experimate.shared.response.TokenResponse;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepo userRepo;

    public AuthService(AuthenticationManager authManager,
                       JwtService jwtService, RefreshTokenService refreshTokenService, UserRepo userRepo) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepo = userRepo;
    }

    public TokenResponse login(String username, String password) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        log.info("User {} logged in successfully.", user.getUsername());
        log.debug("Log in successful, generating new JWT");
        return new TokenResponse(
                jwtService.generateToken(auth.getName()),
                refreshTokenService.createOrUpdateRefreshToken(user)
        );
    }

    public TokenResponse refreshAccessToken(String refreshToken){

        return refreshTokenService.rotateAccessToken(refreshToken);
    }

    /**
     * Invalidates the given refresh token, effectively logging the user out.
     * If the token does not exist in the database, the operation completes silently.
     *
     * @param refreshToken the raw refresh token string extracted from the client cookie
     */
    public void logout(String refreshToken) {
        refreshTokenService.invalidateToken(refreshToken);
        log.info("User logged out.");
    }

}
