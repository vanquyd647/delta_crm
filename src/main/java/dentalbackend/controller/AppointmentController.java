package dentalbackend.controller;

import dentalbackend.application.appointment.AppointmentUseCase;
import dentalbackend.dto.CreateAppointmentRequest;
import dentalbackend.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import dentalbackend.repository.UserRepository;
import dentalbackend.dto.AppointmentResponse;
import dentalbackend.domain.UserEntity;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentUseCase service; // depend on port (DDD)
    private final UserRepository userRepo;

    @PreAuthorize("hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<?> create(@AuthenticationPrincipal UserDetails principal,
                                 @Valid @RequestBody CreateAppointmentRequest req) {
        // Use single-parameter repository method (username or email)
        UserEntity receptionist = userRepo.findByUsernameOrEmail(principal.getUsername())
                .orElseThrow();
        Long receptionistId = receptionist.getId();
        AppointmentResponse appt = service.create(req, receptionistId);

        // Log for debugging who created what
        log.info("Appointment created by user='{}' (id={}). appointmentId={}, customerId={}, dentistId={}",
                principal.getUsername(), receptionistId,
                appt.getId(),
                appt.getCustomerId(),
                appt.getDentistId()
        );

        return ApiResponse.ok(appt);
    }

    @PreAuthorize("hasRole('DENTIST')")
    @GetMapping("/my")
    public ApiResponse<?> myAppointments(@AuthenticationPrincipal UserDetails principal) {
        Long dentistId = userRepo.findByUsernameOrEmail(principal.getUsername())
                .orElseThrow().getId();
        return ApiResponse.ok(service.dentistAppointments(dentistId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/history")
    public ApiResponse<java.util.List<AppointmentResponse>> history(@AuthenticationPrincipal UserDetails principal) {
        Long userId = userRepo.findByUsernameOrEmail(principal.getUsername())
                .orElseThrow().getId();
        return ApiResponse.ok(service.customerAppointments(userId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> getById(@AuthenticationPrincipal UserDetails principal,
                                            @PathVariable Long id) {
        UserEntity requester = userRepo.findByUsernameOrEmail(principal.getUsername())
                .orElseThrow();
        return ApiResponse.ok(service.getAppointmentForUser(id, requester));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ApiResponse<AppointmentResponse> update(@AuthenticationPrincipal UserDetails principal,
                                         @PathVariable Long id,
                                         @RequestBody dentalbackend.domain.Appointment updateReq) {
        Long userId = userRepo.findByUsernameOrEmail(principal.getUsername())
                .orElseThrow().getId();
        return ApiResponse.ok(service.updateAppointment(id, updateReq, userId));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ApiResponse<String> cancel(@AuthenticationPrincipal UserDetails principal,
                                      @PathVariable Long id) {
        Long userId = userRepo.findByUsernameOrEmail(principal.getUsername())
                .orElseThrow().getId();
        service.cancelAppointment(id, userId);
        return ApiResponse.ok("Appointment cancelled", null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ApiResponse<java.util.List<AppointmentResponse>> all() {
        return ApiResponse.ok(service.allAppointments());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}")
    public ApiResponse<AppointmentResponse> adminUpdate(@PathVariable Long id,
                                                @RequestBody dentalbackend.domain.Appointment updateReq) {
        return ApiResponse.ok(service.adminUpdateAppointment(id, updateReq));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    public ApiResponse<String> adminDelete(@PathVariable Long id) {
        service.adminDeleteAppointment(id);
        return ApiResponse.ok("Appointment deleted", null);
    }
}
