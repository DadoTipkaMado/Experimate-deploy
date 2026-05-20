package hr.tvz.experimate.experimate.domain.onboarding.exception;

import hr.tvz.experimate.experimate.shared.exception.ConflictException;

/**
 * Thrown when a user attempts to complete the onboarding quiz after exhausting
 * the maximum number of allowed completions.
 *
 * <p>Extends {@link ConflictException}, which is globally mapped to HTTP 409 Conflict.
 */
public class QuizRetakeLimitExceededException extends ConflictException {

    public QuizRetakeLimitExceededException(Integer userId, int limit) {
        super("User %d has reached the maximum of %d quiz completions".formatted(userId, limit));
    }
}
