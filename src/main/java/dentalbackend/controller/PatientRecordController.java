package dentalbackend.controller;

import dentalbackend.common.ApiResponse;
import dentalbackend.dto.PatientRecordRequest;
import dentalbackend.dto.PatientRecordResponse;
import dentalbackend.application.user.UserUseCase;
import dentalbackend.application.patient.PatientRecordUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientRecordController {
    private final PatientRecordUseCase service;
    private final UserUseCase userUseCase;

    @PreAuthorize("hasRole('DENTIST') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    @GetMapping("/{patientId}/records")
    public ApiResponse<List<PatientRecordResponse>> listByPatient(@PathVariable Long patientId) {
        return ApiResponse.ok(service.forPatient(patientId));
    }

    @PreAuthorize("hasRole('DENTIST') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    @PostMapping("/{patientId}/records")
    public ApiResponse<PatientRecordResponse> create(@PathVariable Long patientId,
                                                     @Valid @RequestBody PatientRecordRequest req,
                                                     @AuthenticationPrincipal UserDetails principal) {
        // receptionist/dentist identity is stored in req.dentistId or principal
        PatientRecordResponse created = service.create(patientId, req);
        return ApiResponse.ok(created);
    }

    @PreAuthorize("hasRole('DENTIST') or hasRole('ADMIN')")
    @PutMapping("/records/{id}")
    public ApiResponse<PatientRecordResponse> update(@PathVariable Long id,
                                                     @RequestBody PatientRecordRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @PreAuthorize("hasRole('DENTIST') or hasRole('ADMIN')")
    @DeleteMapping("/records/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Record deleted", null);
    }
}
