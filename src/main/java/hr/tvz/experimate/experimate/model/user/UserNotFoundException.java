package hr.tvz.experimate.experimate.model.user;

import hr.tvz.experimate.experimate.model.shared.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(Integer id) {
        super("User not found with id " + id);
    }

    public UserNotFoundException(String username) {
        super("User not found with username " + username);
    }
}
