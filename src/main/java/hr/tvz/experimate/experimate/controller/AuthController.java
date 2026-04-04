package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.security.AuthResponse;
import hr.tvz.experimate.experimate.security.AuthService;
import hr.tvz.experimate.experimate.security.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    AuthResponse authenticate(@RequestBody LoginRequest loginRequest) {
        String jwt =  authService.login(loginRequest.username(), loginRequest.password());

        return new AuthResponse(jwt);
    }

    @PostMapping("/refresh")
    AuthResponse refresh(HttpServletRequest request) {
        String jwt =

        return new AuthResponse(jwt);
    }

}
