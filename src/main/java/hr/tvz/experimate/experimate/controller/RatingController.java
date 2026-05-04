package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.rating.*;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rating")
@Validated
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(@Valid @RequestBody CreateRatingDto dto,
                                                        @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ratingService.createRating(dto, userDetails.getId())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<RatingResponse> getRatingById(@PathVariable @Positive Integer id) {
        return ratingService.getRatingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<RatingResponse>> getAllRatings() {
        return ResponseEntity.ok(ratingService.getAllRatings());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RatingResponse> updateRating(@PathVariable @Positive Integer id,
                                                        @Valid @RequestBody UpdateRatingDto dto,
                                                        @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(ratingService.updateRating(id, dto, userDetails.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRating(@PathVariable @Positive Integer id,
                                             @AuthenticationPrincipal AppUserDetails userDetails) {
        ratingService.deleteRating(id, userDetails.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
