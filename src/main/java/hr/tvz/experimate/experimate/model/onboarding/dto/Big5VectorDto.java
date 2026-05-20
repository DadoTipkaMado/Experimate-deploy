package hr.tvz.experimate.experimate.model.onboarding.dto;

/**
 * Response DTO exposing a user's computed Big Five personality scores.
 *
 * <p>Each field is a continuous value in {@code [-1.0, +1.0]}, derived from the
 * BFI-10 instrument after reverse-keying and normalisation.
 */
public record Big5VectorDto(
        double openness,
        double conscientiousness,
        double extraversion,
        double agreeableness,
        double neuroticism
) {}
