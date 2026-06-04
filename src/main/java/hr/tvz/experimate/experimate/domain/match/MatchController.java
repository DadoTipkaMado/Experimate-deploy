package hr.tvz.experimate.experimate.domain.match;

import hr.tvz.experimate.experimate.domain.match.response.MatchExplanationResponse;
import hr.tvz.experimate.experimate.domain.match.response.MatchResponse;
import hr.tvz.experimate.experimate.domain.user.Role;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import hr.tvz.experimate.experimate.shared.RateLimitOperation;
import hr.tvz.experimate.experimate.shared.RateLimiterService;
import hr.tvz.experimate.experimate.shared.exception.ForbiddenActionException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing the personality-based match pipeline.
 *
 * <p>All endpoints require authentication. The viewer's identity is resolved from
 * the JWT via {@link AppUserDetails}.
 */
@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;

    private final RateLimiterService rateLimiterService;

    public MatchController(MatchService matchService, RateLimiterService rateLimiterService) {
        this.matchService = matchService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Returns a list of active tour listings for the authenticated viewer sorted by Big5 compatibility.
     *
     * <p>When {@code q} is omitted, all candidate listings are returned sorted by personality score —
     * no AI is involved. When {@code q} is provided, the query is parsed by the AI into structured
     * search criteria used to filter candidates; this mode requires a premium subscription.
     *
     * @param q         optional natural-language AI search query; requires {@code PREMIUM_USER} role
     * @param principal the authenticated user
     * @return 200 with sorted match results
     * @throws ForbiddenActionException if {@code q} is provided and the user is not premium
     */
    @GetMapping
    public ResponseEntity<List<MatchResponse>> findMatches(
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AppUserDetails principal) {
        if (q != null && !q.isBlank() && principal.getRole() != Role.PREMIUM_USER) {
            throw new ForbiddenActionException("AI-powered search requires a premium subscription.");
        }
        if (q != null && !q.isBlank()) {
            rateLimiterService.consume(RateLimitOperation.AI_SEARCH, principal.getId());
        }
        return ResponseEntity.ok(matchService.findMatches(principal.getId(), q));
    }

    /**
     * Returns a natural-language explanation of why the viewer and the given candidate are compatible.
     * Requires a premium subscription — this endpoint always invokes the AI.
     *
     * <p>If {@code q} is provided (the same query used in the search that surfaced this candidate),
     * bio keywords are extracted and passed to the AI to enrich the explanation.
     *
     * @param candidateId ID of the candidate to explain
     * @param q           optional natural-language search query used to find this match
     * @param principal   the authenticated user
     * @return 200 with the AI-generated compatibility explanation
     */
    @PreAuthorize("hasRole('PREMIUM_USER')")
    @GetMapping("/{candidateId}/explain")
    public ResponseEntity<MatchExplanationResponse> explainMatch(
            @PathVariable Integer candidateId,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AppUserDetails principal) {
        rateLimiterService.consume(RateLimitOperation.AI_EXPLAIN, principal.getId());
        return ResponseEntity.ok(matchService.explainMatch(principal.getId(), candidateId, q));
    }
}
