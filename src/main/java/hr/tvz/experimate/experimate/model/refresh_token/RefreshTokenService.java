package hr.tvz.experimate.experimate.model.refresh_token;

import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.shared.response.TokenResponse;
import hr.tvz.experimate.experimate.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    @Value("${refresh-token.expiration}")
    private long expirationMs;

    private final RefreshTokenRepo refreshTokenRepo;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepo refreshTokenRepo,
                               JwtService jwtService) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.jwtService = jwtService;
    }

    public TokenResponse rotateAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepo.findByToken(refreshTokenString)
                .orElseThrow(() -> {
                    log.debug("Refresh token not found. Cannot rotate token.");
                     return new InvalidRefreshTokenException(refreshTokenString);
                });

        if(!isValid(refreshToken)) {
            log.warn("Invalid refresh token");
            throw new InvalidRefreshTokenException(refreshToken.getId(), refreshToken.getToken());
        }

        String newRefreshToken = createOrUpdateRefreshToken(refreshToken.getUser());
        String newAccessToken = jwtService.generateToken(
                refreshToken.getUser().getUsername()
        );

        log.debug("Created new refresh token for user with id {}", refreshToken.getUser().getId());
        return new TokenResponse(
                newAccessToken,
                newRefreshToken
        );
    }

    public String createOrUpdateRefreshToken(User user) {
        String token = generateToken();
        LocalDateTime expiration = LocalDateTime.now().plusSeconds(expirationMs / 1000);

        RefreshToken refreshToken = refreshTokenRepo.findByUser_Id(user.getId())
                .map(existing -> {
                    existing.updateToken(token, expiration);
                    return existing;
                })
                .orElse(new RefreshToken(token, user, expiration));

        log.info("Created refresh token for user {}", user.getUsername());
        return refreshTokenRepo.save(refreshToken).getToken();
    }

    /**
     * Deletes the refresh token with the given value from the database.
     * If the token does not exist, the operation completes silently (idempotent).
     *
     * @param token the raw refresh token string to invalidate
     */
    @Transactional
    public void invalidateToken(String token) {
        refreshTokenRepo.deleteByToken(token);
        log.info("Refresh token invalidated.");
    }

    private boolean isValid(RefreshToken refreshToken) {
        return (refreshTokenRepo.existsByTokenAndUser_Id(
                refreshToken.getToken(),
                refreshToken.getUser().getId()
        ) &&
                refreshToken.getExpirationDateTime().isAfter(LocalDateTime.now())
        );
    }

    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
