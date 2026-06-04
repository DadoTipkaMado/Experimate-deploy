package hr.tvz.experimate.experimate.domain.partner_event;

import hr.tvz.experimate.experimate.domain.partner.PartnerProfile;
import hr.tvz.experimate.experimate.domain.partner.PartnerProfileRepository;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPin;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPinNotFoundException;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPinRepository;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.shared.payment.PaymentGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerEventServiceTest {

    @Mock private PartnerEventRepository partnerEventRepository;
    @Mock private PartnerPinRepository partnerPinRepository;
    @Mock private PartnerProfileRepository partnerProfileRepository;
    @Mock private PaymentGateway paymentGateway;

    @InjectMocks private PartnerEventService service;

    // ──────────────── createEvent ────────────────

    @Test
    void createEvent_throwsWhenPinNotFound() {
        when(partnerPinRepository.findById(1)).thenReturn(Optional.empty());

        LocalDateTime start = LocalDateTime.now().plusHours(2);
        CreatePartnerEventRequest req = new CreatePartnerEventRequest("Title", null, null, start, start.plusHours(2));

        assertThatThrownBy(() -> service.createEvent(1, 10, req))
                .isInstanceOf(PartnerPinNotFoundException.class);
    }

    @Test
    void createEvent_throwsForbiddenWhenCallerDoesNotOwnPin() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(pin.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        LocalDateTime start = LocalDateTime.now().plusHours(2);
        CreatePartnerEventRequest req = new CreatePartnerEventRequest("Title", null, null, start, start.plusHours(2));

        assertThatThrownBy(() -> service.createEvent(1, 10, req))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void createEvent_throwsWhenEndIsNotAfterStart() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile profile = mock(PartnerProfile.class);

        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(profile));
        // isti profil → ownership prolazi, dolazimo do validacije
        when(pin.getPartnerProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(99);

        LocalDateTime start = LocalDateTime.now().plusHours(2);
        CreatePartnerEventRequest req = new CreatePartnerEventRequest("Title", null, null, start, start.minusHours(1));

        assertThatThrownBy(() -> service.createEvent(1, 10, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ──────────────── updateEvent ────────────────

    @Test
    void updateEvent_throwsForbiddenWhenCallerDoesNotOwnPin() {
        PartnerEvent event = mock(PartnerEvent.class);
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(partnerEventRepository.findById(1)).thenReturn(Optional.of(event));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(event.getPartnerPin()).thenReturn(pin);
        when(pin.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.updateEvent(1, 10, new UpdatePartnerEventRequest(null, null, null, null, null)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void updateEvent_throwsWhenPartialUpdateMakesEndBeforeStart() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end = start.plusHours(2);
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile profile = mock(PartnerProfile.class);

        // pravi entitet — setEndDatetime() zaista mijenja polje, za razliku od mocka
        PartnerEvent event = new PartnerEvent(pin, "Title", null, null, start, end, LocalDateTime.now());

        when(partnerEventRepository.findById(1)).thenReturn(Optional.of(event));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(profile));
        when(pin.getPartnerProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(99);

        // samo endDatetime je promijenjen — na vrijednost koja je ispred starta
        UpdatePartnerEventRequest req = new UpdatePartnerEventRequest(null, null, null, null, start.minusHours(1));

        assertThatThrownBy(() -> service.updateEvent(1, 10, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ──────────────── deleteEvent ────────────────

    @Test
    void deleteEvent_throwsWhenEventNotFound() {
        when(partnerEventRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEvent(1, 10))
                .isInstanceOf(PartnerEventNotFoundException.class);
    }

    @Test
    void deleteEvent_throwsForbiddenWhenCallerDoesNotOwnPin() {
        PartnerEvent event = mock(PartnerEvent.class);
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(partnerEventRepository.findById(1)).thenReturn(Optional.of(event));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(event.getPartnerPin()).thenReturn(pin);
        when(pin.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.deleteEvent(1, 10))
                .isInstanceOf(ForbiddenActionException.class);
    }
}
