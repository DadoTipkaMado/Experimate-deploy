package hr.tvz.experimate.experimate.domain.user;

import hr.tvz.experimate.experimate.domain.onboarding.QuizResultRepo;
import hr.tvz.experimate.experimate.domain.user.dto.CreateUserDto;
import hr.tvz.experimate.experimate.domain.user.exception.EmailTakenException;
import hr.tvz.experimate.experimate.shared.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private QuizResultRepo quizResultRepo;
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private BCryptPasswordEncoder encoder;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserService service;

    @Test
    void createUser_validEmailIsNormalized() {
        CreateUserDto dto = new CreateUserDto(
                "Marko",
                "Petrović",
                LocalDate.of(2006, 12, 6),
                "12312312312312312",
                "mArKo.peTROvic@gmAIl.COM",
                "mpetrovic",
                "12312312",
                null
        );

        when(userRepo.existsByEmail("marko.petrovic@gmail.com")).thenReturn(false);
        when(userRepo.existsByIdNumber("12312312312312312")).thenReturn(false);
        when(userRepo.existsByUsername("mpetrovic")).thenReturn(false);
        when(encoder.encode(any())).thenReturn("hashed_pass");

        service.createUser(dto);

        ArgumentCaptor<User> captor =  ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());

        assertThat(captor.getValue().getEmail()).isEqualTo("marko.petrovic@gmail.com");
    }

    @Test
    void createUser_existingEmailThrowsEmailTakenException(){
        CreateUserDto dto = new CreateUserDto(
                "Marko",
                "Petrović",
                LocalDate.of(2006, 12, 6),
                "12312312312312312",
                "mArKo.peTROvic@gmAIl.COM",
                "mpetrovic",
                "12312312",
                null
        );

        when(userRepo.existsByIdNumber(any())).thenReturn(false);
        when(userRepo.existsByEmail(any())).thenReturn(true);

        assertThatThrownBy(()->service.createUser(dto))
                .isInstanceOf(EmailTakenException.class);
    }
}
