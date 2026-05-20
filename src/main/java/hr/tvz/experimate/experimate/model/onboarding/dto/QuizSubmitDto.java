package hr.tvz.experimate.experimate.model.onboarding.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for submitting BFI-10 quiz answers.
 *
 * <p>Answers must be provided in item order (index 0 = item 1, index 9 = item 10).
 * Each answer is a Likert response on a 1–5 scale.
 *
 * @param answers list of exactly 10 Likert responses (1–5), one per BFI-10 item
 */
public record QuizSubmitDto(
        @NotNull
        @Size(min = 10, max = 10, message = "Exactly 10 answers are required")
        List<@NotNull @Min(1) @Max(5) Integer> answers
) {}
