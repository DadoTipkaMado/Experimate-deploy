package hr.tvz.experimate.experimate.security;

public class MissingCookieException extends RuntimeException {
    public MissingCookieException(String name) {
        super("Could not find %s cookie from servlet request.".formatted(name));
    }
}
