package hr.tvz.experimate.experimate.domain.promoted_ad;

import hr.tvz.experimate.experimate.domain.partner.PartnerProfile;
import hr.tvz.experimate.experimate.domain.partner.PartnerProfileRepository;
import hr.tvz.experimate.experimate.shared.FileStorageService;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.shared.payment.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotedAdServiceTest {

    @Mock private PromotedAdRepository promotedAdRepository;
    @Mock private PartnerProfileRepository partnerProfileRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private PaymentGateway paymentGateway;

    @InjectMocks private PromotedAdService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "adImagesDir", "./test-ad-images");
    }

    // ──────────────── updateAd ────────────────

    @Test
    void updateAd_throwsWhenAdNotFound() {
        when(promotedAdRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAd(1, 10, new UpdatePromotedAdRequest(null, null, null, null, null, null)))
                .isInstanceOf(PromotedAdNotFoundException.class);
    }

    @Test
    void updateAd_throwsForbiddenWhenCallerDoesNotOwnAd() {
        PromotedAd ad = mock(PromotedAd.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(promotedAdRepository.findById(1)).thenReturn(Optional.of(ad));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(ad.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.updateAd(1, 10, new UpdatePromotedAdRequest(null, null, null, null, null, null)))
                .isInstanceOf(ForbiddenActionException.class);
    }

    // ──────────────── deleteAd ────────────────

    @Test
    void deleteAd_throwsWhenAdNotFound() {
        when(promotedAdRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAd(1, 10))
                .isInstanceOf(PromotedAdNotFoundException.class);
    }

    @Test
    void deleteAd_throwsForbiddenWhenCallerDoesNotOwnAd() {
        PromotedAd ad = mock(PromotedAd.class);
        PartnerProfile ownerProfile = mock(PartnerProfile.class);
        PartnerProfile callerProfile = mock(PartnerProfile.class);

        when(promotedAdRepository.findById(1)).thenReturn(Optional.of(ad));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(callerProfile));
        when(ad.getPartnerProfile()).thenReturn(ownerProfile);
        when(ownerProfile.getId()).thenReturn(99);
        when(callerProfile.getId()).thenReturn(42);

        assertThatThrownBy(() -> service.deleteAd(1, 10))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void deleteAd_alsoDeletesImageFileWhenImageExists() {
        PromotedAd ad = mock(PromotedAd.class);
        PartnerProfile profile = mock(PartnerProfile.class);

        when(promotedAdRepository.findById(1)).thenReturn(Optional.of(ad));
        when(partnerProfileRepository.findByUserId(10)).thenReturn(Optional.of(profile));
        when(ad.getPartnerProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(99);
        when(ad.getImageFilename()).thenReturn("old-image.png");

        service.deleteAd(1, 10);

        verify(fileStorageService).delete("old-image.png", "./test-ad-images");
        verify(promotedAdRepository).delete(ad);
    }
}
