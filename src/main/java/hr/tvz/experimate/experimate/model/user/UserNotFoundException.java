package hr.tvz.experimate.experimate.model.user;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Integer id) {
        super("User not found with id " + id);
    }
}
