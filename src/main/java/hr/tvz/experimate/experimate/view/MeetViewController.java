package hr.tvz.experimate.experimate.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class MeetViewController {

    @GetMapping("/meet")
    public RedirectView meet() {
        return new RedirectView("/tours");
    }
}
