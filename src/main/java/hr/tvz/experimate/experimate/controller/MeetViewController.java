package hr.tvz.experimate.experimate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MeetViewController {

    @GetMapping("/meet")
    public String meet(Model model) {
        model.addAttribute("currentPage", "meet");
        return "meet";
    }
}