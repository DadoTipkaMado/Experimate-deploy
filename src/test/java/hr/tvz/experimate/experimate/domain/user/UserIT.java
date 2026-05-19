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
                "MarKoPetrovic.HorVAT@gmaIL.com",
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

    @Test
    void createUser_validEmail_persistsInLowerCase() {
        CreateUserDto dto = new CreateUserDto(
                "David",
                "Topić",
                LocalDate.of(2005, 4, 28),
                "123123123123",
                "MarKoPetrovic.HorVAT@gmaIL.com",
                "dtopic",
                "12312312111212",
                null
        );

        restTemplate.postForEntity(
                "/api/user",
                dto,
                UserResponse.class
        );
        Optional<User> found = userRepo.findByEmail("markopetrovic.horvat@gmail.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("markopetrovic.horvat@gmail.com");
    }

    @Test
    void createUser_existingMail_returns409() {
        CreateUserDto dto1 = new CreateUserDto(
                "David",
                "Topić",
                LocalDate.of(2005, 4, 28),
                "99999999999999999999",
                "MarKoPetrovic.HorVAT@gmaIL.com",
                "utest2",
                "12312312111212",
                null
        );

        CreateUserDto dto2 = new CreateUserDto(
                "David",
                "Topić",
                LocalDate.of(2005, 4, 28),
                "4444444444444444444444",
                "MarKoPetrovic.HorVAT@gmaIL.com",
                "utest1",
                "12312312111212",
                null
        );

        restTemplate.postForEntity(
                "/api/user",
                dto1,
                UserResponse.class
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/user",
                dto2,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
