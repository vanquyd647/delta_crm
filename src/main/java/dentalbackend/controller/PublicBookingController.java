package dentalbackend.controller;

import dentalbackend.application.publicbooking.PublicBookingUseCase;
import dentalbackend.dto.ConsultationRequest;
import dentalbackend.dto.QuickBookingRequest;
import dentalbackend.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Validated
public class PublicBookingController {
    private final PublicBookingUseCase publicBookingUseCase;

    @PostMapping("/consultation")
    public ApiResponse<String> submitConsultation(@Valid @RequestBody ConsultationRequest req) {
        publicBookingUseCase.submitConsultation(req);
        return ApiResponse.ok("Consultation request submitted", null);
    }

    @PostMapping("/quick-booking")
    public ApiResponse<?> quickBook(@Valid @RequestBody QuickBookingRequest req) {
        return ApiResponse.ok(publicBookingUseCase.quickBook(req));
    }
}

