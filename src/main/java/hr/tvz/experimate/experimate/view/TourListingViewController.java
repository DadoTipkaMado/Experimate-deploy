package hr.tvz.experimate.experimate.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TourListingViewController {

    @GetMapping("/tours")
    public String tours() {
        return "tours";
    }

    @GetMapping("/listings/new")
    public String newListing() {
        return "listings-new";
    }
}
