package hr.tvz.experimate.experimate.model.onboarding.dto;

import hr.tvz.experimate.experimate.model.onboarding.QuizStatus;

import java.time.LocalDateTime;

/**
 * Response DTO representing the current state of a user's onboarding quiz.
 *
 * @param status      the current {@link QuizStatus} of the quiz
 * @param completedAt the timestamp when the quiz was completed, or {@code null} if not yet completed
 */
public record QuizStatusResponse(QuizStatus status, LocalDateTime completedAt) {}
