package dentalbackend.controller;

import dentalbackend.common.ApiResponse;
import dentalbackend.application.admin.AdminUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminUseCase adminService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue/total")
    public ApiResponse<Double> totalRevenue() {
        return ApiResponse.ok(adminService.getTotalRevenue());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue/by-day")
    public ApiResponse<Map<java.time.LocalDate, Double>> revenueByDay() {
        return ApiResponse.ok(adminService.getRevenueByDay());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/service-usage")
    public ApiResponse<Map<String, Long>> serviceUsage() {
        return ApiResponse.ok(adminService.getServiceUsage());
    }
}
