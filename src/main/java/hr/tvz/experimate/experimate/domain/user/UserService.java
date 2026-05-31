package hr.tvz.experimate.experimate.domain.user;

import hr.tvz.experimate.experimate.domain.onboarding.QuizResult;
import hr.tvz.experimate.experimate.domain.onboarding.QuizResultRepo;
import hr.tvz.experimate.experimate.shared.FileStorageService;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.shared.event.GoogleUserCreationEvent;
import hr.tvz.experimate.experimate.shared.event.RatingRecalculatedEvent;
import hr.tvz.experimate.experimate.shared.event.UserDeletedEvent;
import hr.tvz.experimate.experimate.shared.event.UserRegisteredEvent;
import hr.tvz.experimate.experimate.domain.user.dto.CreateUserDto;
import hr.tvz.experimate.experimate.domain.user.dto.UpdateUserDto;
import hr.tvz.experimate.experimate.domain.user.exception.EmailTakenException;
import hr.tvz.experimate.experimate.domain.user.exception.IdNumberTakenException;
import hr.tvz.experimate.experimate.domain.user.exception.UserNotFoundException;
import hr.tvz.experimate.experimate.domain.user.exception.UsernameTakenException;
import hr.tvz.experimate.experimate.domain.user.response.UserResponse;
import hr.tvz.experimate.experimate.domain.user.response.UserSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Value("${app.upload.profile-photos-dir}")
    private String profilePhotosDir;

    private final UserRepo userRepo;
    private final QuizResultRepo quizResultRepo;
    private final ApplicationEventPublisher publisher;
    private final BCryptPasswordEncoder encoder;
    private final FileStorageService fileStorageService;

    public UserService(UserRepo userRepo,
                       QuizResultRepo quizResultRepo,
                       ApplicationEventPublisher publisher,
                       BCryptPasswordEncoder encoder,
                       FileStorageService fileStorageService) {
        this.userRepo = userRepo;
        this.quizResultRepo = quizResultRepo;
        this.publisher = publisher;
        this.encoder = encoder;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public UserResponse createUser(CreateUserDto createUserDto) {
        User user = new User.UserBuilder(
                createUserDto.firstName(),
                createUserDto.lastName(),
                createUserDto.dateOfBirth(),
                validateIdNumber(createUserDto.idNumber()),
                validateEmail(createUserDto.email()),
                validateUsername(createUserDto.username()),
                encoder.encode(createUserDto.password())
        )
                .bio(createUserDto.bio())
                .build();

        userRepo.save(user);
        quizResultRepo.save(new QuizResult(user));
        publisher.publishEvent(new UserRegisteredEvent(user.getId(), user.getEmail(), user.getFirstName()));
        log.info("User created with id {}", user.getId());

        return createUserResponse(user);
    }

    public Optional<UserResponse> getUserById(Integer id) {
        return userRepo.findById(id)
                .map(user -> createUserResponse(user));
    }

    public Optional<UserResponse> getUserByUsername(String username) {
        return userRepo.findByUsername(username)
                .map(this::createUserResponse);
    }

    public List<UserResponse> getAllUsers() {
        return userRepo.findAll()
                .stream()
                .map(user -> createUserResponse(user))
                .toList();
    }

    public UserResponse updateUser(Integer id, UpdateUserDto updateUserDto, Integer callerId) {
        if (!callerId.equals(id))
            throw new ForbiddenActionException("You can only edit your own profile.");
        User user = findEntityById(id);

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

        userRepo.save(user);
        log.info("User updated with id {}", id);

        return createUserResponse(user);
    }

    public UserSearchResponse search(String query, Sort.Direction direction) {
        Sort sort = Sort.by(direction, "username", "firstName", "lastName");

        List<UserResponse> result = userRepo.search(query, sort).stream()
                .map(this::createUserResponse)
                .toList();

        return new UserSearchResponse(
                result,
                result.toArray().length
        );
    }

    /**
     * Stores a new profile photo for the given user and deletes the previous one if present.
     *
     * @param id   the user's ID
     * @param file the uploaded image file
     *
     * @return updated {@link UserResponse}
     *
     * @throws UserNotFoundException    if no user exists with the given ID
     * @throws IllegalArgumentException if the file is empty or has a disallowed content type
     */
    public UserResponse uploadProfilePhoto(Integer id, MultipartFile file, Integer callerId) {
        if (!callerId.equals(id))
            throw new ForbiddenActionException("You can only upload a photo to your own profile.");
        User user = findEntityById(id);
        String oldFilename = user.getProfilePhotoFilename();
        String newFilename = fileStorageService.store(file, profilePhotosDir);
        user.setProfilePhotoFilename(newFilename);
        if (oldFilename != null) fileStorageService.delete(oldFilename, profilePhotosDir);
        userRepo.save(user);
        log.info("Profile photo updated for user {}", id);
        return createUserResponse(user);
    }

    /**
     * Removes the given user's profile photo, deleting the stored file and clearing
     * the reference on the user. No-op on the file system if the user has no photo.
     *
     * @param id       the user's ID
     * @param callerId the ID of the authenticated caller, used to enforce that users
     *                 can only modify their own profile
     *
     * @throws ForbiddenActionException if {@code callerId} does not match {@code id}
     * @throws UserNotFoundException    if no user exists with the given ID
     */
    public void deleteProfilePhoto(Integer id, Integer callerId) {
        if (!callerId.equals(id))
            throw new ForbiddenActionException("You can only remove the photo from your own profile.");
        User user = findEntityById(id);
        String filename = user.getProfilePhotoFilename();
        if (filename == null) return;
        user.setProfilePhotoFilename(null);
        fileStorageService.delete(filename, profilePhotosDir);
        userRepo.save(user);
        log.info("Profile photo removed for user {}", id);
    }

    /**
     * Fetches a {@link User} entity by ID, shared internally to avoid duplicating
     * the not-found handling across service methods.
     *
     * @param id the user's ID
     *
     * @return the {@link User} entity
     *
     * @throws UserNotFoundException if no user exists with the given ID
     */
    private User findEntityById(Integer id) {
        return userRepo.findById(id).orElseThrow(() -> {
            log.warn("No user found for id {}", id);
            return new UserNotFoundException(id);
        });
    }

    public Resource getProfilePhotoResourceByFilename(String filename) {
        return fileStorageService.load(filename, profilePhotosDir);
    }

    /**
     * Handles {@link GoogleUserCreationEvent} published by {@code GoogleAuthService} during
     * Google OAuth2 registration. Creates the user account with {@code googleSub} set and
     * {@code emailVerified = true} since the email is Google-verified.
     *
     * <p>Runs synchronously in the same thread and transaction as the publisher, so the
     * created user is immediately visible to subsequent repo lookups in that transaction.
     */
    @EventListener
    @Transactional
    public void handleGoogleUserCreation(GoogleUserCreationEvent event) {
        User user = new User.UserBuilder(
                event.firstName(),
                event.lastName(),
                event.dateOfBirth(),
                validateIdNumber(event.idNumber()),
                validateEmail(event.email()),
                validateUsername(event.username()),
                encoder.encode(event.password())
        ).build();

        user.setGoogleSub(event.googleSub());
        user.setEmailVerified(true);

        userRepo.save(user);
        quizResultRepo.save(new QuizResult(user));
        log.info("Google user created with id {}", user.getId());
    }

    @Transactional
    public void deleteUser(Integer id, Integer callerId) {
        if (!callerId.equals(id))
            throw new ForbiddenActionException("You can only delete your own account.");
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

    private String validateEmail(String email) {
        String normalized = email.toLowerCase();
        if (userRepo.existsByEmail(normalized)) {
            log.warn("User with email {} already exists", normalized);
            throw new EmailTakenException(email);
        }
        return normalized;
    }

    private String validateIdNumber(String idNumber) {
        if (userRepo.existsByIdNumber(idNumber)) {
            log.warn("User with idNumber {} already exists", idNumber);
            throw new IdNumberTakenException(idNumber);
        }
        return idNumber;
    }

    private UserResponse createUserResponse(User user) {
        String profilePhotoUrl = user.getProfilePhotoFilename() != null
                ? "/api/user/profile-photo/" + user.getProfilePhotoFilename()
                : null;
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getRating(),
                profilePhotoUrl,
                user.getPersonalitySummary()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    void handleRatingRecalculatedEvent(RatingRecalculatedEvent event) {
        User user = findEntityById(event.userId());
        user.setRating(event.ratingScore());
    }
}
