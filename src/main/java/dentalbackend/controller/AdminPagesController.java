package dentalbackend.controller;

import dentalbackend.domain.Appointment;
import dentalbackend.repository.AppointmentRepository;
import dentalbackend.repository.ServiceRepository;
import dentalbackend.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPagesController {
    private final ServiceRepository serviceRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AppointmentRepository appointmentRepository;

    @GetMapping("/tools")
    public String adminTools() { return "admin-pages"; }

    @GetMapping("/customer-profile")
    public String customerProfile() { return "customer-profile"; }

    @GetMapping("/staff-profile")
    public String staffProfile() { return "staff-profile"; }

    @GetMapping("/supplier")
    public String supplier() { return "supplier"; }

    @GetMapping("/prescriptions")
    public String prescriptions() { return "prescriptions"; }

    @GetMapping("/user-groups")
    public String userGroups() { return "user-groups"; }

    @GetMapping("/branches")
    public String branches() { return "branches"; }

    @GetMapping("/lookups")
    public String lookups() { return "lookups"; }

    @GetMapping("/discounts")
    public String discounts() { return "discounts"; }

    // Admin management pages
    @GetMapping("")
    public String adminHome(Model model) {
        // redirect to dashboard view which will use server-side counts
        return "admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long serviceCount = serviceRepository.count();
        long dentistCount = staffProfileRepository.count();
        long apptCount = appointmentRepository.count();

        model.addAttribute("servicesCount", serviceCount);
        model.addAttribute("dentistsCount", dentistCount);
        model.addAttribute("appointmentsCount", apptCount);

        // recent 5 appointments (sorted by scheduledTime desc if present)
        List<Appointment> recent = appointmentRepository.findAllWithRelations().stream()
                .sorted(Comparator.comparing((Appointment a) -> a.getScheduledTime() == null ? 0L : a.getScheduledTime().toEpochMilli()).reversed())
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentAppointments", recent);
        return "admin/dashboard";
    }

    @GetMapping("/services")
    public String adminServices() { return "admin/services"; }

    @GetMapping("/dentists")
    public String adminDentists() { return "admin/dentists"; }

    @GetMapping("/staffs")
    public String adminStaffs() { return "admin/staffs"; }

    @GetMapping("/appointments")
    public String adminAppointments() { return "admin/appointments"; }

}
