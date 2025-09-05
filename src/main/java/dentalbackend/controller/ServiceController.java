package dentalbackend.controller;

import dentalbackend.common.ApiResponse;
import dentalbackend.dto.CreateServiceRequest;
import dentalbackend.dto.UpdateServiceRequest;

import dentalbackend.application.service.ServiceUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {
    private final ServiceUseCase service;

    // Public: List all services
    @GetMapping
    public ApiResponse<java.util.List<dentalbackend.dto.ServiceResponse>> listAll() {
        return ApiResponse.ok(service.listAll());
    }

    // Public: Get service by id
    @GetMapping("/{id}")
    public ApiResponse<dentalbackend.dto.ServiceResponse> getById(@PathVariable Long id) {
        var result = service.getById(id);
        if (result == null) return ApiResponse.error("Service not found");
        return ApiResponse.ok(result);
    }

    // Admin: Create new service
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<dentalbackend.dto.ServiceResponse> create(@Valid @RequestBody CreateServiceRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    // Admin: Update service
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<dentalbackend.dto.ServiceResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateServiceRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    // Admin: Delete service
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Service deleted", null);
    }
}
