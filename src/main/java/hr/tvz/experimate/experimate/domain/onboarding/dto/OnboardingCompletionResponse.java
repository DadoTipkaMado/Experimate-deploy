package hr.tvz.experimate.experimate.domain.onboarding.dto;

/**
 * Response DTO returned when a user successfully completes the onboarding quiz.
 *
 * @param vector            the computed Big Five personality scores
 * @param onboardingSummary a natural-language summary of the personality profile, or {@code null}
 */
public record OnboardingCompletionResponse(Big5VectorDto vector, String onboardingSummary) {}
