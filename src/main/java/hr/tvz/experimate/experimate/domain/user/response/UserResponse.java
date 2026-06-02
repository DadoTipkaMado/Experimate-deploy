package hr.tvz.experimate.experimate.domain.user.response;

import java.util.List;

public record UserResponse(
        Integer id,
        String username,
        String role,
        String firstName,
        String lastName,
        String bio,
        double rating,
        String profilePhotoUrl,
        String personalitySummary,
        boolean onboardingCompleted,
        List<String> personalityTraits
) {
}
