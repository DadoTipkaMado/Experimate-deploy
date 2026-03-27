package hr.tvz.experimate.experimate.model.user;

import hr.tvz.experimate.experimate.model.shared.event.UserDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepo userRepo;
    private final ApplicationEventPublisher publisher;

    public UserService(UserRepo userRepo, ApplicationEventPublisher publisher) {
        this.userRepo = userRepo;
        this.publisher = publisher;
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

        if (updateUserDto.username() != null) {
            user.setUsername(
                    validateUsername(updateUserDto.username())
            );
        }
        if (updateUserDto.password() != null)
            user.setPassword(updateUserDto.password());

        if (updateUserDto.bio() != null)
            user.setBio(updateUserDto.bio());


        userRepo.save(user);
        log.info("User updated with id {}", id);

        return user;
    }

    @Transactional
    public void deleteUser(Integer id) {
        if (!userRepo.existsById(id)) throw new UserNotFoundException(id);

        UserDeletedEvent event = new UserDeletedEvent(id);
        publisher.publishEvent(event);

        userRepo.deleteById(id);
    }

    private String validateUsername(String username) {
        if (userRepo.existsByUsername(username)) {
            log.warn("User with username {} already exists", username);
            throw new UsernameTakenException(username);
        }
        return username;
    }

    private String validateIdNumber(String idNumber) {
        if (userRepo.existsByIdNumber(idNumber)) {
            log.warn("User with idNumber {} already exists", idNumber);
            throw new IdNumberTakenException(idNumber);
        }
        return idNumber;
    }
}
