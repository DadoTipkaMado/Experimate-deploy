package hr.tvz.experimate.experimate.model.onboarding;

import hr.tvz.experimate.experimate.model.onboarding.exception.QuizAlreadyCompletedException;
import hr.tvz.experimate.experimate.model.onboarding.exception.QuizCannotBeCancelledException;
import hr.tvz.experimate.experimate.model.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Entity tracking the state and answers of a user's BFI-10 onboarding quiz.
 *
 * <p>A {@code QuizResult} row is created eagerly at user registration with status
 * {@link QuizStatus#AWAITING}. It transitions to {@link QuizStatus#COMPLETED} when
 * the user submits all 10 answers, or to {@link QuizStatus#CANCELLED} via
 * {@link #markCancelled()}. A completed quiz cannot be cancelled.
 *
 * <p>The raw answers are stored in the {@code quiz_result_answers} join table via
 * {@code @ElementCollection} — one row per BFI-10 item, keyed by item number (1–10),
 * with the Likert response (1–5) as the value.
 */
@Entity
@Table(name = "quiz_result")
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    private QuizStatus status = QuizStatus.AWAITING;

    @ElementCollection
    @CollectionTable(
            name = "quiz_result_answers",
            joinColumns = @JoinColumn(name = "quiz_result_id")
    )
    @MapKeyColumn(name = "item_number")
    @Column(name = "answer_value")
    private Map<Integer, Integer> answers = new HashMap<>();

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    /** For Hibernate. */
    protected QuizResult() {}

    /**
     * Creates a new quiz result for the given user in the {@link QuizStatus#AWAITING} state.
     *
     * @param user the user who owns this quiz result
     */
    public QuizResult(User user) {
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Records the submitted answers and transitions the quiz to {@link QuizStatus#COMPLETED}.
     *
     * @param answers map of BFI-10 item number (1–10) to Likert response (1–5);
     *                must contain exactly 10 entries
     * @throws QuizAlreadyCompletedException if the quiz is already in COMPLETED state
     * @throws IllegalArgumentException if the answers map is null or does not contain exactly 10 entries
     */
    public void markCompleted(Map<Integer, Integer> answers) {
        if (this.status == QuizStatus.COMPLETED)
            throw new QuizAlreadyCompletedException(user.getId());
        if (answers == null || answers.size() != 10)
            throw new IllegalArgumentException("Answers must contain exactly 10 entries");
        if (answers.values().stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("All answers must be non-null");
        this.answers = answers;
        this.status = QuizStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Transitions the quiz to {@link QuizStatus#CANCELLED}.
     *
     * @throws QuizCannotBeCancelledException if the quiz is not in {@link QuizStatus#AWAITING} state
     */
    public void markCancelled() {
        if (this.status != QuizStatus.AWAITING)
            throw new QuizCannotBeCancelledException(user.getId());
        this.status = QuizStatus.CANCELLED;
    }

    /**
     * Resets this quiz result to the {@link QuizStatus#AWAITING} state, clearing all
     * previously submitted answers and the completion timestamp.
     *
     * <p>Called during a GDPR data deletion request to allow the user to retake the quiz.
     */
    public void reset() {
        this.answers.clear();
        this.status = QuizStatus.AWAITING;
        this.completedAt = null;
    }

    /** @return the primary key of this quiz result. */
    public Integer getId() { return id; }

    /** @return the user who owns this quiz result. */
    public User getUser() { return user; }

    /** @return the current status of the quiz. */
    public QuizStatus getStatus() { return status; }

    /** @return the raw BFI-10 answers, or an empty map if not yet submitted. */
    public Map<Integer, Integer> getAnswers() { return answers; }

    /** @return the timestamp when this quiz result was created. */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @return the timestamp when the quiz was completed, or {@code null} if not yet completed. */
    public LocalDateTime getCompletedAt() { return completedAt; }
}
