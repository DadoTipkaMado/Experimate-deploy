package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.match.MatchExplanationResponse;
import hr.tvz.experimate.experimate.model.match.MatchResponse;
import hr.tvz.experimate.experimate.model.match.MatchService;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    /**
     * Returns a list of compatible users for the authenticated viewer, sorted by
     * compatibility score descending.
     *
     * <p>If {@code q} is provided, results are filtered using natural-language
     * search criteria parsed by the AI. If omitted, all compatible users are returned.
     *
     * @param q         optional natural-language search query
     * @param principal the authenticated user
     * @return 200 with sorted match results
     */
    @GetMapping
    public ResponseEntity<List<MatchResponse>> findMatches(
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.ok(matchService.findMatches(principal.getId(), q));
    }

    /**
     * Returns a natural-language explanation of why the viewer and the given candidate are compatible.
     *
     * <p>If {@code q} is provided (the same query used in the search that surfaced this candidate),
     * bio keywords are extracted and passed to the AI to enrich the explanation.
     *
     * @param candidateId ID of the candidate to explain
     * @param q           optional natural-language search query used to find this match
     * @param principal   the authenticated user
     * @return 200 with the AI-generated compatibility explanation
     */
    @GetMapping("/{candidateId}/explain")
    public ResponseEntity<MatchExplanationResponse> explainMatch(
            @PathVariable Integer candidateId,
            @RequestParam(required = false) String q,
            @AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.ok(matchService.explainMatch(principal.getId(), candidateId, q));
    }
}
