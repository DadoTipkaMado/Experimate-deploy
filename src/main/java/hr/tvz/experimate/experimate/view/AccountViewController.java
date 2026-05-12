package hr.tvz.experimate.experimate.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountViewController {

    @GetMapping("/account")
    public String account() { return "account"; }

    @GetMapping("/account/edit")
    public String accountEdit() { return "account-edit"; }

    @GetMapping("/requests")
    public String requests() { return "requests"; }

    @GetMapping("/ratings")
    public String ratings() { return "ratings"; }

    @GetMapping("/profile/{username}")
    public String profile() { return "profile"; }

    @GetMapping("/settings")
    public String settings() { return "settings"; }

    @GetMapping("/onboarding")
    public String onboarding() { return "onboarding"; }
}
