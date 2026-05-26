package hr.tvz.experimate.experimate.domain.promoted_ad;

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

/**
 * Business logic for partner feed advertisements.
 *
 * <p>Handles CRUD for {@link PromotedAd} entities and ad image file management.
 * All write operations verify that the requesting user owns the target ad.
 * The {@code findAllActiveBetween} query is used by {@link hr.tvz.experimate.experimate.domain.feed.FeedService FeedService}
 * to populate the interleaved feed.
 */
@Service
public class PromotedAdService {

    @Value("${app.upload.promoted-ad-images-dir}")
    private String adImagesDir;

    private final PromotedAdRepository promotedAdRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final FileStorageService fileStorageService;

    public PromotedAdService(PromotedAdRepository promotedAdRepository,
                             PartnerProfileRepository partnerProfileRepository,
                             FileStorageService fileStorageService) {
        this.promotedAdRepository = promotedAdRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Creates a new promoted ad for the requesting partner.
     *
     * @param userId the authenticated partner's user ID
     * @param req    ad creation data
     * @return the created ad
     */
    @Transactional
    public PromotedAdResponse createAd(Integer userId, CreatePromotedAdRequest req) {
        PartnerProfile profile = resolveProfile(userId);
        PromotedAd ad = new PromotedAd(
                profile, req.title(), req.description(), req.linkUrl(),
                req.activeFrom(), req.activeUntil(), LocalDateTime.now());
        return toResponse(promotedAdRepository.save(ad));
    }

    /**
     * Returns all ads belonging to the requesting partner.
     *
     * @param userId the authenticated partner's user ID
     */
    @Transactional(readOnly = true)
    public List<PromotedAdResponse> getMyAds(Integer userId) {
        PartnerProfile profile = resolveProfile(userId);
        return promotedAdRepository.findAllByPartnerProfile_Id(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns all currently active ads within their scheduling window.
     * Used by the feed service to interleave ads with listings.
     *
     * @param now the current timestamp to evaluate scheduling boundaries against
     */
    @Transactional(readOnly = true)
    public List<PromotedAdResponse> findActiveAds(LocalDateTime now) {
        return promotedAdRepository.findAllActiveBetween(now)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Updates a promoted ad. Only non-null fields in the request are applied.
     * The requesting user must own the ad.
     *
     * @param adId   the ad to update
     * @param userId the authenticated user's ID
     * @param req    fields to update
     * @return the updated ad
     * @throws ForbiddenActionException if the user does not own the ad
     */
    @Transactional
    public PromotedAdResponse updateAd(Integer adId, Integer userId, UpdatePromotedAdRequest req) {
        PromotedAd ad = findAdOrThrow(adId);
        checkOwnership(ad, resolveProfile(userId));

        if (req.title() != null) ad.setTitle(req.title());
        if (req.description() != null) ad.setDescription(req.description());
        if (req.linkUrl() != null) ad.setLinkUrl(req.linkUrl());
        if (req.active() != null) ad.setActive(req.active());
        if (req.activeFrom() != null) ad.setActiveFrom(req.activeFrom());
        if (req.activeUntil() != null) ad.setActiveUntil(req.activeUntil());

        return toResponse(promotedAdRepository.save(ad));
    }

    /**
     * Deletes a promoted ad and its image file (if any).
     * The requesting user must own the ad.
     *
     * @param adId   the ad to delete
     * @param userId the authenticated user's ID
     * @throws ForbiddenActionException if the user does not own the ad
     */
    @Transactional
    public void deleteAd(Integer adId, Integer userId) {
        PromotedAd ad = findAdOrThrow(adId);
        checkOwnership(ad, resolveProfile(userId));
        if (ad.getImageFilename() != null) {
            fileStorageService.delete(ad.getImageFilename(), adImagesDir);
        }
        promotedAdRepository.delete(ad);
    }

    /**
     * Stores a new image for the given ad and removes the previous one if present.
     * The requesting user must own the ad.
     *
     * @param adId   the ad receiving the image
     * @param userId the authenticated user's ID
     * @param file   the uploaded image
     * @return the updated ad
     * @throws ForbiddenActionException if the user does not own the ad
     * @throws IllegalArgumentException if the file is empty or has a disallowed content type
     */
    @Transactional
    public PromotedAdResponse uploadImage(Integer adId, Integer userId, MultipartFile file) {
        PromotedAd ad = findAdOrThrow(adId);
        checkOwnership(ad, resolveProfile(userId));

        String oldFilename = ad.getImageFilename();
        String newFilename = fileStorageService.store(file, adImagesDir);
        ad.setImageFilename(newFilename);
        if (oldFilename != null) fileStorageService.delete(oldFilename, adImagesDir);

        return toResponse(promotedAdRepository.save(ad));
    }

    /**
     * Loads a promoted ad image as a file resource for the HTTP response.
     *
     * @param filename the image filename stored on disk
     * @return the file resource
     */
    public Resource getImageResource(String filename) {
        return fileStorageService.load(filename, adImagesDir);
    }

    private PromotedAd findAdOrThrow(Integer adId) {
        return promotedAdRepository.findById(adId)
                .orElseThrow(() -> new PromotedAdNotFoundException(adId));
    }

    private PartnerProfile resolveProfile(Integer userId) {
        return partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Partner profile not found for user " + userId));
    }

    private void checkOwnership(PromotedAd ad, PartnerProfile profile) {
        if (!ad.getPartnerProfile().getId().equals(profile.getId())) {
            throw new ForbiddenActionException("You do not own this promoted ad.");
        }
    }

    private PromotedAdResponse toResponse(PromotedAd ad) {
        String imageUrl = ad.getImageFilename() != null
                ? "/api/promoted-ads/image/" + ad.getImageFilename()
                : null;
        return new PromotedAdResponse(
                ad.getId(),
                ad.getTitle(),
                ad.getDescription(),
                imageUrl,
                ad.getLinkUrl(),
                ad.getActive(),
                ad.getViewCount(),
                ad.getActiveFrom(),
                ad.getActiveUntil(),
                ad.getCreatedAt()
        );
    }
}
