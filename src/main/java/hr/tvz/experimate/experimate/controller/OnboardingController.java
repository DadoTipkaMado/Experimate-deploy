package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.onboarding.OnboardingService;
import hr.tvz.experimate.experimate.model.onboarding.dto.*;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the BFI-10 onboarding quiz.
 *
 * <p>All endpoints require authentication. The acting user's ID is resolved
 * from the JWT via {@link AppUserDetails} — no caller ID is passed in the request body.
 */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    /**
     * Returns the list of BFI-10 questions to display to the user.
     *
     * @return 200 with ordered list of all 10 questions
     */
    @GetMapping("/questions")
    public ResponseEntity<List<QuestionDto>> getQuestions() {
        return ResponseEntity.ok(onboardingService.getQuestions());
    }

    /**
     * Returns the current quiz status for the authenticated user.
     *
     * @param principal the authenticated user
     * @return 200 with the user's current quiz status
     */
    @GetMapping("/status")
    public ResponseEntity<QuizStatusResponse> getStatus(@AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.ok(onboardingService.getStatus(principal.getId()));
    }

    /**
     * Submits quiz answers and returns the computed Big Five personality vector.
     *
     * @param principal the authenticated user
     * @param dto       the submitted answers
     * @return 200 with the computed personality vector and summary
     */
    @PostMapping("/answers")
    public ResponseEntity<OnboardingCompletionResponse> completeQuiz(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @RequestBody QuizSubmitDto dto) {
        return ResponseEntity.ok(onboardingService.completeQuiz(principal.getId(), dto.answers()));
    }

    /**
     * Cancels the authenticated user's quiz.
     *
     * @param principal the authenticated user
     * @return 204 No Content
     */
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelQuiz(@AuthenticationPrincipal AppUserDetails principal) {
        onboardingService.cancelQuiz(principal.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes all quiz and personality data for the authenticated user (GDPR).
     *
     * @param principal the authenticated user
     * @return 204 No Content
     */
    @DeleteMapping("/data")
    public ResponseEntity<Void> deleteQuizData(@AuthenticationPrincipal AppUserDetails principal) {
        onboardingService.deleteQuizData(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
