package hr.tvz.experimate.experimate.model.ai;

import hr.tvz.experimate.experimate.model.onboarding.Big5Vector;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for all AI-powered features in ExperiMate.
 *
 * <p>Uses a {@link ChatClient} pre-configured with the application-wide system prompt.
 * All methods are stubs returning {@link Optional#empty()} until Phase 1B implementation.
 */
@Service
public class AiMatchingService {

    private final ChatClient chatClient;

    public AiMatchingService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Generates a short natural-language summary of a user's personality profile.
     *
     * @param vector the user's computed Big Five personality vector
     * @return a personality summary, or empty if generation fails
     */
    public Optional<String> generatePersonalitySummary(Big5Vector vector) {
        return Optional.empty();
    }

    /**
     * Generates a natural-language explanation of why two users are compatible.
     *
     * @param viewer    the personality vector of the user viewing the match
     * @param candidate the personality vector of the candidate Mate
     * @return a compatibility explanation, or empty if generation fails
     */
    public Optional<String> explainCompatibility(Big5Vector viewer, Big5Vector candidate) {
        return Optional.empty();
    }

    /**
     * Interprets a natural-language search query into structured matching criteria.
     *
     * @param query the raw search query entered by the user
     * @return a structured representation of the search intent, or empty if interpretation fails
     */
    public Optional<String> interpretSearchQuery(String query) {
        return Optional.empty();
    }
}
