package hr.tvz.experimate.experimate.controller;

import hr.tvz.experimate.experimate.model.tour_listing.TourListingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TourListingViewController {

    private final TourListingService tourListingService;

    public TourListingViewController(TourListingService tourListingService) {
        this.tourListingService = tourListingService;
    }

    @GetMapping("/listings")
    public String listings(Model model) {
        model.addAttribute("listings", tourListingService.getAllListings());
        model.addAttribute("currentPage", "map"); // map tab ostaje aktivan — home screen
        return "tour-listings";
    }
}