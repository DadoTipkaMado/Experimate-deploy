package hr.tvz.experimate.experimate.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommunityViewController {

    @GetMapping("/community")
    public String community() {
        return "community";
    }
}