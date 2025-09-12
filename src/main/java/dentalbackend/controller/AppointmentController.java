package dentalbackend.controller;

import dentalbackend.application.appointment.AppointmentUseCase;
import dentalbackend.application.dentist.DentistUseCase;
import dentalbackend.dto.CreateAppointmentRequest;
import dentalbackend.dto.UpdateAppointmentRequest;
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
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentUseCase service; // depend on port (DDD)
    private final UserRepository userRepo;
    private final DentistUseCase dentistUseCase;
    private final JdbcTemplate jdbc; // used by admin diagnostic endpoint

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
        Long userId = userRepo.findByUsernameOrEmail(principal.getUsername())
                .orElseThrow().getId();

        // 1) Try using the user id (Appointment.dentist is usually a UserEntity -> uses user id)
        var list = service.dentistAppointments(userId);
        int initialCount = list == null ? 0 : list.size();
        log.info("myAppointments: tried userId={} returnedCount={}", userId, initialCount);

        // 2) If empty, try resolving Dentist entity by userId and query by the Dentist entity id (legacy cases)
        if (list == null || list.isEmpty()) {
            var dentist = dentistUseCase.findByUserId(userId);
            if (dentist != null) {
                log.info("myAppointments: found dentist record for userId={} -> dentist.id={} dentist.userId={}", userId, dentist.getId(), dentist.getUserId());

                // Try dentist table id (some data may have dentist_id set to dentists.id)
                if (dentist.getId() != null) {
                    list = service.dentistAppointments(dentist.getId());
                    log.info("myAppointments: tried dentist.id={} returnedCount={}", dentist.getId(), list == null ? 0 : list.size());
                }
                // If still empty, try the linked user id stored on Dentist (defensive)
                if ((list == null || list.isEmpty()) && dentist.getUserId() != null) {
                    list = service.dentistAppointments(dentist.getUserId());
                    log.info("myAppointments: tried dentist.userId={} returnedCount={}", dentist.getUserId(), list == null ? 0 : list.size());
                }
            } else {
                log.info("myAppointments: no dentist profile found for userId={}", userId);
            }
        }

        // Final debug summary
        int finalCount = list == null ? 0 : list.size();
        log.info("myAppointments: finalCount={} for userId={}", finalCount, userId);

        return ApiResponse.ok(list);
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
        return ApiResponse.ok(service.getAppointmentForUser(id, requester.getId()));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}")
    public ApiResponse<AppointmentResponse> update(@AuthenticationPrincipal UserDetails principal,
                                         @PathVariable Long id,
                                         @RequestBody UpdateAppointmentRequest updateReq) {
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/mismatches")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> findMismatchedAppointments() {
        // Find appointments where dentist_id does not match any users.id but matches dentists.id
        String sql = "SELECT a.id AS appt_id, a.dentist_id AS appt_dentist_id, d.id AS dentist_tbl_id, d.user_id AS dentist_user_id " +
                "FROM appointments a JOIN dentists d ON a.dentist_id = d.id LEFT JOIN users u ON a.dentist_id = u.id " +
                "WHERE u.id IS NULL";
        var rows = jdbc.queryForList(sql);
        return ApiResponse.ok(rows);
    }
}
