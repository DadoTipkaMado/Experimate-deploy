package hr.tvz.experimate.experimate.model.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.tvz.experimate.experimate.model.onboarding.Big5Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Service for all AI-powered features in ExperiMate.
 *
 * <p>Uses a {@link ChatClient} pre-configured in {@code AiConfig} with the application-wide
 * system prompt loaded from {@code resources/ai/system-prompt.txt}. Each method adds
 * task-specific instructions on top of that global context via the user message.
 */
@Service
public class AiMatchingService {

    private static final Logger log = LoggerFactory.getLogger(AiMatchingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> TRAIT_KEYS = Set.of("curiosity", "focus", "energy", "warmth", "sensitivity");
    private static final Set<String> VALID_TRAIT_VALUES = Set.of("high", "low", "any");

    private final ChatClient chatClient;

    public AiMatchingService(ChatClient chatClient) {
        this.chatClient = chatClient;
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

        String prompt = """
        TASK: Write a short personality summary for a user based on their trait scores.
        
        STYLE — "character card" feel:
        - Imagine the kind of evocative one-paragraph description you'd see introducing
          a character in a thoughtful video game (Hades, Stardew Valley, Disco Elysium)
          — grounded in real human qualities, never fantasy or supernatural.
        - Trait-led phrasing is encouraged: "Quiet energy, curious mind, warm presence."
        - Hint at how this person shows up in the world, not what they ARE in some
          abstract sense.
        - Short bridge clauses are fine, even encouraged: dashes, em-dashes, comma
          chains. The rhythm should feel like a character card, not a paragraph essay.
        
        FORMAT: 1-2 sentences, max 35 words. Warm, celebratory, slightly playful.
                No preamble, no trailing commentary.
        
        BOUNDARIES:
        - Never use these trait names: Openness, Conscientiousness, Extraversion,
          Agreeableness, Neuroticism. Use only: %s.
        - Never use fantasy vocabulary: hero, warrior, sage, wanderer, realm, quest,
          power, ability, class, level. This is a real person, described stylishly —
          not a character in a fantasy game.
        - Never frame any trait as a weakness. Low Energy is "quiet energy", not
          "limited social capacity".
        - Never reference scores, numbers, or "the algorithm".
        
        EXAMPLES of the right tone:
        - "Quiet energy, sharp curiosity — the kind of person who asks one question
          that reframes the whole conversation."
        - "Warm, grounded, hard to ruffle — you're the friend people text when their
          plans fall apart and they need someone steady."
        - "Bright spark of a person — high energy, easy to laugh, allergic to dull
          conversation."
        
        SCORES:
        %s
        """.formatted(TRAIT_KEYS, formatVector(vector));

        Optional<String> result = callAi(prompt, "generatePersonalitySummary");
        result.ifPresent(s -> log.debug("generatePersonalitySummary result: \"{}\"", s));
        return result;
    }

    /**
     * Generates a natural-language explanation of why two users are compatible as Mates.
     *
     * @param viewer       personality vector of the user viewing the match
     * @param viewerBio    bio of the viewing user, or {@code null} if not set
     * @param candidate    personality vector of the candidate Mate
     * @param candidateBio bio of the candidate Mate, or {@code null} if not set
     * @return a 2-3 sentence compatibility explanation, or empty if the AI call fails
     */
    public Optional<String> explainCompatibility(Big5Vector viewer, String viewerBio,
                                                  Big5Vector candidate, String candidateBio) {
        log.debug("explainCompatibility called — viewerBio={}, candidateBio={}",
                viewerBio != null && !viewerBio.isBlank() ? "present" : "absent",
                candidateBio != null && !candidateBio.isBlank() ? "present" : "absent");

        String prompt = """
            TASK: Write a one-sentence explanation of why these two people might enjoy
            spending a day together as Mates.
            
            CONTEXT: ExperiMate connects people for a single shared day — coffee, a walk,
            exploring a city. The connection is platonic and exploratory.
            
            FORMAT: One sentence, max 20 words. Reflective and warm, not promotional.
            Reference specifics from bios when available. Use only these trait names:
            %s. No preamble.
            
            RULES:
            - Translate personality scores into human qualities. Never mention scores,
              numbers, or trait names directly in the output.
            - If a bio is missing, work only from personality. Do not invent biographical
              details, hobbies, or preferences.
            - If both bios are missing, lean fully on personality and keep it short.
            - Match the language of whichever bio is present. If both are empty,
              default to English.
            
            EXAMPLES of the right tone:
            - "Ana shares your reflective pace — and her love of bookshops mirrors
              your quiet curiosity."
            - "Both of you are easy company in unfamiliar places — warm, slow, open
              to wherever the day goes."
            - "Ivan's grounded warmth balances your spark of energy."
            
            PERSON A:
            %s
            PERSON B:
            %s
            """.formatted(TRAIT_KEYS,
                formatPerson(viewer, viewerBio),
                formatPerson(candidate, candidateBio));

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

        String prompt = """
            TASK: Parse a search query into structured matching criteria for finding
            compatible Mates for a shared day.
            
            FORMAT: Strict JSON. Begin with `{`, end with `}`. No code fences, no preamble.
            
            SCHEMA:
            {
              "keywords": ["<activities, interests, locations — match against listings>"],
              "negativeKeywords": ["<things to avoid — detect 'not', 'no', 'without'>"],
              "bioKeywords": ["<person qualities/passions — match against bios>"],
              "traitPreferences": {
                "curiosity":   "high" | "low" | "any",
                "focus":       "high" | "low" | "any",
                "energy":      "high" | "low" | "any",
                "warmth":      "high" | "low" | "any",
                "sensitivity": "high" | "low" | "any"
              }
            }
            
            RULES:
            - keywords vs bioKeywords: "hiker" goes in bioKeywords, "hiking trail" in keywords.
            - traitPreferences: derive from descriptive language. Default "any" when unclear.
              "adventurous" → energy: high, curiosity: high
              "chill" → energy: low
              "warm" → warmth: high
            - Context: if user describes THEMSELVES then says what they're LOOKING FOR,
              traitPreferences reflect what they're looking for, not what they are.
            
            EXAMPLES:
            
            Query: "chill coffee spots in Zagreb, no crowds"
            {
              "keywords": ["coffee", "Zagreb"],
              "negativeKeywords": ["crowds"],
              "bioKeywords": ["chill", "calm"],
              "traitPreferences": {"curiosity":"any","focus":"any","energy":"low","warmth":"any","sensitivity":"any"}
            }
            
            Query: "I'm adventurous, looking for someone calm to balance me"
            {
              "keywords": [],
              "negativeKeywords": [],
              "bioKeywords": ["calm", "grounded"],
              "traitPreferences": {"curiosity":"any","focus":"any","energy":"low","warmth":"high","sensitivity":"any"}
            }
            
            Query: "passionate hiker, deep conversations, not into nightlife"
            {
              "keywords": ["hiking", "outdoors"],
              "negativeKeywords": ["nightlife", "bars"],
              "bioKeywords": ["hiker", "passionate", "thoughtful"],
              "traitPreferences": {"curiosity":"high","focus":"any","energy":"any","warmth":"any","sensitivity":"any"}
            }
            
            QUERY:
            %s
            """.formatted(query);

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
