package dentalbackend.controller;

import dentalbackend.domain.Appointment;
import dentalbackend.domain.UserEntity;
import dentalbackend.repository.AppointmentRepository;
import dentalbackend.repository.BranchRepository;
import dentalbackend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    public AppointmentController(AppointmentRepository appointmentRepository,
                                 UserRepository userRepository,
                                 BranchRepository branchRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
    }

    // Return a minimal list of appointments with relation info for UI selects
    @GetMapping
    public ResponseEntity<List<Map<String,Object>>> list() {
        List<Appointment> list = appointmentRepository.findAllWithRelations();
        List<Map<String,Object>> out = list.stream().map(a -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", a.getId());
            String label = "#" + a.getId();
            if (a.getCustomer() != null) label += " - " + (a.getCustomer().getFullName() != null ? a.getCustomer().getFullName() : a.getCustomer().getUsername());
            if (a.getScheduledTime() != null) label += " - " + a.getScheduledTime().toString();
            m.put("label", label);
            m.put("customerId", a.getCustomer() != null ? a.getCustomer().getId() : null);
            m.put("customerName", a.getCustomer() != null ? a.getCustomer().getFullName() : null);
            m.put("dentistId", a.getDentist() != null ? a.getDentist().getId() : null);
            m.put("dentistName", a.getDentist() != null ? a.getDentist().getFullName() : null);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<Appointment> assign(@PathVariable Long id,
                                              @RequestParam(required = false) Long assistantId,
                                              @RequestParam(required = false) Long branchId,
                                              @RequestParam(required = false) Integer estimatedMinutes) {
        Optional<Appointment> ap = appointmentRepository.findById(id);
        if (ap.isEmpty()) return ResponseEntity.notFound().build();
        Appointment a = ap.get();
        if (assistantId != null) a.setAssistant(userRepository.findById(assistantId).orElse(null));
        if (branchId != null) a.setBranch(branchRepository.findById(branchId).orElse(null));
        if (estimatedMinutes != null) a.setEstimatedMinutes(estimatedMinutes);
        appointmentRepository.save(a);
        return ResponseEntity.ok(a);
    }

    // Admin: delete appointment
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String,Object>> delete(@PathVariable Long id) {
        if (!appointmentRepository.existsById(id)) {
            return ResponseEntity.status(404).body(java.util.Map.of("success", false, "message", "Appointment not found"));
        }
        appointmentRepository.deleteById(id);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Appointment deleted"));
    }
}
