package hr.tvz.experimate.experimate.model.onboarding;

import hr.tvz.experimate.experimate.model.ai.AiMatchingService;
import hr.tvz.experimate.experimate.model.onboarding.dto.*;
import hr.tvz.experimate.experimate.model.onboarding.exception.QuizResultNotFoundException;
import hr.tvz.experimate.experimate.model.onboarding.exception.QuizRetakeLimitExceededException;
import hr.tvz.experimate.experimate.model.shared.event.QuizCompletedEvent;
import hr.tvz.experimate.experimate.model.user.User;
import hr.tvz.experimate.experimate.model.user.UserRepo;
import hr.tvz.experimate.experimate.model.user.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing the BFI-10 onboarding quiz lifecycle.
 *
 * <p>Handles question retrieval, status checks, quiz completion, cancellation,
 * and GDPR data deletion. Quiz completion scoring is delegated to {@link Big5Calculator}.
 */
@Service
public class OnboardingService {

    private static final int MAX_QUIZ_COMPLETIONS = 3;
    private static final String SUMMARY_PLACEHOLDER = "[AI personality summary not yet implemented]";
    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final QuizResultRepo quizResultRepo;
    private final UserRepo userRepo;
    private final Big5Calculator big5Calculator;
    private final AiMatchingService aiMatchingService;
    private final ApplicationEventPublisher publisher;

    public OnboardingService(QuizResultRepo quizResultRepo,
                             UserRepo userRepo,
                             Big5Calculator big5Calculator,
                             AiMatchingService aiMatchingService,
                             ApplicationEventPublisher publisher) {
        this.quizResultRepo = quizResultRepo;
        this.userRepo = userRepo;
        this.big5Calculator = big5Calculator;
        this.aiMatchingService = aiMatchingService;
        this.publisher = publisher;
    }

    /**
     * Returns the list of BFI-10 questions to display to the user.
     *
     * <p>Dimension and keying information are intentionally excluded from the response
     * to avoid influencing answers.
     *
     * @return ordered list of all 10 BFI-10 questions
     */
    public List<QuestionDto> getQuestions() {
        return Arrays.stream(Bfi10Question.values())
                .map(q -> new QuestionDto(q.getItemNumber(), q.getText()))
                .toList();
    }

    /**
     * Returns the current quiz status for the given user.
     *
     * @param userId the ID of the user
     * @return the user's current {@link QuizStatusResponse}
     * @throws QuizResultNotFoundException if no quiz result exists for the user
     */
    public QuizStatusResponse getStatus(Integer userId) {
        QuizResult quizResult = findQuizResultByUserId(userId);
        return new QuizStatusResponse(quizResult.getStatus(), quizResult.getCompletedAt());
    }

    /**
     * Cancels the user's quiz, transitioning it from AWAITING to CANCELLED.
     *
     * @param userId the ID of the user
     * @throws QuizResultNotFoundException if no quiz result exists for the user
     * @throws hr.tvz.experimate.experimate.model.onboarding.exception.QuizCannotBeCancelledException
     *                                     if the quiz is not in AWAITING state
     */
    @Transactional
    public void cancelQuiz(Integer userId) {
        QuizResult quizResult = findQuizResultByUserId(userId);
        quizResult.markCancelled();
        quizResultRepo.save(quizResult);
        log.info("Quiz cancelled for user {}", userId);
    }

    /**
     * Submits quiz answers, computes the Big Five personality vector, and marks the quiz as completed.
     *
     * <p>The answers list must contain exactly 10 Likert responses (1–5), ordered by item number.
     * Index 0 corresponds to item 1, index 9 to item 10.
     *
     * @param userId  the ID of the user submitting answers
     * @param answers list of 10 Likert responses (1–5), ordered by item number
     * @return the computed personality vector and an AI-generated summary (placeholder until Phase 1B)
     * @throws UserNotFoundException             if no user exists with the given ID
     * @throws QuizResultNotFoundException       if no quiz result exists for the user
     * @throws QuizRetakeLimitExceededException  if the user has exhausted the retake limit
     * @throws hr.tvz.experimate.experimate.model.onboarding.exception.QuizAlreadyCompletedException
     *                                           if the quiz is already completed
     */
    @Transactional
    public OnboardingCompletionResponse completeQuiz(Integer userId, List<Integer> answers) {
        User user = findUserById(userId);

        if (user.getQuizCompletionCount() >= MAX_QUIZ_COMPLETIONS)
            throw new QuizRetakeLimitExceededException(userId, MAX_QUIZ_COMPLETIONS);

        Map<Integer, Integer> answersMap = toAnswersMap(answers);
        Big5Vector vector = big5Calculator.compute(answersMap);

        user.setPersonalityMetricsFromVector(vector);
        user.incrementQuizCompletionCount();
        userRepo.save(user);

        QuizResult quizResult = findQuizResultByUserId(userId);
        quizResult.markCompleted(answersMap);
        quizResultRepo.save(quizResult);

        publisher.publishEvent(new QuizCompletedEvent(userId, vector));
        log.info("Quiz completed for user {}", userId);

        String summary = aiMatchingService.generatePersonalitySummary(vector)
                .orElse(SUMMARY_PLACEHOLDER);

        return new OnboardingCompletionResponse(toVectorDto(vector), summary);
    }

    /**
     * Clears all personality data for the given user and resets their quiz to AWAITING,
     * allowing them to retake it.
     *
     * <p>The quiz completion counter is preserved to enforce the retake limit.
     *
     * @param userId the ID of the user requesting data deletion
     * @throws UserNotFoundException       if no user exists with the given ID
     * @throws QuizResultNotFoundException if no quiz result exists for the user
     */
    @Transactional
    public void deleteQuizData(Integer userId) {
        User user = findUserById(userId);
        user.clearPersonalityData();
        userRepo.save(user);

        QuizResult quizResult = findQuizResultByUserId(userId);
        quizResult.reset();
        quizResultRepo.save(quizResult);

        log.info("Quiz data deleted for user {}", userId);
    }

    private User findUserById(Integer userId) {
        return userRepo.findById(userId).orElseThrow(() -> {
            log.warn("No user found for id {}", userId);
            return new UserNotFoundException(userId);
        });
    }

    private QuizResult findQuizResultByUserId(Integer userId) {
        return quizResultRepo.findByUser_Id(userId)
                .orElseThrow(() -> new QuizResultNotFoundException(userId));
    }

    private Map<Integer, Integer> toAnswersMap(List<Integer> answers) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < answers.size(); i++) {
            map.put(i + 1, answers.get(i));
        }
        return map;
    }

    private Big5VectorDto toVectorDto(Big5Vector vector) {
        return new Big5VectorDto(
                vector.openness(),
                vector.conscientiousness(),
                vector.extraversion(),
                vector.agreeableness(),
                vector.neuroticism()
        );
    }
}
