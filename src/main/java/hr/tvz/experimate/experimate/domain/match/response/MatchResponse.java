package hr.tvz.experimate.experimate.domain.match.response;

/**
 * Response DTO for a single personality match result.
 *
 * @param userId             ID of the matched user
 * @param username           username of the matched user
 * @param firstName          first name
 * @param lastName           last name
 * @param bio                bio, or {@code null} if not set
 * @param profilePhotoUrl    profile photo URL, or {@code null} if not set
 * @param compatibilityScore weighted cosine similarity score in [0, 100], or {@code null} if
 *                           either user has not completed onboarding
 * @param activeListing      the candidate's tour listing that produced this match result
 */
public record MatchResponse(
        Integer userId,
        String username,
        String firstName,
        String lastName,
        String bio,
        String profilePhotoUrl,
        Integer compatibilityScore,
        ActiveListingInfo activeListing
) {}
