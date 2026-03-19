package hr.tvz.experimate.experimate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountViewController {

    @GetMapping("/account")
    public String account(Model model) {
        model.addAttribute("currentPage", "account");
        return "account";
    }
}