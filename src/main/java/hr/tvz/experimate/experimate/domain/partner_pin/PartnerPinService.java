package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.domain.partner.PartnerProfile;
import hr.tvz.experimate.experimate.domain.partner.PartnerProfileRepository;
import hr.tvz.experimate.experimate.shared.FileStorageService;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Business logic for partner venue pins.
 *
 * <p>Handles CRUD for {@link PartnerPin} entities and logo file management.
 * All write operations verify that the requesting user owns the target pin
 * before applying changes.
 */
@Service
public class PartnerPinService {

    @Value("${app.upload.partner-logos-dir}")
    private String partnerLogosDir;

    private final PartnerPinRepository partnerPinRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final FileStorageService fileStorageService;

    public PartnerPinService(PartnerPinRepository partnerPinRepository,
                             PartnerProfileRepository partnerProfileRepository,
                             FileStorageService fileStorageService) {
        this.partnerPinRepository = partnerPinRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Creates a new venue pin for the requesting partner.
     *
     * @param userId the authenticated partner's user ID
     * @param req    pin creation data
     * @return the created pin
     */
    @Transactional
    public PartnerPinResponse createPin(Integer userId, CreatePartnerPinRequest req) {
        PartnerProfile profile = resolveProfile(userId);
        PartnerPin pin = new PartnerPin(profile, req.name(), req.description(),
                req.latitude(), req.longitude(), LocalDateTime.now());
        return toResponse(partnerPinRepository.save(pin));
    }

    /**
     * Returns all pins belonging to the requesting partner.
     *
     * @param userId the authenticated partner's user ID
     */
    @Transactional(readOnly = true)
    public List<PartnerPinResponse> getMyPins(Integer userId) {
        PartnerProfile profile = resolveProfile(userId);
        return partnerPinRepository.findAllByPartnerProfile_Id(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns all active pins across all partners, for the public map.
     */
    @Transactional(readOnly = true)
    public List<PartnerPinResponse> getAllActivePins() {
        return partnerPinRepository.findAllByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single pin by ID, visible to all authenticated users.
     *
     * @param pinId the pin's ID
     * @throws NoSuchElementException if the pin does not exist
     */
    @Transactional(readOnly = true)
    public PartnerPinResponse getPinById(Integer pinId) {
        return toResponse(findPinOrThrow(pinId));
    }

    /**
     * Updates a partner pin. Only non-null fields in the request are applied.
     * The requesting user must own the pin.
     *
     * @param pinId  the pin to update
     * @param userId the authenticated user's ID
     * @param req    fields to update
     * @return the updated pin
     * @throws ForbiddenActionException if the user does not own the pin
     */
    @Transactional
    public PartnerPinResponse updatePin(Integer pinId, Integer userId, UpdatePartnerPinRequest req) {
        PartnerPin pin = findPinOrThrow(pinId);
        checkOwnership(pin, resolveProfile(userId));

        if (req.name() != null) pin.setName(req.name());
        if (req.description() != null) pin.setDescription(req.description());
        if (req.latitude() != null) pin.setLatitude(req.latitude());
        if (req.longitude() != null) pin.setLongitude(req.longitude());
        if (req.active() != null) pin.setActive(req.active());

        return toResponse(partnerPinRepository.save(pin));
    }

    /**
     * Deletes a pin and its logo file (if any).
     * The requesting user must own the pin.
     *
     * @param pinId  the pin to delete
     * @param userId the authenticated user's ID
     * @throws ForbiddenActionException if the user does not own the pin
     */
    @Transactional
    public void deletePin(Integer pinId, Integer userId) {
        PartnerPin pin = findPinOrThrow(pinId);
        checkOwnership(pin, resolveProfile(userId));
        if (pin.getLogoFilename() != null) {
            fileStorageService.delete(pin.getLogoFilename(), partnerLogosDir);
        }
        partnerPinRepository.delete(pin);
    }

    /**
     * Stores a new logo image for the given pin and removes the previous one if present.
     * The requesting user must own the pin.
     *
     * @param pinId  the pin receiving the logo
     * @param userId the authenticated user's ID
     * @param file   the uploaded image
     * @return the updated pin
     * @throws ForbiddenActionException if the user does not own the pin
     * @throws IllegalArgumentException if the file is empty or has a disallowed content type
     */
    @Transactional
    public PartnerPinResponse uploadLogo(Integer pinId, Integer userId, MultipartFile file) {
        PartnerPin pin = findPinOrThrow(pinId);
        checkOwnership(pin, resolveProfile(userId));

        String oldFilename = pin.getLogoFilename();
        String newFilename = fileStorageService.store(file, partnerLogosDir);
        pin.setLogoFilename(newFilename);
        if (oldFilename != null) fileStorageService.delete(oldFilename, partnerLogosDir);

        return toResponse(partnerPinRepository.save(pin));
    }

    /**
     * Loads a partner pin logo as a file resource for the HTTP response.
     *
     * @param filename the logo filename stored on disk
     * @return the file resource
     */
    public Resource getLogoResource(String filename) {
        return fileStorageService.load(filename, partnerLogosDir);
    }

    private PartnerPin findPinOrThrow(Integer pinId) {
        return partnerPinRepository.findById(pinId)
                .orElseThrow(() -> new PartnerPinNotFoundException(pinId));
    }

    private PartnerProfile resolveProfile(Integer userId) {
        return partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Partner profile not found for user " + userId));
    }

    private void checkOwnership(PartnerPin pin, PartnerProfile profile) {
        if (!pin.getPartnerProfile().getId().equals(profile.getId())) {
            throw new ForbiddenActionException("You do not own this partner pin.");
        }
    }

    private PartnerPinResponse toResponse(PartnerPin pin) {
        String logoUrl = pin.getLogoFilename() != null
                ? "/api/partner-pins/logo/" + pin.getLogoFilename()
                : null;
        return new PartnerPinResponse(
                pin.getId(),
                pin.getName(),
                pin.getDescription(),
                logoUrl,
                pin.getLatitude(),
                pin.getLongitude(),
                pin.getActive(),
                pin.getCreatedAt(),
                pin.getPartnerProfile().getCompanyName()
        );
    }
}
