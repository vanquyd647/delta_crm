package dentalbackend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebPageController {
    @GetMapping("/booking")
    public String bookingPage() {
        return "booking";
    }

    @GetMapping("/consultation")
    public String consultationPage() {
        return "consultation";
    }

    @GetMapping("/appointments")
    public String appointmentsPage() {
        return "appointments";
    }

    @GetMapping({"/", "/index"})
    public String indexPage() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage() {
        return "reset-password";
    }

    // Admin page mappings were moved to AdminPagesController to centralize admin routes and avoid duplication.
}
