package hr.tvz.experimate.experimate.model.user;

public class UsernameTakenException extends RuntimeException {
    public UsernameTakenException(String username) {
        super("Username: '%s' is already taken!".formatted(username));
    }
}
