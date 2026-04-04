package hr.tvz.experimate.experimate.model.refresh_token;

import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.security.JwtService;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

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

    public RefreshToken createOrUpdateToken(User user) {
        String token = generateToken();
        LocalDateTime expiration = LocalDateTime.now().plusSeconds(expirationMs / 1000);

        RefreshToken refreshToken = refreshTokenRepo.findByUser_Id(user.getId())
                .map(existing -> {
                    existing.updateToken(token, expiration);
                    return existing;
                })
                .orElse(new RefreshToken(token, user, expiration));

        log.info("Created refresh token for user {}", user.getUsername());
        return refreshTokenRepo.save(refreshToken);
    }



    private String generateToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64];
        random.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
