package hr.tvz.experimate.experimate.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TourListingViewController {

    @GetMapping("/tours")
    public String tours(Model model) {
        model.addAttribute("currentPage", "tours");
        return "tours";
    }

    @GetMapping("/listings/new")
    public String newListing(Model model) {
        model.addAttribute("currentPage", "tours");
        return "listings-new";
    }
}
