package hr.tvz.experimate.experimate.domain.premium;

import hr.tvz.experimate.experimate.domain.user.Role;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PremiumExpirySchedulerTest {

    @Mock private UserRepo userRepo;

    @InjectMocks private PremiumExpiryScheduler scheduler;

    @Test
    void revokeExpiredPremiumAccounts_whenExpiredAccountsExist_revokesOnlyExpired() {
        User expiredFirst = mock(User.class);
        User expiredSecond = mock(User.class);
        User stillActive = mock(User.class);

        // repo returns only the two expired users — stillActive is filtered out by premiumUntil > now
        when(userRepo.findByRoleAndPremiumUntilBefore(eq(Role.PREMIUM_USER), any()))
                .thenReturn(List.of(expiredFirst, expiredSecond));

        scheduler.revokeExpiredPremiumAccounts();

        verify(expiredFirst).revokePremium();
        verify(expiredSecond).revokePremium();
        verify(stillActive, never()).revokePremium();
    }
}
