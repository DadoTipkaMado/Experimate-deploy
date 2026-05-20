package hr.tvz.experimate.experimate.security;

import hr.tvz.experimate.experimate.model.shared.exception.RefreshTokenException;

public class MissingCookieException extends RefreshTokenException {
    public MissingCookieException(String name) {
        super("Could not find %s cookie from servlet request.".formatted(name));
    }
}
