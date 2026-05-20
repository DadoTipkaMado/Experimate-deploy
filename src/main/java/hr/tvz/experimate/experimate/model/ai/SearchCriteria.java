package hr.tvz.experimate.experimate.model.ai;

import java.util.List;
import java.util.Map;

/**
 * Structured matching criteria parsed from a natural-language search query
 * by {@link AiMatchingService#interpretSearchQuery(String)}.
 *
 * <p>Produced by deserializing the JSON string returned by the AI. Field names
 * match the JSON schema keys exactly so Jackson can deserialize without annotations.
 *
 * @param keywords         activity/interest terms to match against tour listing descriptions
 * @param negativeKeywords things to exclude, detected from "no", "not", "without" in the query
 * @param bioKeywords      person-quality terms to match against user bios
 * @param traitPreferences map of trait name → "high" | "low" | "any" for personality filtering
 */
public record SearchCriteria(
        List<String> keywords,
        List<String> negativeKeywords,
        List<String> bioKeywords,
        Map<String, String> traitPreferences
) {}
