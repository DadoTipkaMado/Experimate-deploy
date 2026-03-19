package hr.tvz.experimate.experimate.model.user;

import hr.tvz.experimate.experimate.model.shared.ConflictException;

public class UsernameTakenException extends ConflictException {
    public UsernameTakenException(String username) {
        super("Username: '%s' is already taken!".formatted(username));
    }
}
