package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.domain.partner.PartnerProfile;
import hr.tvz.experimate.experimate.domain.partner.PartnerProfileRepository;
import hr.tvz.experimate.experimate.shared.FileStorageService;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerPinServiceTest {

    @Mock private PartnerPinRepository partnerPinRepository;
    @Mock private PartnerProfileRepository partnerProfileRepository;
    @Mock private PartnerPinSubscriptionRepository subscriptionRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private PartnerPinService service;

    @BeforeEach
    void setUp() {
        // @Value fields nisu injektani od strane Mockita — postavljamo ručno da file operacije imaju ne-null dir
        ReflectionTestUtils.setField(service, "partnerLogosDir", "./test-logos");
    }

    // ──────────────── updatePin ────────────────

    @Test
    void updatePin_throwsWhenPinNotFound() {
        when(partnerPinRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePin(1, 10, new UpdatePartnerPinRequest(null, null, null, null, null)))
                .isInstanceOf(PartnerPinNotFoundException.class);
    }

    @Test
    void updatePin_throwsForbiddenWhenCallerDoesNotOwnPin() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(pin.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.updatePin(1, 10, new UpdatePartnerPinRequest(null, null, null, null, null)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    // ──────────────── deletePin ────────────────

    @Test
    void deletePin_throwsWhenPinNotFound() {
        when(partnerPinRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePin(1, 10))
                .isInstanceOf(PartnerPinNotFoundException.class);
    }

    @Test
    void deletePin_throwsForbiddenWhenCallerDoesNotOwnPin() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(pin.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.deletePin(1, 10))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void deletePin_alsoDeletesLogoFileWhenLogoExists() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile profile = mock(PartnerProfile.class);

        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(profile));
        // isti profil na obje strane → ownership check prolazi
        when(pin.getPartnerProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(99);
        when(pin.getLogoFilename()).thenReturn("old-logo.png");

        service.deletePin(1, 10);

        verify(fileStorageService).delete("old-logo.png", "./test-logos");
        verify(partnerPinRepository).delete(pin);
    }

    // ──────────────── uploadLogo ────────────────

    @Test
    void uploadLogo_throwsWhenPinNotFound() {
        when(partnerPinRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.uploadLogo(1, 10, mock(MultipartFile.class)))
                .isInstanceOf(PartnerPinNotFoundException.class);
    }

    @Test
    void uploadLogo_throwsForbiddenWhenCallerDoesNotOwnPin() {
        PartnerPin pin = mock(PartnerPin.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(partnerPinRepository.findById(1)).thenReturn(Optional.of(pin));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(pin.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.uploadLogo(1, 10, mock(MultipartFile.class)))
                .isInstanceOf(ForbiddenActionException.class);
    }
}
