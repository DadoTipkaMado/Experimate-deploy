package hr.tvz.experimate.experimate.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AccountViewController {

    @GetMapping("/account")
    public String account() { return "account"; }

    @GetMapping("/account/edit")
    public String accountEdit() { return "account-edit"; }

    @GetMapping("/requests")
    public RedirectView requests() { return new RedirectView("/tours"); }

    @GetMapping("/ratings")
    public String ratings() { return "ratings"; }

    @GetMapping("/profile/{username}")
    public String profile() { return "profile"; }

    @GetMapping("/settings")
    public String settings() { return "settings"; }

    @GetMapping("/onboarding")
    public String onboarding() { return "onboarding"; }
}
