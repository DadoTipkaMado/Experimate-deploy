package hr.tvz.experimate.experimate.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingViewController {

    @GetMapping({"/", "/landing"})
    public String landing() {
        return "landing";
    }
}
