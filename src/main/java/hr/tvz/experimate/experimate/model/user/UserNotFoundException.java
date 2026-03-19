package hr.tvz.experimate.experimate.model.user;

import hr.tvz.experimate.experimate.model.shared.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(Integer id) {
        super("User not found with id " + id);
    }
}
