package hr.tvz.experimate.experimate.push;

import hr.tvz.experimate.experimate.domain.token.EmailVerificationToken;
import hr.tvz.experimate.experimate.domain.token.EmailVerificationTokenRepo;
import hr.tvz.experimate.experimate.domain.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationReminderSchedulerTest {

    @Mock private EmailVerificationTokenRepo emailVerificationTokenRepo;
    @Mock private PushNotificationService pushNotificationService;

    @InjectMocks private EmailVerificationReminderScheduler scheduler;

    @Test
    void sendVerificationReminders_whenUsersUnverified_sendsReminderToEachUser() {
        EmailVerificationToken firstToken = tokenForUser(10);
        EmailVerificationToken secondToken = tokenForUser(20);
        when(emailVerificationTokenRepo.findAllByExpirationDateTimeAfter(any()))
                .thenReturn(List.of(firstToken, secondToken));

        scheduler.sendVerificationReminders();

        verify(pushNotificationService).sendToUser(10,
                "Verify your email",
                "Confirm your email address to unlock all ExperiMate features.",
                "/verify-email");
        verify(pushNotificationService).sendToUser(20,
                "Verify your email",
                "Confirm your email address to unlock all ExperiMate features.",
                "/verify-email");
    }

    @Test
    void sendVerificationReminders_whenNoUnverifiedUsers_sendsNoPush() {
        when(emailVerificationTokenRepo.findAllByExpirationDateTimeAfter(any()))
                .thenReturn(List.of());

        scheduler.sendVerificationReminders();

        verify(pushNotificationService, never()).sendToUser(any(), any(), any(), any());
    }

    private EmailVerificationToken tokenForUser(Integer userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        return new EmailVerificationToken("hash-" + userId, user, LocalDateTime.now().plusHours(1));
    }
}
