package hr.tvz.experimate.experimate.view;

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

    @GetMapping("/account/edit")
    public String accountEdit(Model model) {
        model.addAttribute("currentPage", "account");
        return "account-edit";
    }

    @GetMapping("/requests")
    public String requests(Model model) {
        model.addAttribute("currentPage", "account");
        return "requests";
    }

    @GetMapping("/ratings")
    public String ratings(Model model) {
        model.addAttribute("currentPage", "account");
        return "ratings";
    }

    @GetMapping("/profile/{username}")
    public String profile() {
        return "profile";
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("currentPage", "account");
        return "settings";
    }

    @GetMapping("/onboarding")
    public String onboarding() {
        return "onboarding";
    }
}