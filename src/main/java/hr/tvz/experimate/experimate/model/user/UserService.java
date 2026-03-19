package hr.tvz.experimate.experimate.model.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public User createUser(CreateUserDto createUserDto) {
        User user = new User.UserBuilder(
                createUserDto.firstName(),
                createUserDto.lastName(),
                createUserDto.dateOfBirth(),
                validateIdNumber(
                        createUserDto.idNumber()
                ),
                validateUsername(
                        createUserDto.username()
                ),
                createUserDto.password()
        )
                .bio(createUserDto.bio())
                .build();

        userRepo.save(user);
        log.info("User created with id {}", user.getId());

        return user;
    }

    public Optional<User> getUserById(Integer id) {
        return userRepo.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User updateUser(Integer id, UpdateUserDto updateUserDto) {
        User user = userRepo.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        if (updateUserDto.username() != null) user.setUsername(
                validateUsername(updateUserDto.username())
        );
        if (updateUserDto.password() != null) user.setPassword(updateUserDto.password());
        if (updateUserDto.bio() != null) user.setBio(updateUserDto.bio());
        userRepo.save(user);

        log.info("User updated with id {}", id);

        return user;
    }

    public void deleteUser(Integer id) {
        if (userRepo.existsById(id)) {
            userRepo.deleteById(id);
            log.info("User deleted with id {}", id);
        } else {
            log.warn("User not found with id {}", id);
        }
    }

    private String validateUsername(String username){
        if(userRepo.existsByUsername(username)) {
            log.warn("User with username {} already exists", username);
            throw new UsernameTakenException(username);
        }
        return username;
    }

    private String validateIdNumber(String idNumber){
        if(userRepo.existsByIdNumber(idNumber)) {
            log.warn("User with idNumber {} already exists", idNumber);
            throw new IdNumberTakenException(idNumber);
        }
        return idNumber;
    }
}
