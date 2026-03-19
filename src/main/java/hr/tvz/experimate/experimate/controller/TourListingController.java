package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.tour_listing.CreateTourListingDto;
import hr.tvz.experimate.experimate.model.tour_listing.TourListing;
import hr.tvz.experimate.experimate.model.tour_listing.TourListingService;
import hr.tvz.experimate.experimate.model.tour_listing.UpdateTourListingDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/api/tour-listing")
class TourListingController {

    private final TourListingService tourListingService;

    public TourListingController(TourListingService tourListingService) {
        this.tourListingService = tourListingService;
    }

    @GetMapping
    public ResponseEntity<List<TourListing>> getAllTourListings() {
        return ResponseEntity.ok(
                tourListingService.getAllListings()
        );
    }

    @GetMapping(value="/{id}")
    public ResponseEntity<TourListing> getTourListingById(@PathVariable Integer id) {
        return tourListingService.getListingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TourListing> createTourListing(@RequestBody CreateTourListingDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                tourListingService.createListing(dto)
        );
    }

    @PatchMapping(value="/{id}")
    public ResponseEntity<TourListing> patchTourListing(@PathVariable Integer id,
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
