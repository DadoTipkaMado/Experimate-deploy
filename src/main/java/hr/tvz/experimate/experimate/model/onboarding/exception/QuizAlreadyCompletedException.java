package hr.tvz.experimate.experimate.model.onboarding.exception;

/**
 * Thrown when an attempt is made to complete a quiz that has already been completed.
 *
 * <p>Extends {@link IllegalOnboardingStateException}, which is globally mapped to HTTP 409 Conflict.
 */
public class QuizAlreadyCompletedException extends IllegalOnboardingStateException {

    public QuizAlreadyCompletedException(Integer userId) {
        super("Quiz for user %d is already completed".formatted(userId));
    }
}
