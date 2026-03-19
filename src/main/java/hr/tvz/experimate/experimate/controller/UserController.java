package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.user.CreateUserDto;
import hr.tvz.experimate.experimate.model.user.UpdateUserDto;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/user")
class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody CreateUserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                userService.createUser(dto)
        );
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    @PatchMapping(value = "/{id}")
    public ResponseEntity<User> patchUser(@PathVariable Integer id,
                                          @RequestBody UpdateUserDto dto){
        return ResponseEntity.ok(
                userService.updateUser(id, dto)
        );
    }

    @DeleteMapping(value="/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
