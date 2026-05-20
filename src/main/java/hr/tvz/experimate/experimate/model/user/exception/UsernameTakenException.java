package hr.tvz.experimate.experimate.model.user.exception;

import hr.tvz.experimate.experimate.model.shared.exception.ConflictException;

public class UsernameTakenException extends ConflictException {
    public UsernameTakenException(String username) {
        super("Username: '%s' is already taken!".formatted(username));
    }
}
