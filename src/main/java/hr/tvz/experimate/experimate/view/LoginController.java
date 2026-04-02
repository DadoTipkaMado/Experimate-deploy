package hr.tvz.experimate.experimate.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Model model){
        model.addAttribute("currentPage", "login");
        return  "login";
    }

}
