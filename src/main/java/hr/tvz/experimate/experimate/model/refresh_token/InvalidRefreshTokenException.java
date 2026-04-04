package hr.tvz.experimate.experimate.model.refresh_token;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(Integer userId, String refreshToken) {
        super("Refresh token %s invalid for user with id %d.".formatted(refreshToken, userId));
    }
}
