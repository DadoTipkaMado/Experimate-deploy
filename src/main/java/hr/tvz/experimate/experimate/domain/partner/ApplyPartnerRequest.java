package hr.tvz.experimate.experimate.domain.partner;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/partner/apply}.
 * Collected during self-serve partner onboarding; no admin approval required.
 */
public record ApplyPartnerRequest(

        @NotBlank
        String companyName,

        @NotBlank
        @Email
        String contactEmail,

        String website
) {}
