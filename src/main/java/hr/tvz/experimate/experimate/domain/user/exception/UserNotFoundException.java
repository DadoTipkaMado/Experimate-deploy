package hr.tvz.experimate.experimate.domain.user.exception;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(Integer id) {
        super("User not found with id " + id);
    }

    public UserNotFoundException(String username) {
        super("User not found with username " + username);
    }
}
