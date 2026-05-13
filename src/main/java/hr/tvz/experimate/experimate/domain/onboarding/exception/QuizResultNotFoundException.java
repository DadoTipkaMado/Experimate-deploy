package hr.tvz.experimate.experimate.domain.onboarding.exception;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

/**
 * Thrown when no {@link hr.tvz.experimate.experimate.domain.onboarding.QuizResult}
 * exists for the given user.
 *
 * <p>Extends {@link NotFoundException}, which is globally mapped to HTTP 404 Not Found.
 */
public class QuizResultNotFoundException extends NotFoundException {

    public QuizResultNotFoundException(Integer userId) {
        super("Quiz result not found for user " + userId);
    }
}
