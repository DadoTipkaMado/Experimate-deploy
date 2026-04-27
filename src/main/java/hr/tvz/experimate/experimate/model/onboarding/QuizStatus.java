package hr.tvz.experimate.experimate.model.onboarding;

/**
 * Lifecycle status of a user's BFI-10 personality quiz.
 *
 * <ul>
 *   <li>{@link #AWAITING} – quiz has been created for the user but not yet completed.</li>
 *   <li>{@link #COMPLETED} – user submitted all 10 answers; Big Five vector is available.</li>
 *   <li>{@link #CANCELLED} – user explicitly opted out; generic (non-personalised) search applies.</li>
 * </ul>
 */
public enum QuizStatus {
    AWAITING,
    COMPLETED,
    CANCELLED
}
