package hr.tvz.experimate.experimate.model.onboarding.exception;

/**
 * Thrown when a quiz cancellation is attempted on a quiz that is not in the
 * {@link hr.tvz.experimate.experimate.model.onboarding.QuizStatus#AWAITING AWAITING} state.
 *
 * <p>Extends {@link IllegalOnboardingStateException}, which is globally mapped to HTTP 409 Conflict.
 */
public class QuizCannotBeCancelledException extends IllegalOnboardingStateException {

    public QuizCannotBeCancelledException(Integer userId) {
        super("Quiz for user %d cannot be cancelled — it is not in AWAITING state".formatted(userId));
    }
}
