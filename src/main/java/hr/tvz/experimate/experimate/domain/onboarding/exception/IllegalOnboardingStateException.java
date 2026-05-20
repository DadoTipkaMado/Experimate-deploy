package hr.tvz.experimate.experimate.domain.onboarding.exception;

/**
 * Base exception for illegal state transitions in the onboarding quiz flow.
 *
 * <p>Extends {@link IllegalStateException}, which is globally mapped to HTTP 409 Conflict.
 * Subclass this for any operation that is invalid given the current
 * {@link hr.tvz.experimate.experimate.domain.onboarding.QuizStatus} of the quiz.
 */
public class IllegalOnboardingStateException extends IllegalStateException {

    public IllegalOnboardingStateException(String message) {
        super(message);
    }
}
