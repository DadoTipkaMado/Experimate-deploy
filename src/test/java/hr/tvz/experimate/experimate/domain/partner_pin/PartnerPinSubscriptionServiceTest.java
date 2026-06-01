package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.domain.partner.PartnerProfile;
import hr.tvz.experimate.experimate.domain.partner.PartnerProfileRepository;
import hr.tvz.experimate.experimate.shared.exception.ConflictException;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.shared.payment.PaymentFailedException;
import hr.tvz.experimate.experimate.shared.payment.PaymentGateway;
import hr.tvz.experimate.experimate.shared.payment.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnerPinSubscriptionServiceTest {

    @Mock private PartnerPinSubscriptionRepository subscriptionRepository;
    @Mock private PartnerPinRepository partnerPinRepository;
    @Mock private PartnerProfileRepository partnerProfileRepository;
    @Mock private PaymentGateway paymentGateway;

    @InjectMocks private PartnerPinSubscriptionService service;

    // ──────────────── subscribe ────────────────

    @Test
    void subscribe_whenPinNotFound_throwsNotFound() {
        when(partnerPinRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subscribe(10, 1))
                .isInstanceOf(PartnerPinNotFoundException.class);

        verify(paymentGateway, never()).charge(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribe_whenCallerDoesNotOwnPin_throwsForbidden() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(pin.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.subscribe(10, 1))
                .isInstanceOf(ForbiddenActionException.class);

        verify(paymentGateway, never()).charge(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribe_whenNoSubscriptionAndPaymentDeclined_throwsPaymentFailedAndSavesNothing() {
        stubOwnedPin();
        when(subscriptionRepository.findByPartnerPin_Id(1)).thenReturn(Optional.empty());
        when(paymentGateway.charge(any())).thenReturn(new PaymentResult(false, null));

        assertThatThrownBy(() -> service.subscribe(10, 1))
                .isInstanceOf(PaymentFailedException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribe_whenAlreadyActiveAndRenewing_throwsConflictWithoutCharging() {
        stubOwnedPin();
        PartnerPinSubscription active = mock(PartnerPinSubscription.class);
        when(active.isHighlightedAt(any(LocalDateTime.class))).thenReturn(true);
        when(active.getCancelAtPeriodEnd()).thenReturn(false);
        when(subscriptionRepository.findByPartnerPin_Id(1)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.subscribe(10, 1))
                .isInstanceOf(ConflictException.class);

        verify(paymentGateway, never()).charge(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribe_whenExpiredAndPaymentDeclined_throwsPaymentFailedAndSavesNothing() {
        stubOwnedPin();
        PartnerPinSubscription expired = mock(PartnerPinSubscription.class);
        when(expired.isHighlightedAt(any(LocalDateTime.class))).thenReturn(false);
        when(subscriptionRepository.findByPartnerPin_Id(1)).thenReturn(Optional.of(expired));
        when(paymentGateway.charge(any())).thenReturn(new PaymentResult(false, null));

        assertThatThrownBy(() -> service.subscribe(10, 1))
                .isInstanceOf(PaymentFailedException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    // ──────────────── cancel ────────────────

    @Test
    void cancel_whenPinNotFound_throwsNotFound() {
        when(partnerPinRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(10, 1))
                .isInstanceOf(PartnerPinNotFoundException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void cancel_whenCallerDoesNotOwnPin_throwsForbidden() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(pin.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.cancel(10, 1))
                .isInstanceOf(ForbiddenActionException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void cancel_whenNoSubscriptionExists_throwsConflict() {
        stubOwnedPin();
        when(subscriptionRepository.findByPartnerPin_Id(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(10, 1))
                .isInstanceOf(ConflictException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void cancel_whenSubscriptionNoLongerHighlighted_throwsConflict() {
        stubOwnedPin();
        PartnerPinSubscription expired = mock(PartnerPinSubscription.class);
        when(expired.isHighlightedAt(any(LocalDateTime.class))).thenReturn(false);
        when(subscriptionRepository.findByPartnerPin_Id(1)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.cancel(10, 1))
                .isInstanceOf(ConflictException.class);

        verify(expired, never()).requestCancellation();
        verify(subscriptionRepository, never()).save(any());
    }

    // ──────────────── getSubscription ────────────────

    @Test
    void getSubscription_whenPinNotFound_throwsNotFound() {
        when(partnerPinRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubscription(10, 1))
                .isInstanceOf(PartnerPinNotFoundException.class);
    }

    @Test
    void getSubscription_whenCallerDoesNotOwnPin_throwsForbidden() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(pin.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.getSubscription(10, 1))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void getSubscription_whenPinNeverSubscribed_throwsSubscriptionNotFound() {
        stubOwnedPin();
        when(subscriptionRepository.findByPartnerPin_Id(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubscription(10, 1))
                .isInstanceOf(PartnerPinSubscriptionNotFoundException.class);
    }

    /**
     * Wires pin 1 to be owned by the caller (user 10) so ownership passes and the test reaches
     * the subscription/payment branch under test.
     */
    private void stubOwnedPin() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile profile = mock(PartnerProfile.class);
        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(profile));
        when(pin.getPartnerProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(99);
    }
}
