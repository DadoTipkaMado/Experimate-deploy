package hr.tvz.experimate.experimate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ExploreController {

    @GetMapping("/explore")
    public String explore(Model model) {
        model.addAttribute("currentPage", "explore");
        return "explore";
    }
}