package hr.tvz.experimate.experimate.domain.partner;

/**
 * Response body for {@code GET /api/partner/status}.
 *
 * <p>{@code isPartner} is {@code true} when the authenticated user holds {@code ROLE_PARTNER}.
 * {@code profile} contains the full partner profile when {@code isPartner} is {@code true},
 * and is {@code null} otherwise.
 */
public record PartnerStatusResponse(boolean isPartner, PartnerProfileResponse profile) {}
