package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.tour_listing.CreateTourListingDto;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingResponse;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingService;
import hr.tvz.experimate.experimate.model.tour_listing.UpdateTourListingDto;
import hr.tvz.experimate.experimate.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/api/tour-listing")
public class TourListingController {

    private final TourListingService tourListingService;

    public TourListingController(TourListingService tourListingService) {
        this.tourListingService = tourListingService;
    }

    @GetMapping
    public ResponseEntity<List<TourListingResponse>> getAllTourListings(@AuthenticationPrincipal AppUserDetails userDetails) {
        return ResponseEntity.ok(
                tourListingService.getAllListings(userDetails.getId())
        );
    }

    @GetMapping(value="/{id}")
    public ResponseEntity<TourListingResponse> getTourListingById(@PathVariable Integer id) {
        return tourListingService.getListingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TourListingResponse> createTourListing(@RequestBody CreateTourListingDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                tourListingService.createListing(dto)
        );
    }

    @PatchMapping(value="/{id}")
    public ResponseEntity<TourListingResponse> patchTourListing(@PathVariable Integer id,
                                                        @RequestBody UpdateTourListingDto dto) {
        return ResponseEntity.ok(
                tourListingService.updateListing(id, dto)
        );
    }

    @DeleteMapping(value="/{id}")
    public ResponseEntity<Void>  deleteTourListing(@PathVariable Integer id) {
        tourListingService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }
}
