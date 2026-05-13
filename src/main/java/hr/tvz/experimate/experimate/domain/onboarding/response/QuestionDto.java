package hr.tvz.experimate.experimate.domain.onboarding.response;

/**
 * Response DTO representing a single BFI-10 quiz question shown to the user.
 *
 * @param itemNumber the 1-based question number (1–10)
 * @param text       the verbatim statement shown to the user
 */
public record QuestionDto(int itemNumber, String text) {}
