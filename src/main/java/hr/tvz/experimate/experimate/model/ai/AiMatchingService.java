package hr.tvz.experimate.experimate.model.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.tvz.experimate.experimate.model.onboarding.Big5Vector;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for all AI-powered features in ExperiMate.
 *
 * <p>Uses a {@link ChatClient} pre-configured in {@code AiConfig} with the application-wide
 * system prompt loaded from {@code resources/ai/system-prompt.txt}. Each method adds
 * task-specific instructions on top of that global context via the user message.
 *
 * <p>Prompt templates are loaded from {@code resources/ai/prompts/} at startup via
 * {@link PostConstruct}. Dynamic values are substituted at call time using
 * {@link String#formatted}.
 */
@Service
public class AiMatchingService {

    private static final Logger log = LoggerFactory.getLogger(AiMatchingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> TRAIT_KEYS = Set.of("curiosity", "focus", "energy", "warmth", "sensitivity");
    private static final Set<String> VALID_TRAIT_VALUES = Set.of("high", "low", "any");

    @Value("classpath:ai/prompts/personality-summary.txt")
    private Resource personalitySummaryPromptResource;

    @Value("classpath:ai/prompts/explain-compatibility.txt")
    private Resource explainCompatibilityPromptResource;

    @Value("classpath:ai/prompts/interpret-search-query.txt")
    private Resource interpretSearchQueryPromptResource;

    private String personalitySummaryPrompt;
    private String explainCompatibilityPrompt;
    private String interpretSearchQueryPrompt;

    private final ChatClient chatClient;

    public AiMatchingService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Loads all prompt templates from the classpath at application startup.
     *
     * @throws IllegalStateException if any prompt file cannot be read
     */
    @PostConstruct
    private void loadPrompts() {
        try {
            personalitySummaryPrompt = personalitySummaryPromptResource.getContentAsString(StandardCharsets.UTF_8);
            explainCompatibilityPrompt = explainCompatibilityPromptResource.getContentAsString(StandardCharsets.UTF_8);
            interpretSearchQueryPrompt = interpretSearchQueryPromptResource.getContentAsString(StandardCharsets.UTF_8);
            log.debug("AI prompt templates loaded successfully");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load AI prompt templates from classpath", e);
        }
    }

    /**
     * Generates a short natural-language summary of a user's personality profile.
     *
     * @param vector the user's computed Big Five personality vector
     * @return a 2-3 sentence personality summary, or empty if the AI call fails
     */
    public Optional<String> generatePersonalitySummary(Big5Vector vector) {
        log.debug("generatePersonalitySummary called — scores: [{}]",
                formatVector(vector).strip().replace("\n", ", "));

        String prompt = personalitySummaryPrompt.formatted(TRAIT_KEYS, formatVector(vector));

        Optional<String> result = callAi(prompt, "generatePersonalitySummary");
        result.ifPresent(s -> log.debug("generatePersonalitySummary result: \"{}\"", s));
        return result;
    }

    /**
     * Generates a natural-language explanation of why two users are compatible as Mates.
     *
     * <p>If {@code searchBioKeywords} is non-empty, the prompt instructs the AI to
     * explicitly reference bio terms that matched what the viewer searched for
     * (e.g. "you were looking for a hiker — Ana's profile shows exactly that").
     *
     * @param viewer            personality vector of the user viewing the match
     * @param viewerBio         bio of the viewing user, or {@code null} if not set
     * @param candidate         personality vector of the candidate Mate
     * @param candidateBio      bio of the candidate Mate, or {@code null} if not set
     * @param searchBioKeywords bio-related terms extracted from the viewer's search query,
     *                          or an empty list if no query was provided
     * @return a 2-3 sentence compatibility explanation, or empty if the AI call fails
     */
    public Optional<String> explainCompatibility(Big5Vector viewer, String viewerBio,
                                                  Big5Vector candidate, String candidateBio,
                                                  List<String> searchBioKeywords) {
        log.debug("explainCompatibility called — viewerBio={}, candidateBio={}, searchBioKeywords={}",
                viewerBio != null && !viewerBio.isBlank() ? "present" : "absent",
                candidateBio != null && !candidateBio.isBlank() ? "present" : "absent",
                searchBioKeywords);

        String searchContextSection = (searchBioKeywords != null && !searchBioKeywords.isEmpty())
                ? "\nSEARCH CONTEXT:\nThe viewer searched for someone described as: "
                        + String.join(", ", searchBioKeywords) + ".\n"
                        + "If Person B's bio matches any of these qualities, reference it explicitly "
                        + "(e.g. \"you were looking for a hiker — and Ana's profile shows exactly that\").\n"
                : "";

        String prompt = explainCompatibilityPrompt.formatted(TRAIT_KEYS, searchContextSection,
                formatPerson(viewer, viewerBio), formatPerson(candidate, candidateBio));

        Optional<String> result = callAi(prompt, "explainCompatibility");
        result.ifPresent(s -> log.debug("explainCompatibility result: \"{}\"", s));
        return result;
    }

    /**
     * Parses a natural-language search query into structured matching criteria.
     *
     * <p>Returns a JSON string with three fields: {@code keywords} for matching against
     * listing titles and descriptions, {@code traitPreferences} for filtering by personality
     * scores, and {@code bioKeywords} for matching against user bios.
     *
     * <p>If the AI returns a response that does not conform to the expected schema,
     * the result is discarded and {@link Optional#empty()} is returned, preventing
     * malformed data from reaching the search pipeline.
     *
     * @param query the raw natural-language search query entered by the user
     * @return a validated JSON string with parsed criteria, or empty if the AI call fails or returns invalid output
     */
    public Optional<String> interpretSearchQuery(String query) {
        log.debug("interpretSearchQuery called — query: \"{}\"", query);

        String prompt = interpretSearchQueryPrompt.formatted(query);

        Optional<String> result = callAi(prompt, "interpretSearchQuery");
        if (result.isPresent() && !isValidSearchJson(result.get())) {
            log.warn("interpretSearchQuery discarding invalid JSON. Raw response: {}", result.get());
            return Optional.empty();
        }
        result.ifPresent(s -> log.debug("interpretSearchQuery parsed criteria: {}", s));
        return result;
    }

    private Optional<String> callAi(String userMessage, String methodName) {
        log.debug("Calling Claude API [{}]", methodName);
        try {
            // .system() is intentionally omitted — the global system prompt from
            // AiConfig (resources/ai/system-prompt.txt) is applied automatically as defaultSystem.
            String result = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();
            log.debug("Claude API [{}] responded ({} chars)", methodName,
                    result != null ? result.length() : 0);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.warn("Claude API [{}] failed: {}", methodName, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isValidSearchJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.has("keywords") || !root.get("keywords").isArray()) return false;
            if (!root.has("bioKeywords") || !root.get("bioKeywords").isArray()) return false;
            if (!root.has("traitPreferences") || !root.get("traitPreferences").isObject()) return false;

            JsonNode traits = root.get("traitPreferences");
            for (String key : TRAIT_KEYS) {
                if (!traits.has(key)) return false;
                if (!VALID_TRAIT_VALUES.contains(traits.get(key).asText())) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String formatVector(Big5Vector v) {
        return ("Curiosity (Openness): %+.2f\n" +
                "Focus (Conscientiousness): %+.2f\n" +
                "Energy (Extraversion): %+.2f\n" +
                "Warmth (Agreeableness): %+.2f\n" +
                "Sensitivity (Neuroticism): %+.2f\n")
                .formatted(v.openness(), v.conscientiousness(),
                        v.extraversion(), v.agreeableness(), v.neuroticism());
    }

    private String formatPerson(Big5Vector v, String bio) {
        String bioLine = (bio != null && !bio.isBlank()) ? "Bio: " + bio + "\n" : "";
        return bioLine + formatVector(v);
    }
}
