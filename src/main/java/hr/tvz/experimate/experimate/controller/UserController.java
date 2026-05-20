package hr.tvz.experimate.experimate.controller;
import hr.tvz.experimate.experimate.model.shared.exception.InternalServerException;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import hr.tvz.experimate.experimate.model.user.CreateUserDto;
import hr.tvz.experimate.experimate.model.user.UpdateUserDto;
import hr.tvz.experimate.experimate.model.user.response.UserResponse;
import hr.tvz.experimate.experimate.model.user.UserService;
import hr.tvz.experimate.experimate.model.user.response.UserSearchResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(value = "/api/user")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                userService.createUser(dto)
        );
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable @Positive Integer id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value="/by-username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    @PatchMapping(value = "/{id}")
    public ResponseEntity<UserResponse> patchUser(@PathVariable @Positive Integer id,
                                                  @Valid @RequestBody UpdateUserDto dto,
                                                  @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(userService.updateUser(id, dto, userDetails.getId()));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable @Positive Integer id,
                                           @AuthenticationPrincipal AppUserDetails userDetails) {
        userService.deleteUser(id, userDetails.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping(value = "/{id}/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadProfilePhoto(@PathVariable @Positive Integer id,
                                                           @RequestParam("file") MultipartFile file,
                                                           @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(userService.uploadProfilePhoto(id, file, userDetails.getId()));
    }

    @GetMapping(value = "/profile-photo/{filename}")
    public ResponseEntity<Resource> getProfilePhoto(@PathVariable String filename) {
        Resource resource = userService.getProfilePhotoResourceByFilename(filename);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElseThrow(() -> new InternalServerException(
                        "Could not determine media type for file: " + resource.getFilename()));
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    @GetMapping(value = "/search")
    public ResponseEntity<UserSearchResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "ASC") Sort.Direction direction) {
        if (query == null)
            return ResponseEntity.ok(new UserSearchResponse(List.of(), 0));
        return ResponseEntity.ok(
                userService.search(query, direction)
        );
    }
}
