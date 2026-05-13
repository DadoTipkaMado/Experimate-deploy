package hr.tvz.experimate.experimate.shared.event;

import hr.tvz.experimate.experimate.domain.onboarding.Big5Vector;

/**
 * Published when a user successfully completes the onboarding quiz and their
 * Big Five personality vector has been computed and stored.
 *
 * @param userId the ID of the user who completed the quiz
 * @param vector the computed {@link Big5Vector} for that user
 */
public record QuizCompletedEvent(Integer userId, Big5Vector vector) {}
