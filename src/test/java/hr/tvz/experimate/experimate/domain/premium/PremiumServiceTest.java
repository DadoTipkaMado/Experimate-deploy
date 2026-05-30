package hr.tvz.experimate.experimate.domain.premium;

import hr.tvz.experimate.experimate.domain.user.Role;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PremiumServiceTest {

    @Mock private UserRepo userRepo;
    @Mock private PaymentGateway paymentGateway;

    @InjectMocks private PremiumService premiumService;

    @Test
    void purchase_whenPaymentDeclined_doesNotGrantPremium() {
        User user = mock(User.class);
        when(user.getRole()).thenReturn(Role.USER);
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(paymentGateway.charge(any(), any(), any()))
                .thenReturn(new PaymentResult(false, "DECLINED"));

        assertThrows(PaymentFailedException.class,
                () -> premiumService.purchase(1, PremiumPackage.MONTH));

        verify(user, never()).grantPremium(any());
    }

    @Test
    void purchase_whenUserAlreadyPremium_extendsFromCurrentExpiry() {
        LocalDateTime existingExpiry = LocalDateTime.now().plusDays(5);

        User user = mock(User.class);
        when(user.getRole()).thenReturn(Role.PREMIUM_USER);
        when(user.getPremiumExpiryDate()).thenReturn(existingExpiry);
        when(userRepo.findById(1)).thenReturn(Optional.of(user));
        when(paymentGateway.charge(any(), any(), any()))
                .thenReturn(new PaymentResult(true, "TXN-123"));

        premiumService.purchase(1, PremiumPackage.MONTH);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(user).grantPremium(captor.capture());
        assertEquals(existingExpiry.plus(PremiumPackage.MONTH.getDuration()), captor.getValue());
    }
}
