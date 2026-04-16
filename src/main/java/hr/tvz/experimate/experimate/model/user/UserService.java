package hr.tvz.experimate.experimate.model.user;

import hr.tvz.experimate.experimate.model.shared.event.RatingRecalculatedEvent;
import hr.tvz.experimate.experimate.model.shared.event.UserDeletedEvent;
import hr.tvz.experimate.experimate.model.user.exception.IdNumberTakenException;
import hr.tvz.experimate.experimate.model.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.model.user.exception.UsernameTakenException;
import hr.tvz.experimate.experimate.model.user.response.UserResponse;
import hr.tvz.experimate.experimate.model.user.response.UserSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepo userRepo;
    private final ApplicationEventPublisher publisher;
    private final BCryptPasswordEncoder encoder;

    public UserService(UserRepo userRepo,
                       ApplicationEventPublisher publisher,
                       BCryptPasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.publisher = publisher;
        this.encoder = encoder;
    }

    public UserResponse createUser(CreateUserDto createUserDto) {
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
                encoder.encode(createUserDto.password())
        )
                .bio(createUserDto.bio())
                .build();

        userRepo.save(user);
        log.info("User created with id {}", user.getId());

        return createUserResponse(user);
    }

    public Optional<UserResponse> getUserById(Integer id) {
        return userRepo.findById(id)
                .map(user -> createUserResponse(user));
    }

    public List<UserResponse> getAllUsers() {
        return userRepo.findAll()
                .stream()
                .map(user -> createUserResponse(user))
                .toList();
    }

    public UserResponse updateUser(Integer id, UpdateUserDto updateUserDto) {
        User user = userRepo.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        if (updateUserDto.username() != null) {
            try {
                user.setUsername(
                        validateUsername(updateUserDto.username())
                );
            } catch (UsernameTakenException e) {
                if (!updateUserDto.username().equals(user.getUsername())) {
                    log.warn("Attempted to update username to another user's username.");
                    throw new IllegalArgumentException("Cannot set username to a taken username.");
                }
                log.debug("Username has stayed the same in latest update for profile.");
            }
        }
        if (updateUserDto.password() != null)
            user.setPassword(
                    encoder.encode(updateUserDto.password())
            );
        if (updateUserDto.bio() != null)
            user.setBio(updateUserDto.bio());
        if (updateUserDto.profilePhotoUrl() != null)
            user.setProfilePhotoUrl(updateUserDto.profilePhotoUrl());

        userRepo.save(user);
        log.info("User updated with id {}", id);

        return createUserResponse(user);
    }

    public UserSearchResponse search(String query, Sort.Direction direction) {
        Sort sort = Sort.by(direction, "username", "firstName", "lastName");

        List<UserResponse> result = userRepo.findByUsernameContainingOrFirstNameContainingOrLastNameContaining(
                        query, query, query, sort
                ).stream()
                .map(this::createUserResponse)
                .toList();

        return new UserSearchResponse(
                result,
                result.toArray().length
        );
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

    private UserResponse createUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getRating(),
                user.getProfilePhotoUrl()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void handleRatingRecalculatedEvent(RatingRecalculatedEvent event) {
        User user = userRepo.findById(event.userId())
                .orElseThrow(() -> {
                    log.warn("No user found for given id {}.", event.userId());
                    return new UserNotFoundException(event.userId());
                });

        user.setRating(event.ratingScore());
    }

    //TODO dodaj handling za rating evente
    //TODO dodaj metodue recalculate rating score
}
