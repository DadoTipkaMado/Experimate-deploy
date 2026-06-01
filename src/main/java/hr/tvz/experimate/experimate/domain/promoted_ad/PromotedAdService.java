package hr.tvz.experimate.experimate.domain.promoted_ad;

import hr.tvz.experimate.experimate.domain.partner.PartnerProfile;
import hr.tvz.experimate.experimate.domain.partner.PartnerProfileRepository;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEvent;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventNotFoundException;
import hr.tvz.experimate.experimate.domain.partner_event.PartnerEventRepository;
import hr.tvz.experimate.experimate.shared.FileStorageService;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import hr.tvz.experimate.experimate.shared.payment.ChargeRequest;
import hr.tvz.experimate.experimate.shared.payment.PaymentFailedException;
import hr.tvz.experimate.experimate.shared.payment.PaymentGateway;
import hr.tvz.experimate.experimate.shared.payment.PaymentResult;
import hr.tvz.experimate.experimate.shared.payment.Pricing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Business logic for partner feed advertisements.
 *
 * <p>Handles CRUD for {@link PromotedAd} entities and ad image file management.
 * All write operations verify that the requesting user owns the target ad.
 * The {@code findAllActiveBetween} query is used by {@link hr.tvz.experimate.experimate.domain.feed.FeedService FeedService}
 * to populate the interleaved feed.
 *
 * <p>Display in the feed is paid for by duration: creating an ad (or promoting an event) charges a
 * fixed daily rate for the display window via {@link PaymentGateway}, and {@link #extend} charges
 * again to add more days. Pricing comes from {@link Pricing}.
 */
@Service
public class PromotedAdService {

    @Value("${app.upload.promoted-ad-images-dir}")
    private String adImagesDir;

    private final PromotedAdRepository promotedAdRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final PartnerEventRepository partnerEventRepository;
    private final FileStorageService fileStorageService;
    private final PaymentGateway paymentGateway;

    public PromotedAdService(PromotedAdRepository promotedAdRepository,
                             PartnerProfileRepository partnerProfileRepository,
                             PartnerEventRepository partnerEventRepository,
                             FileStorageService fileStorageService,
                             PaymentGateway paymentGateway) {
        this.promotedAdRepository = promotedAdRepository;
        this.partnerProfileRepository = partnerProfileRepository;
        this.partnerEventRepository = partnerEventRepository;
        this.fileStorageService = fileStorageService;
        this.paymentGateway = paymentGateway;
    }

    /**
     * Creates a new promoted ad for the requesting partner.
     *
     * @param userId the authenticated partner's user ID
     * @param req    ad creation data
     * @return the created ad
     * @throws PaymentFailedException if the charge for the display window is declined
     */
    @Transactional
    public PromotedAdResponse createAd(Integer userId, CreatePromotedAdRequest req) {
        PartnerProfile profile = resolveProfile(userId);
        chargeForWindow(req.title(), req.activeFrom(), req.activeUntil());
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
     * Extends an ad's display window by paying for additional days.
     *
     * <p>Mirrors the premium "extend, don't reset" rule: the extra days are added on top of
     * {@code max(now, activeUntil)}, so an ad that is still running gains time at the end while an
     * ad that already lapsed starts a fresh window from now. The requesting user must own the ad.
     *
     * @param adId   the ad to extend
     * @param userId the authenticated user's ID
     * @param req    the number of additional display days to purchase
     * @return the updated ad with its new {@code activeUntil}
     * @throws ForbiddenActionException if the user does not own the ad
     * @throws PaymentFailedException   if the charge for the added days is declined
     */
    @Transactional
    public PromotedAdResponse extend(Integer adId, Integer userId, ExtendPromotedAdRequest req) {
        PromotedAd ad = findAdOrThrow(adId);
        checkOwnership(ad, resolveProfile(userId));

        int days = req.additionalDays();
        charge(days, "ExperiMate promoted ad extension — " + ad.getTitle() + " (" + days + " day(s))");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = (ad.getActiveUntil() != null && ad.getActiveUntil().isAfter(now))
                ? ad.getActiveUntil()
                : now;
        ad.setActiveUntil(base.plusDays(days));

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
     * Promotes a partner event into the feed by creating a {@link PromotedAd} that wraps it.
     *
     * <p>Title, description and link default to the event's title, description and ticket vendor
     * URL; each is overridable via the request, and blank overrides fall back to the event value.
     * The promotion starts immediately ({@code activeFrom = null}) and runs until the event ends
     * ({@code activeUntil = event.endDatetime}).
     *
     * @param userId  the authenticated partner's user ID
     * @param eventId the event to promote
     * @param req     optional field overrides
     * @return the created promotion as an ad response
     * @throws PartnerEventNotFoundException if the event does not exist
     * @throws ForbiddenActionException      if the user does not own the event's pin
     * @throws EventAlreadyPromotedException if the event already has a promotion
     * @throws PaymentFailedException        if the charge for the display window is declined
     */
    @Transactional
    public PromotedAdResponse promoteEvent(Integer userId, Integer eventId, PromoteEventRequest req) {
        PartnerProfile profile = resolveProfile(userId);
        PartnerEvent event = partnerEventRepository.findById(eventId)
                .orElseThrow(() -> new PartnerEventNotFoundException(eventId));
        checkEventOwnership(event, profile);

        if (promotedAdRepository.existsByPartnerEvent_Id(eventId)) {
            throw new EventAlreadyPromotedException(eventId);
        }

        String title = StringUtils.hasText(req.overrideTitle()) ? req.overrideTitle() : event.getTitle();
        String description = StringUtils.hasText(req.overrideDescription())
                ? req.overrideDescription() : event.getDescription();
        String linkUrl = StringUtils.hasText(req.overrideLinkUrl())
                ? req.overrideLinkUrl() : event.getTicketVendorUrl();

        // The promotion runs from now until the event ends, so it is billed for that window.
        chargeForWindow(title, null, event.getEndDatetime());

        PromotedAd ad = new PromotedAd(
                profile, title, description, linkUrl, null, event.getEndDatetime(), LocalDateTime.now());
        ad.setPartnerEvent(event);
        return toResponse(promotedAdRepository.save(ad));
    }

    /**
     * Removes an event's promotion from the feed.
     * The requesting user must own the pin the event belongs to.
     *
     * @param userId  the authenticated partner's user ID
     * @param eventId the event whose promotion to remove
     * @throws PromotedAdNotFoundException if the event has no promotion
     * @throws ForbiddenActionException    if the user does not own the event's pin
     */
    @Transactional
    public void unpromoteEvent(Integer userId, Integer eventId) {
        PromotedAd ad = promotedAdRepository.findByPartnerEvent_Id(eventId)
                .orElseThrow(() -> PromotedAdNotFoundException.forEvent(eventId));
        checkEventOwnership(ad.getPartnerEvent(), resolveProfile(userId));
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

    /**
     * Charges for an ad's display window. The window runs from {@code activeFrom} (or now, if null)
     * to {@code activeUntil}; an open-ended ad ({@code activeUntil == null}) is billed for
     * {@link Pricing#PROMOTED_AD_OPEN_ENDED_DAYS} days, since an unbounded window cannot be priced.
     *
     * @param title       the ad title, included in the charge description
     * @param activeFrom  start of the display window, or {@code null} for "starts now"
     * @param activeUntil end of the display window, or {@code null} for open-ended
     * @throws PaymentFailedException if the gateway declines the charge
     */
    private void chargeForWindow(String title, LocalDateTime activeFrom, LocalDateTime activeUntil) {
        LocalDateTime start = activeFrom != null ? activeFrom : LocalDateTime.now();
        LocalDateTime end = activeUntil != null
                ? activeUntil
                : start.plusDays(Pricing.PROMOTED_AD_OPEN_ENDED_DAYS);
        long days = billableDays(start, end);
        charge(days, "ExperiMate promoted ad — " + title + " (" + days + " day(s))");
    }

    /**
     * Charges {@link Pricing#PROMOTED_AD_PER_DAY} for the given number of display days.
     *
     * @param days        number of days to bill
     * @param description charge description forwarded to the gateway
     * @throws PaymentFailedException if the gateway declines the charge
     */
    private void charge(long days, String description) {
        BigDecimal amount = Pricing.PROMOTED_AD_PER_DAY.multiply(BigDecimal.valueOf(days));
        PaymentResult result = paymentGateway.charge(new ChargeRequest(amount, Pricing.CURRENCY, description));
        if (!result.success()) {
            throw new PaymentFailedException("Payment declined for promoted ad");
        }
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

    private void checkEventOwnership(PartnerEvent event, PartnerProfile profile) {
        if (!event.getPartnerPin().getPartnerProfile().getId().equals(profile.getId())) {
            throw new ForbiddenActionException("You do not own this partner event.");
        }
    }

    private PromotedAdResponse toResponse(PromotedAd ad) {
        String imageUrl = ad.getImageFilename() != null
                ? "/api/promoted-ads/image/" + ad.getImageFilename()
                : null;
        Integer eventId = ad.getPartnerEvent() != null ? ad.getPartnerEvent().getId() : null;
        return new PromotedAdResponse(
                ad.getId(),
                ad.getTitle(),
                ad.getDescription(),
                imageUrl,
                ad.getLinkUrl(),
                ad.getActive(),
                ad.getViewCount(),
                eventId,
                ad.getActiveFrom(),
                ad.getActiveUntil(),
                ad.getCreatedAt()
        );
    }

    /**
     * Number of days a display window spans for billing, rounding any partial day up and flooring
     * at one day (so even a very short window is charged for at least a single day).
     *
     * @param start start of the window
     * @param end   end of the window
     */
    private static long billableDays(LocalDateTime start, LocalDateTime end) {
        long minutes = ChronoUnit.MINUTES.between(start, end);
        return Math.max(1, (long) Math.ceil(minutes / (60.0 * 24)));
    }
}
