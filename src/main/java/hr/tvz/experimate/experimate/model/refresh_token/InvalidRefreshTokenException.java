package hr.tvz.experimate.experimate.model.refresh_token;

import hr.tvz.experimate.experimate.model.shared.exception.RefreshTokenException;

public class InvalidRefreshTokenException extends RefreshTokenException {
    public InvalidRefreshTokenException(Integer userId, String refreshToken) {
        super("Refresh token %s invalid for user with id %d.".formatted(refreshToken, userId));
    }

    public InvalidRefreshTokenException(String refreshToken) {
        super("Invalid refresh token: %s.".formatted(refreshToken));
    }
}
