package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.shared.payment.PaymentGateway;
import hr.tvz.experimate.experimate.shared.payment.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Period;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnerPinSubscriptionSchedulerTest {

    @Mock private PartnerPinSubscriptionRepository subscriptionRepository;
    @Mock private PaymentGateway paymentGateway;

    @InjectMocks private PartnerPinSubscriptionScheduler scheduler;

    @Test
    void processDueSubscriptions_whenCancellationPending_expiresWithoutCharging() {
        PartnerPinSubscription subscription = mock(PartnerPinSubscription.class);
        when(subscription.getCancelAtPeriodEnd()).thenReturn(true);
        when(subscriptionRepository.findByStatusAndCurrentPeriodEndBefore(any(), any()))
                .thenReturn(List.of(subscription));

        scheduler.processDueSubscriptions();

        verify(subscription).expire();
        verify(subscription, never()).renew(any());
        verify(paymentGateway, never()).charge(any());
    }

    @Test
    void processDueSubscriptions_whenRenewalChargeDeclined_expiresSubscription() {
        PartnerPin pin = mock(PartnerPin.class);
        when(pin.getName()).thenReturn("Cafe Noir");
        PartnerPinSubscription subscription = mock(PartnerPinSubscription.class);
        when(subscription.getCancelAtPeriodEnd()).thenReturn(false);
        when(subscription.getPartnerPin()).thenReturn(pin);
        when(subscriptionRepository.findByStatusAndCurrentPeriodEndBefore(any(), any()))
                .thenReturn(List.of(subscription));
        when(paymentGateway.charge(any())).thenReturn(new PaymentResult(false, null));

        scheduler.processDueSubscriptions();

        verify(subscription).expire();
        verify(subscription, never()).renew(any(Period.class));
    }
}
