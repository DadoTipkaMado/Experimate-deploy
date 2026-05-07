package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.tour_listing.CreateTourListingDto;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingResponse;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingService;
import hr.tvz.experimate.experimate.model.tour_listing.UpdateTourListingDto;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/api/tour-listing")
@Validated
public class TourListingController {

    private final TourListingService tourListingService;

    public TourListingController(TourListingService tourListingService) {
        this.tourListingService = tourListingService;
    }

    @GetMapping("/mine")
    public ResponseEntity<List<TourListingResponse>> getMyListings(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false, defaultValue = "ASC") Sort.Direction direction) {
        return ResponseEntity.ok(
                tourListingService.getMyListings(userDetails.getId(), filter, direction)
        );
    }

    @GetMapping
    public ResponseEntity<List<TourListingResponse>> getAllTourListings(@AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(
                tourListingService.getAllListings(userDetails.getId())
        );
    }

    @GetMapping(value="/{id}")
    public ResponseEntity<TourListingResponse> getTourListingById(@PathVariable @Positive Integer id) {
        return tourListingService.getListingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TourListingResponse> createTourListing(@Valid @RequestBody CreateTourListingDto dto,
                                                                   @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                tourListingService.createListing(dto, userDetails.getId())
        );
    }

    @PatchMapping(value="/{id}")
    public ResponseEntity<TourListingResponse> patchTourListing(@PathVariable @Positive Integer id,
                                                                  @Valid @RequestBody UpdateTourListingDto dto,
                                                                  @AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(tourListingService.updateListing(id, dto, userDetails.getId()));
    }

    @DeleteMapping(value="/{id}")
    public ResponseEntity<Void> deleteTourListing(@PathVariable @Positive Integer id,
                                                   @AuthenticationPrincipal AppUserDetails userDetails) {
        tourListingService.deleteListing(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
