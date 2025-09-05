package dentalbackend.controller;

import dentalbackend.domain.Payment;
import dentalbackend.application.payment.PaymentUseCase;
import dentalbackend.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentUseCase paymentService;

    // Staff: Record payment for an appointment
    @PreAuthorize("hasRole('ROLE_RECEPTIONIST') or hasRole('ROLE_ADMIN')")
    @PostMapping("/record")
    public ApiResponse<Payment> recordPayment(@RequestParam Long appointmentId,
                                              @RequestParam Double amount,
                                              @RequestParam String method,
                                              @RequestParam(required = false) String invoiceNumber) {
        return ApiResponse.ok(paymentService.recordPayment(appointmentId, amount, method, invoiceNumber));
    }

    // Staff/Admin: Get payments for an appointment
    @PreAuthorize("hasRole('ROLE_RECEPTIONIST') or hasRole('ROLE_ADMIN')")
    @GetMapping("/appointment/{appointmentId}")
    public ApiResponse<java.util.List<Payment>> getPaymentsForAppointment(@PathVariable Long appointmentId) {
        return ApiResponse.ok(paymentService.getPaymentsForAppointment(appointmentId));
    }

    // Staff/Admin: Get payment by id (for invoice)
    @PreAuthorize("hasRole('ROLE_RECEPTIONIST') or hasRole('ROLE_ADMIN')")
    @GetMapping("/{paymentId}")
    public ApiResponse<Payment> getPayment(@PathVariable Long paymentId) {
        return ApiResponse.ok(paymentService.getPayment(paymentId));
    }
}
