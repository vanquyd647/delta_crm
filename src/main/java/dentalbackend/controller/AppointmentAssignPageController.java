package dentalbackend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/appointment-assign","/admin/appointment-assign"})
public class AppointmentAssignPageController {
    @GetMapping
    public String page() {
        return "appointment-assign";
    }
}
