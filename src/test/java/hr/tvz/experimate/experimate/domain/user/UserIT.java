package hr.tvz.experimate.experimate.domain.user;

import hr.tvz.experimate.experimate.AbstractIntegrationTest;
import hr.tvz.experimate.experimate.domain.user.dto.CreateUserDto;
import hr.tvz.experimate.experimate.domain.user.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepo userRepo;
    @Autowired
    BCryptPasswordEncoder encoder;

    @Test
    void createUser_persistsUser_returnsUserResponse_passwordIsHashed() {
        CreateUserDto dto = new CreateUserDto(
                "David",
                "Topić",
                LocalDate.of(2005, 4, 28),
                "123123123123",
                "dtopic",
                "12312312111212",
                null
        );
        String username = dto.username();
        String password = dto.password();
        ResponseEntity<UserResponse> postResponse = restTemplate.postForEntity(
                "/api/user",
                dto,
                UserResponse.class
        );

        Optional<User> user = userRepo.findByUsername(username);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(user).isPresent();
        assertThat(encoder.matches(password, user.get().getPassword())).isTrue();
    }
}
