package hr.tvz.experimate.experimate.model.onboarding;

/**
 * Immutable value object holding a user's Big Five personality scores.
 *
 * <p>Each field represents one OCEAN dimension as a continuous float in {@code [-1.0, +1.0]}.
 * The value reflects the average of the two BFI-10 items for that dimension after reverse-keying
 * and normalisation — any float in the range is possible, not just the endpoints.
 *
 * <p>Produced by {@link Big5Calculator#compute(java.util.Map)} and stored on the {@code User} entity.
 *
 * @param openness          Openness to experience (UI label: Curiosity)
 * @param conscientiousness Conscientiousness (UI label: Focus)
 * @param extraversion      Extraversion (UI label: Energy)
 * @param agreeableness     Agreeableness (UI label: Warmth)
 * @param neuroticism       Neuroticism (UI label: Sensitivity)
 */
public record Big5Vector(
        double openness,
        double conscientiousness,
        double extraversion,
        double agreeableness,
        double neuroticism
) {}
