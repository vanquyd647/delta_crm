package dentalbackend.controller;


import dentalbackend.application.chat.ChatUseCase;
import dentalbackend.application.dentist.DentistUseCase;
import dentalbackend.application.publicbooking.PublicBookingUseCase;
import dentalbackend.application.service.ServiceUseCase;
import dentalbackend.dto.ChatRequest;
import dentalbackend.dto.ChatResponse;
import dentalbackend.dto.QuickBookingRequest;
import dentalbackend.dto.ServiceResponse;
import dentalbackend.dto.DentistResponse;
import dentalbackend.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatUseCase chatService;
    private final ServiceUseCase serviceUseCase;
    private final DentistUseCase dentistUseCase;
    private final PublicBookingUseCase publicBookingUseCase;

    @Value("${app.chat.default-system:}")
    private String defaultSystem;

    @Value("${app.chat.include-services:false}")
    private boolean includeServices;

    @Value("${app.chat.include-services-full:false}")
    private boolean includeServicesFull;

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse generate(@Valid @RequestBody ChatRequest req) {
        String reply = chatService.generate(req.getMessage(), req.getSystem());
        return new ChatResponse(reply);
    }

    // New endpoint: provide booking assistance suggestions based on user message
    @PostMapping(value = "/assist", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> assist(@Valid @RequestBody ChatRequest req) {
        Map<String, Object> result = new HashMap<>();
        String userMessage = req.getMessage() == null ? "" : req.getMessage().toLowerCase();

        try {
            // Fetch available services and dentists
            List<ServiceResponse> services = Collections.emptyList();
            try {
                services = serviceUseCase.listAll();
            } catch (Exception ignored) {}

            List<DentistResponse> dentists = Collections.emptyList();
            try {
                dentists = dentistUseCase.listActive();
            } catch (Exception ignored) {}

            // Simple keyword match to find relevant services
            List<ServiceResponse> matched = new ArrayList<>();
            if (services != null && !services.isEmpty()) {
                for (ServiceResponse s : services) {
                    String name = s.getName() == null ? "" : s.getName().toLowerCase();
                    String desc = s.getDescription() == null ? "" : s.getDescription().toLowerCase();
                    if (!userMessage.isBlank() && (userMessage.contains(name) || Arrays.stream(userMessage.split("\\s+")).anyMatch(tok -> name.contains(tok) || desc.contains(tok)))) {
                        matched.add(s);
                    }
                }
            }

            // If no matched services, suggest top 3 services as common options
            List<ServiceResponse> suggestedServices;
            if (matched.isEmpty()) {
                suggestedServices = services == null ? Collections.emptyList() : services.stream().limit(3).collect(Collectors.toList());
            } else {
                suggestedServices = matched.stream().limit(5).collect(Collectors.toList());
            }

            // Prepare dentist suggestions (top 3 active)
            List<DentistResponse> suggestedDentists = dentists == null ? Collections.emptyList() : dentists.stream().limit(3).collect(Collectors.toList());

            // Prepare suggested dates (next 3 days) and timeslots
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            List<String> suggestedDates = new ArrayList<>();
            LocalDate start = LocalDate.now().plusDays(1);
            for (int i = 0; i < 3; i++) {
                suggestedDates.add(start.plusDays(i).format(dateFmt));
            }
            List<String> suggestedTimes = Arrays.asList("09:00", "11:00", "14:00", "16:00");

            // Build quick booking templates (one per suggested service x date/time sample)
            List<Map<String, Object>> quickTemplates = new ArrayList<>();
            for (ServiceResponse s : suggestedServices) {
                // pick first date/time combination
                if (suggestedDates.isEmpty() || suggestedTimes.isEmpty()) break;
                Map<String, Object> tmpl = new HashMap<>();
                tmpl.put("serviceId", s.getId());
                tmpl.put("serviceName", s.getName());
                tmpl.put("price", s.getPrice());
                tmpl.put("preferredDentistId", suggestedDentists.isEmpty() ? null : suggestedDentists.get(0).getId());
                tmpl.put("date", suggestedDates.get(0));
                tmpl.put("time", suggestedTimes.get(0));
                tmpl.put("notes", "Đặt qua chatbot - vui lòng xác nhận nếu muốn tiến hành");
                quickTemplates.add(tmpl);
            }

            result.put("messageSummary", "Tôi đã phân tích yêu cầu của bạn và gợi ý các dịch vụ/nha sĩ phù hợp.");
            result.put("suggestedServices", suggestedServices);
            result.put("suggestedDentists", suggestedDentists);
            result.put("suggestedDates", suggestedDates);
            result.put("suggestedTimes", suggestedTimes);
            result.put("quickBookingTemplates", quickTemplates);
            result.put("rawUserMessage", req.getMessage());

            return ApiResponse.ok(result);
        } catch (Exception e) {
            // ApiResponse.error only accepts a single message string; include a short error code in the message
            return ApiResponse.error("assist_failed: Unable to generate booking suggestions: " + e.getMessage());
        }
    }

    // New endpoint: perform quick booking (for chat flow) - forwards to public booking use case
    @PostMapping(value = "/book", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<?> book(@Valid @RequestBody QuickBookingRequest req) {
        try {
            var appt = publicBookingUseCase.quickBook(req);
            return ApiResponse.ok(appt);
        } catch (Exception e) {
            // include a short error code in the single message string
            return ApiResponse.error("booking_failed: " + e.getMessage());
        }
    }

    // Debug endpoint to inspect assembled system prompt and services used
    @PostMapping(value = "/debug", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> debug(@Valid @RequestBody ChatRequest req) {
        String sys = (req.getSystem() != null && !req.getSystem().isBlank()) ? req.getSystem() : defaultSystem;

        Map<String, Object> result = new HashMap<>();
        result.put("defaultSystem", defaultSystem);
        result.put("systemProvided", req.getSystem());

        if (includeServices) {
            try {
                List<?> services = serviceUseCase.listAll();
                if (services != null && !services.isEmpty()) {
                    if (includeServicesFull) {
                        result.put("servicesJson", services);
                        sys = (sys == null) ? "" : sys;
                        sys = sys + "\nServices JSON included.";
                    } else {
                        result.put("servicesSummary", services);
                        sys = (sys == null) ? "" : sys;
                        sys = sys + "\nAvailable services included.";
                    }
                }
            } catch (Exception e) {
                result.put("servicesError", e.getMessage());
            }
        }

        result.put("systemToSend", sys);
        result.put("userMessage", req.getMessage());
        return ApiResponse.ok(result);
    }
}
