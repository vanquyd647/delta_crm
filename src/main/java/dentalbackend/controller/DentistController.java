package dentalbackend.controller;

import dentalbackend.application.dentist.DentistUseCase;
import dentalbackend.application.user.UserUseCase;
import dentalbackend.common.ApiResponse;
import dentalbackend.dto.CreateDentistRequest;
import dentalbackend.dto.DentistResponse;
import dentalbackend.dto.UpdateDentistRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/dentists")
@RequiredArgsConstructor
public class DentistController {
    private final DentistUseCase dentistService;
    private final UserUseCase userUseCase;

    // Public: list active dentists
    @GetMapping
    public ApiResponse<List<DentistResponse>> listActive() {
        return ApiResponse.ok(dentistService.listActive());
    }

    // Admin: list all dentists including inactive
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ApiResponse<List<DentistResponse>> listAll() {
        return ApiResponse.ok(dentistService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<DentistResponse> getById(@PathVariable Long id) {
        var d = dentistService.getById(id);
        if (d == null) return ApiResponse.error("Dentist not found");
        return ApiResponse.ok(d);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<DentistResponse> create(@Valid @RequestBody CreateDentistRequest req) {
        return ApiResponse.ok(dentistService.create(req));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<DentistResponse> update(@PathVariable Long id, @RequestBody UpdateDentistRequest req) {
        return ApiResponse.ok(dentistService.update(id, req));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        dentistService.delete(id);
        return ApiResponse.ok("Dentist deleted", null);
    }

    // Get dentist profile linked to currently authenticated user
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ApiResponse<DentistResponse> myDentist(@AuthenticationPrincipal UserDetails principal) {
        var user = userUseCase.findByUsernameOrEmail(principal.getUsername()).orElse(null);
        if (user == null) return ApiResponse.error("User not found");
        var d = dentistService.findByUserId(user.getId());
        if (d == null) return ApiResponse.error("Dentist profile not found for current user");
        return ApiResponse.ok(d);
    }
}

