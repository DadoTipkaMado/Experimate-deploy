package hr.tvz.experimate.experimate.view;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthViewController {

    /**
     * Google Identity Services delivers the sign-in credential from its popup back to the
     * opener page via {@code window.postMessage}. Recent Chromium versions sever the
     * opener relationship under the default Cross-Origin-Opener-Policy, which surfaces as
     * "Cross-Origin-Opener-Policy policy would block the window.postMessage call" in the
     * console and an empty credential (→ 400 from /api/auth/google). Declaring
     * {@code same-origin-allow-popups} on the pages that host the Google button puts them
     * in a COOP group that keeps the popup connected, so the credential is delivered.
     * Only login/register run GSI, so the header is scoped to those two pages.
     */
    private void allowGooglePopup(HttpServletResponse response) {
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin-allow-popups");
    }

    @GetMapping("/login")
    public String login(HttpServletResponse response) {
        allowGooglePopup(response);
        return "login";
    }

    @GetMapping("/register")
    public String register(HttpServletResponse response) {
        allowGooglePopup(response);
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @GetMapping("/verify-email")
    public String verifyEmail() {
        return "verify-email";
    }
}
