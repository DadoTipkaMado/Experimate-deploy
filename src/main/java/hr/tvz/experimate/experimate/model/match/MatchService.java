package hr.tvz.experimate.experimate.model.match;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.tvz.experimate.experimate.model.ai.AiMatchingService;
import hr.tvz.experimate.experimate.model.ai.SearchCriteria;
import hr.tvz.experimate.experimate.model.onboarding.Big5Calculator;
import hr.tvz.experimate.experimate.model.onboarding.Big5Vector;
import hr.tvz.experimate.experimate.model.onboarding.PersonalityDimension;
import hr.tvz.experimate.experimate.model.onboarding.exception.IllegalOnboardingStateException;
import hr.tvz.experimate.experimate.model.tour_listing.TourListing;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingRepo;
import hr.tvz.experimate.experimate.user.User;
import hr.tvz.experimate.experimate.user.UserRepo;
import hr.tvz.experimate.experimate.user.exception.UserNotFoundException;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orchestrates the personality-based match pipeline.
 *
 * <p>Search operates on {@link TourListing} entities — listings are the primary result,
 * not users. The hosting user's personality metrics are used only for scoring and sorting.
 *
 * <p>For a given viewer, fetches all active candidate listings, applies optional search
 * criteria parsed from a natural-language query, scores each listing's host using weighted
 * cosine similarity, and returns results sorted by:
 * <ol>
 *   <li>Host onboarding status — onboarded hosts first.</li>
 *   <li>Compatibility score descending — within the onboarded tier, best personality matches first.</li>
 *   <li>Bio score descending — as a tiebreaker, listings whose host bio matches more of the
 *       viewer's search terms rank higher. Bio score is internal and not exposed in the response.</li>
 * </ol>
 *
 * <p>If the viewer has not completed onboarding, all compatibility scores are {@code null}
 * and results are sorted by host onboarding status only.
 */
@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final double TRAIT_SCORE_THRESHOLD = 0.0;

    private final UserRepo userRepo;
    private final TourListingRepo tourListingRepo;
    private final Big5Calculator big5Calculator;
    private final AiMatchingService aiMatchingService;

    public MatchService(UserRepo userRepo,
                        TourListingRepo tourListingRepo,
                        Big5Calculator big5Calculator,
                        AiMatchingService aiMatchingService) {
        this.userRepo = userRepo;
        this.tourListingRepo = tourListingRepo;
        this.big5Calculator = big5Calculator;
        this.aiMatchingService = aiMatchingService;
    }

    /**
     * Returns a list of active tour listings for the given viewer, optionally filtered
     * by a natural-language search query. Results are sorted by host onboarding status
     * (onboarded first), then by compatibility score descending, then by bio score descending.
     *
     * <p>If {@code query} is blank or {@code null}, all active candidate listings are returned.
     * If the AI fails to parse the query, matching falls back to unfiltered results.
     * If the viewer has not completed onboarding, all compatibility scores are {@code null}.
     *
     * <p>Scores are pre-computed before sorting so that sort order and displayed score
     * are always consistent.
     *
     * @param viewerId ID of the requesting user
     * @param query    optional natural-language search query
     * @return scored and sorted match results
     */
    @Transactional(readOnly = true)
    public List<MatchResponse> findMatches(Integer viewerId, String query) {
        User viewer = userRepo.findById(viewerId).orElseThrow(() -> new UserNotFoundException(viewerId));
        List<TourListing> candidates = tourListingRepo.findMatchCandidateListings(viewerId, LocalDateTime.now());
        log.debug("findMatches — viewerId={}, candidateListings={}, query=\"{}\"", viewerId, candidates.size(), query);

        List<String> bioKeywords = Collections.emptyList();
        if (query != null && !query.isBlank()) {
            Optional<SearchCriteria> criteriaOpt = parseSearchQuery(query);
            if (criteriaOpt.isPresent()) {
                candidates = filterByCriteria(candidates, criteriaOpt.get());
                bioKeywords = criteriaOpt.get().bioKeywords() != null ? criteriaOpt.get().bioKeywords() : Collections.emptyList();
                log.debug("findMatches — {} listings after search filter, bioKeywords={}", candidates.size(), bioKeywords);
            }
        }

        Big5Vector viewerVector = Big5Vector.fromUser(viewer);
        final List<String> finalBioKeywords = bioKeywords;

        Map<Integer, Integer> compatibilityScores = buildCompatibilityScoreMap(candidates, viewerVector);
        Map<Integer, Integer> bioScores = buildBioScoreMap(candidates, finalBioKeywords);

        return candidates.stream()
                .sorted(buildMatchComparator(compatibilityScores, bioScores))
                .map(listing -> toMatchResponse(listing, compatibilityScores.get(listing.getId())))
                .toList();
    }

    /**
     * Generates a natural-language explanation of why the viewer and the candidate are compatible.
     * Both users must have completed onboarding; if either has not,
     * an {@link IllegalOnboardingStateException} is thrown.
     *
     * <p>If {@code query} is provided, bio keywords extracted from it are passed to the AI
     * to enrich the explanation — e.g. "you searched for a hiker, and Ana's profile shows exactly that."
     *
     * @param viewerId    ID of the requesting user
     * @param candidateId ID of the candidate to explain
     * @param query       optional natural-language search query used to find this match
     * @return AI-generated compatibility explanation
     * @throws IllegalOnboardingStateException if either user has not completed onboarding
     */
    @Transactional(readOnly = true)
    public MatchExplanationResponse explainMatch(Integer viewerId, Integer candidateId, String query) {
        User viewer = userRepo.findById(viewerId).orElseThrow(() -> new UserNotFoundException(viewerId));
        User candidate = userRepo.findById(candidateId).orElseThrow(() -> new UserNotFoundException(candidateId));

        if (!viewer.isOnboardingCompleted() || !candidate.isOnboardingCompleted()) {
            throw new IllegalOnboardingStateException(
                    "Both users must complete onboarding before a compatibility explanation can be generated.");
        }

        String explanation = aiMatchingService
                .explainCompatibility(
                        Big5Vector.fromUser(viewer), viewer.getBio(),
                        Big5Vector.fromUser(candidate), candidate.getBio(),
                        extractBioKeywordsFromQuery(query)
                )
                .orElse("Compatibility explanation unavailable at this time.");

        return new MatchExplanationResponse(explanation);
    }

    // --- query parsing ---

    /**
     * Calls the AI to parse a natural-language query into structured {@link SearchCriteria}.
     * Returns empty if the AI call fails or the response cannot be deserialized.
     */
    private Optional<SearchCriteria> parseSearchQuery(String query) {
        return aiMatchingService.interpretSearchQuery(query)
                .flatMap(json -> {
                    try {
                        return Optional.of(parseCriteria(json));
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to parse search criteria JSON, falling back to unfiltered: {}", e.getMessage());
                        return Optional.empty();
                    }
                });
    }

    /**
     * Parses the JSON string returned by {@link AiMatchingService#interpretSearchQuery}
     * into a {@link SearchCriteria} object.
     */
    private SearchCriteria parseCriteria(@NotBlank(message = "Search json cannot be blank or null.") String json)
            throws JsonProcessingException {
        return objectMapper.readValue(json, SearchCriteria.class);
    }

    /**
     * Extracts bio keywords from an optional search query by parsing it through the AI.
     * Returns an empty list if the query is blank, the AI fails, or the criteria contain no bio keywords.
     */
    private List<String> extractBioKeywordsFromQuery(String query) {
        if (query == null || query.isBlank()) return Collections.emptyList();
        return parseSearchQuery(query)
                .map(c -> c.bioKeywords() != null ? c.bioKeywords() : Collections.<String>emptyList())
                .orElse(Collections.emptyList());
    }

    // --- search filtering ---

    /**
     * Filters the candidate list to only those listings that satisfy the given search criteria.
     */
    private List<TourListing> filterByCriteria(List<TourListing> candidates, SearchCriteria criteria) {
        return candidates.stream()
                .filter(l -> matchesCriteria(l, criteria))
                .toList();
    }

    /**
     * Returns {@code true} if the listing passes all applicable search filters.
     *
     * <p>Delegates to three independent sub-filters:
     * trait preferences (hard, onboarded hosts only),
     * negative keyword exclusion (hard), and positive term matching (soft).
     */
    private boolean matchesCriteria(TourListing listing, SearchCriteria criteria) {
        if (listing.getHost().isOnboardingCompleted() && !passesTraitFilter(listing.getHost(), criteria)) return false;
        if (!passesNegativeKeywordFilter(listing, criteria)) return false;
        if (!passesPositiveTermsFilter(listing, criteria)) return false;
        return true;
    }

    /**
     * Returns {@code true} if the host's personality traits satisfy the trait preferences
     * in the given criteria. Only called when the host has completed onboarding.
     */
    private boolean passesTraitFilter(User host, SearchCriteria criteria) {
        Map<PersonalityDimension, String> traitPrefs = criteria.traitPreferences()
                .entrySet().stream()
                .filter(e -> PersonalityDimension.fromDisplayname(e.getKey()) != null)
                .collect(Collectors.toMap(
                        e -> PersonalityDimension.fromDisplayname(e.getKey()),
                        Map.Entry::getValue
                ));
        Map<PersonalityDimension, Double> hostTraits = Map.of(
                PersonalityDimension.OPENNESS,          host.getPersonalityOpenness(),
                PersonalityDimension.CONSCIENTIOUSNESS, host.getPersonalityConscientiousness(),
                PersonalityDimension.EXTRAVERSION,      host.getPersonalityExtraversion(),
                PersonalityDimension.AGREEABLENESS,     host.getPersonalityAgreeableness(),
                PersonalityDimension.NEUROTICISM,       host.getPersonalityNeuroticism()
        );
        return traitPrefsMatch(hostTraits, traitPrefs);
    }

    /**
     * Returns {@code true} if none of the negative keywords appear in the listing's city
     * or tour description. Returns {@code true} when no negative keywords are specified.
     */
    private boolean passesNegativeKeywordFilter(TourListing listing, SearchCriteria criteria) {
        List<String> negativeKeywords = criteria.negativeKeywords() != null
                ? criteria.negativeKeywords()
                : Collections.emptyList();
        return negativeKeywords.isEmpty() || !listingFieldsMatch(listing, negativeKeywords);
    }

    /**
     * Returns {@code true} if the listing matches at least one positive keyword in its domain.
     * {@code keywords} match against listing city and description; {@code bioKeywords} match
     * against host bio only. A match in either domain satisfies the filter.
     * Returns {@code true} when no positive terms are specified.
     */
    private boolean passesPositiveTermsFilter(TourListing listing, SearchCriteria criteria) {
        List<String> keywords = criteria.keywords() != null ? criteria.keywords() : Collections.emptyList();
        List<String> bioKeywords = criteria.bioKeywords() != null ? criteria.bioKeywords() : Collections.emptyList();
        if (keywords.isEmpty() && bioKeywords.isEmpty()) return true;

        boolean listingMatch = !keywords.isEmpty() && listingFieldsMatch(listing, keywords);
        boolean bioMatch = !bioKeywords.isEmpty() && hostBioMatch(listing.getHost(), bioKeywords);
        return listingMatch || bioMatch;
    }

    // --- text matching ---

    /**
     * Returns {@code true} if any of the given terms appear in the listing's city or
     * tour description (case-insensitive substring match).
     */
    private boolean listingFieldsMatch(TourListing listing, List<String> terms) {
        String city = listing.getCity().toLowerCase();
        String description = listing.getTourDescription().toLowerCase();
        return terms.stream()
                .map(String::toLowerCase)
                .anyMatch(t -> city.contains(t) || description.contains(t));
    }

    /**
     * Returns {@code true} if any of the given terms appear in the host's bio
     * (case-insensitive substring match). Returns {@code false} if bio is {@code null}.
     */
    private boolean hostBioMatch(User host, List<String> terms) {
        if (host.getBio() == null) return false;
        String bio = host.getBio().toLowerCase();
        return terms.stream().map(String::toLowerCase).anyMatch(bio::contains);
    }

    private boolean traitPrefsMatch(Map<PersonalityDimension, Double> hostTraits,
                                    Map<PersonalityDimension, String> traitPrefs) {
        for (Map.Entry<PersonalityDimension, String> pref : traitPrefs.entrySet()) {
            String prefValue = pref.getValue();
            if (prefValue.equalsIgnoreCase("any")) continue;
            boolean isHigh = hostTraits.get(pref.getKey()) > TRAIT_SCORE_THRESHOLD;
            if (prefValue.equalsIgnoreCase("high") && !isHigh) return false;
            if (prefValue.equalsIgnoreCase("low") && isHigh) return false;
        }
        return true;
    }

    // --- scoring ---

    /**
     * Pre-computes weighted cosine compatibility scores for all candidates.
     * Storing in a map ensures sort order and displayed scores are always consistent.
     *
     * @return map of listing ID to score in [0, 100], or {@code null} if either party lacks a vector
     */
    private Map<Integer, Integer> buildCompatibilityScoreMap(List<TourListing> candidates, Big5Vector viewerVector) {
        Map<Integer, Integer> scores = new HashMap<>();
        for (TourListing l : candidates) {
            scores.put(l.getId(), computeMatchScore(viewerVector, Big5Vector.fromUser(l.getHost())));
        }
        return scores;
    }

    /**
     * Pre-computes bio scores for all candidates. Bio score is the count of bio keywords
     * from the viewer's search query that appear in the host's bio. Used as a sort tiebreaker
     * and not exposed in the API response.
     *
     * @return map of listing ID to number of matched bio keywords (0 when bio is absent or no query)
     */
    private Map<Integer, Integer> buildBioScoreMap(List<TourListing> candidates, List<String> bioKeywords) {
        Map<Integer, Integer> scores = new HashMap<>();
        for (TourListing l : candidates) {
            scores.put(l.getId(), computeBioScore(l.getHost(), bioKeywords));
        }
        return scores;
    }

    /**
     * Returns a comparator that sorts listings by:
     * <ol>
     *   <li>Host onboarding completed (onboarded first)</li>
     *   <li>Compatibility score descending (nulls last)</li>
     *   <li>Bio score descending (tiebreaker)</li>
     * </ol>
     */
    private Comparator<TourListing> buildMatchComparator(Map<Integer, Integer> compatibilityScores,
                                                          Map<Integer, Integer> bioScores) {
        return Comparator
                .<TourListing>comparingInt(l -> l.getHost().isOnboardingCompleted() ? 0 : 1)
                .thenComparing(Comparator.<TourListing, Integer>comparing(
                        l -> compatibilityScores.get(l.getId()),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .thenComparingInt(l -> -bioScores.getOrDefault(l.getId(), 0));
    }

    /**
     * Computes the weighted cosine similarity between two personality vectors.
     *
     * @param viewerVector    the viewer's personality vector, or {@code null} if not onboarded
     * @param candidateVector the candidate's personality vector, or {@code null} if not onboarded
     * @return score in [0, 100], or {@code null} if either vector is {@code null}
     */
    private Integer computeMatchScore(Big5Vector viewerVector, Big5Vector candidateVector) {
        if (viewerVector == null || candidateVector == null) return null;
        return big5Calculator.weightedCosineSimilarity(viewerVector, candidateVector);
    }

    /**
     * Counts how many of the given bio keywords appear in the host's bio (case-insensitive).
     * Returns 0 if the bio is {@code null} or no keywords are provided.
     */
    private int computeBioScore(User host, List<String> bioKeywords) {
        if (host.getBio() == null || bioKeywords.isEmpty()) return 0;
        String bio = host.getBio().toLowerCase();
        return (int) bioKeywords.stream().map(String::toLowerCase).filter(bio::contains).count();
    }

    // --- response building ---

    /**
     * Builds a {@link MatchResponse} for the given listing using a pre-computed compatibility score.
     */
    private MatchResponse toMatchResponse(TourListing listing, Integer compatibilityScore) {
        User candidate = listing.getHost();
        return new MatchResponse(
                candidate.getId(),
                candidate.getUsername(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getBio(),
                buildProfilePhotoUrl(candidate),
                compatibilityScore,
                buildListingInfo(listing)
        );
    }

    /**
     * Builds an {@link ActiveListingInfo} summary from the given listing,
     * truncating the description to 200 characters if needed.
     */
    private ActiveListingInfo buildListingInfo(TourListing listing) {
        String description = listing.getTourDescription();
        String snippet = description != null
                ? description.substring(0, Math.min(200, description.length()))
                : null;
        return new ActiveListingInfo(listing.getId(), listing.getCity(), listing.getMeetingDate(), snippet);
    }

    /**
     * Returns the profile photo URL for the given user, or {@code null} if no photo is set.
     */
    private String buildProfilePhotoUrl(User user) {
        return user.getProfilePhotoFilename() != null
                ? "/api/user/profile-photo/" + user.getProfilePhotoFilename()
                : null;
    }
}
