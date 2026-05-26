package hr.tvz.experimate.experimate.domain.partner_event;

import hr.tvz.experimate.experimate.domain.partner.PartnerProfile;
import hr.tvz.experimate.experimate.domain.partner.PartnerProfileRepository;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPin;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPinNotFoundException;
import hr.tvz.experimate.experimate.domain.partner_pin.PartnerPinRepository;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for partner events.
 *
 * <p>Events are scoped to a {@link PartnerPin}. All write operations verify that
 * the requesting user owns the pin the event belongs to before making changes.
 * Read operations are open to all authenticated users.
 */
@Service
public class PartnerEventService {

    private final PartnerEventRepository partnerEventRepository;
    private final PartnerPinRepository partnerPinRepository;
    private final PartnerProfileRepository partnerProfileRepository;

    public PartnerEventService(PartnerEventRepository partnerEventRepository,
                               PartnerPinRepository partnerPinRepository,
                               PartnerProfileRepository partnerProfileRepository) {
        this.partnerEventRepository = partnerEventRepository;
        this.partnerPinRepository = partnerPinRepository;
        this.partnerProfileRepository = partnerProfileRepository;
    }

    /**
     * Creates a new event on the given pin.
     * The requesting user must own the pin.
     *
     * @param pinId  the ID of the pin to attach the event to
     * @param userId the authenticated partner's user ID
     * @param req    event data; both datetimes must be in the future and end must follow start
     * @return the created event
     * @throws IllegalArgumentException if end is not after start
     * @throws ForbiddenActionException if the user does not own the pin
     */
    @Transactional
    public PartnerEventResponse createEvent(Integer pinId, Integer userId, CreatePartnerEventRequest req) {
        PartnerPin pin = findPinOrThrow(pinId);
        checkPinOwnership(pin, resolveProfile(userId));

        if (!req.endDatetime().isAfter(req.startDatetime())) {
            throw new IllegalArgumentException("endDatetime must be after startDatetime");
        }

        PartnerEvent event = new PartnerEvent(
                pin, req.title(), req.description(), req.ticketVendorUrl(),
                req.startDatetime(), req.endDatetime(), LocalDateTime.now());

        return toResponse(partnerEventRepository.save(event));
    }

    /**
     * Returns all events for the given pin, ordered by start time ascending.
     * Accessible to all authenticated users.
     *
     * @param pinId the pin whose events to list
     */
    @Transactional(readOnly = true)
    public List<PartnerEventResponse> getEventsForPin(Integer pinId) {
        findPinOrThrow(pinId);
        return partnerEventRepository.findAllByPartnerPin_IdOrderByStartDatetimeAsc(pinId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns all events across every pin owned by the given user's partner profile.
     * Used by {@code GET /api/partner/events} to populate the partner dashboard.
     *
     * @param userId the authenticated partner's user ID
     */
    @Transactional(readOnly = true)
    public List<PartnerEventResponse> getEventsForUser(Integer userId) {
        return partnerEventRepository.findByPartnerPin_PartnerProfile_UserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single event by ID. Accessible to all authenticated users.
     *
     * @param eventId the event's ID
     * @throws NoSuchElementException if the event does not exist
     */
    @Transactional(readOnly = true)
    public PartnerEventResponse getEventById(Integer eventId) {
        return toResponse(findEventOrThrow(eventId));
    }

    /**
     * Updates an event. Only non-null fields in the request are applied.
     * The requesting user must own the pin the event belongs to.
     *
     * @param eventId the event to update
     * @param userId  the authenticated user's ID
     * @param req     fields to update
     * @return the updated event
     * @throws ForbiddenActionException if the user does not own the event's pin
     * @throws IllegalArgumentException if the updated datetimes violate end-after-start
     */
    @Transactional
    public PartnerEventResponse updateEvent(Integer eventId, Integer userId, UpdatePartnerEventRequest req) {
        PartnerEvent event = findEventOrThrow(eventId);
        checkPinOwnership(event.getPartnerPin(), resolveProfile(userId));

        if (req.title() != null) event.setTitle(req.title());
        if (req.description() != null) event.setDescription(req.description());
        if (req.ticketVendorUrl() != null) event.setTicketVendorUrl(req.ticketVendorUrl());
        if (req.startDatetime() != null) event.setStartDatetime(req.startDatetime());
        if (req.endDatetime() != null) event.setEndDatetime(req.endDatetime());

        if (!event.getEndDatetime().isAfter(event.getStartDatetime())) {
            throw new IllegalArgumentException("endDatetime must be after startDatetime");
        }

        return toResponse(partnerEventRepository.save(event));
    }

    /**
     * Deletes an event.
     * The requesting user must own the pin the event belongs to.
     *
     * @param eventId the event to delete
     * @param userId  the authenticated user's ID
     * @throws ForbiddenActionException if the user does not own the event's pin
     */
    @Transactional
    public void deleteEvent(Integer eventId, Integer userId) {
        PartnerEvent event = findEventOrThrow(eventId);
        checkPinOwnership(event.getPartnerPin(), resolveProfile(userId));
        partnerEventRepository.delete(event);
    }

    private PartnerPin findPinOrThrow(Integer pinId) {
        return partnerPinRepository.findById(pinId)
                .orElseThrow(() -> new PartnerPinNotFoundException(pinId));
    }

    private PartnerEvent findEventOrThrow(Integer eventId) {
        return partnerEventRepository.findById(eventId)
                .orElseThrow(() -> new PartnerEventNotFoundException(eventId));
    }

    private PartnerProfile resolveProfile(Integer userId) {
        return partnerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Partner profile not found for user " + userId));
    }

    private void checkPinOwnership(PartnerPin pin, PartnerProfile profile) {
        if (!pin.getPartnerProfile().getId().equals(profile.getId())) {
            throw new ForbiddenActionException("You do not own this partner pin.");
        }
    }

    private PartnerEventResponse toResponse(PartnerEvent event) {
        PartnerPin pin = event.getPartnerPin();
        return new PartnerEventResponse(
                event.getId(),
                pin.getId(),
                pin.getName(),
                pin.getLatitude(),
                pin.getLongitude(),
                event.getTitle(),
                event.getDescription(),
                event.getTicketVendorUrl(),
                event.getStartDatetime(),
                event.getEndDatetime(),
                event.getCreatedAt()
        );
    }
}
