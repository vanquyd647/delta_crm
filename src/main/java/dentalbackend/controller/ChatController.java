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
import org.springframework.web.client.RestTemplate;

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

    // optional ML service URL (e.g. http://localhost:5000)
    @Value("${ml.service.url:}")
    private String mlServiceUrl;

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse generate(@Valid @RequestBody ChatRequest req) {
        String reply = chatService.generate(req.getMessage(), req.getSystem());
        return new ChatResponse(reply);
    }

    /**
     * AI-powered chat assistant endpoint.
     *
     * Flow:
     * 1. Use OpenAI to analyze user intent (booking vs chitchat)
     * 2. If booking intent → fetch services/dentists + call ML service for smart recommendations
     * 3. If chitchat → return friendly AI response only
     *
     * Examples:
     * - "chào" → Chitchat response
     * - "tôi muốn khám răng" → Booking suggestions with services/dentists/dates
     */
    @PostMapping(value = "/assist", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> assist(@Valid @RequestBody ChatRequest req) {
        Map<String, Object> result = new HashMap<>();
        String userMessage = req.getMessage() == null ? "" : req.getMessage();

        try {
            // STEP 1: Use OpenAI to analyze user intent
            String intentAnalysisPrompt = String.format(
                "You are an intent classifier for a dental clinic chatbot.\n\n" +

                "BOOKING intent indicators (respond 'BOOKING'):\n" +
                "- Wants to book/schedule appointment: 'tôi muốn', 'đặt lịch', 'book', 'schedule'\n" +
                "- Asks about services: 'khám răng', 'tẩy trắng', 'nhổ răng', 'services', 'dịch vụ'\n" +
                "- Asks about prices/costs: 'giá', 'chi phí', 'bao nhiêu tiền', 'price', 'cost'\n" +
                "- Asks about dentists/doctors: 'nha sĩ', 'bác sĩ', 'dentist', 'doctor'\n" +
                "- Asks about availability: 'có thể', 'available', 'khi nào'\n\n" +

                "CHITCHAT intent indicators (respond 'CHITCHAT'):\n" +
                "- Greetings only: 'chào', 'hello', 'hi', 'xin chào'\n" +
                "- Thanks only: 'cảm ơn', 'thank you', 'thanks'\n" +
                "- General questions: 'giờ mở cửa', 'địa chỉ', 'opening hours', 'location'\n" +
                "- Goodbyes: 'tạm biệt', 'bye', 'goodbye'\n\n" +

                "Message: \"%s\"\n\n" +
                "Respond with ONLY ONE WORD - either 'BOOKING' or 'CHITCHAT':\n" +
                "Intent:",
                userMessage
            );

            String intent = chatService.generate(intentAnalysisPrompt, null).trim().toUpperCase();
            result.put("intent", intent);
            result.put("rawUserMessage", userMessage);

            // STEP 2A: Handle CHITCHAT - just friendly AI response
            if (intent.contains("CHITCHAT")) {
                String chitchatPrompt = String.format(
                    "You are a friendly dental clinic receptionist. " +
                    "Respond naturally and briefly to this message. " +
                    "If they greet you, greet back and ask how you can help.\n\n" +
                    "User: %s\n\n" +
                    "Assistant:",
                    userMessage
                );
                String aiReply = chatService.generate(chitchatPrompt, null);

                result.put("type", "chitchat");
                result.put("reply", aiReply);
                result.put("messageSummary", aiReply);

                return ApiResponse.ok(result);
            }

            // STEP 2B: Handle BOOKING intent - full analysis with ML
            result.put("type", "booking");

            // Use OpenAI to extract booking info from user message
            String extractionPrompt = String.format(
                "Extract booking information from this message. Return JSON format:\n" +
                "{\n" +
                "  \"service_keywords\": [\"keyword1\", \"keyword2\"],\n" +
                "  \"summary\": \"brief summary of what user wants\"\n" +
                "}\n\n" +
                "Message: \"%s\"\n\n" +
                "JSON:",
                userMessage
            );
            String extractedInfo = chatService.generate(extractionPrompt, null);
            result.put("aiExtraction", extractedInfo);

            // Fetch available services and dentists
            List<ServiceResponse> services = Collections.emptyList();
            try {
                services = serviceUseCase.listAll();
            } catch (Exception ignored) {}

            List<DentistResponse> dentists = Collections.emptyList();
            try {
                dentists = dentistUseCase.listActive();
            } catch (Exception ignored) {}

            // Call ML service for intelligent recommendations (if configured)
            List<ServiceResponse> suggestedServices = new ArrayList<>();
            if (mlServiceUrl != null && !mlServiceUrl.isBlank()) {
                try {
                    RestTemplate rt = new RestTemplate();
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("query", userMessage);
                    payload.put("top_k", 5);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mlResp = rt.postForObject(mlServiceUrl + "/recommend", payload, Map.class);

                    if (mlResp != null && mlResp.containsKey("results")) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> mlResults = (List<Map<String, Object>>) mlResp.get("results");
                        result.put("mlRecommendations", mlResults);

                        // Convert ML results to ServiceResponse (match by ID)
                        for (Map<String, Object> mlItem : mlResults) {
                            Object idObj = mlItem.get("id");
                            if (idObj != null) {
                                Long serviceId = idObj instanceof Integer ? ((Integer)idObj).longValue() : (Long)idObj;
                                services.stream()
                                    .filter(s -> s.getId().equals(serviceId))
                                    .findFirst()
                                    .ifPresent(suggestedServices::add);
                            }
                        }
                    }
                } catch (Exception ex) {
                    result.put("mlError", "ml_call_failed: " + ex.getMessage());
                }
            }

            // Fallback: if ML didn't return services or not configured, suggest top 3
            if (suggestedServices.isEmpty()) {
                suggestedServices = services == null ? Collections.emptyList() :
                    services.stream().limit(3).collect(Collectors.toList());
            }

            // Prepare dentist suggestions (top 3 active)
            List<DentistResponse> suggestedDentists = dentists == null ? Collections.emptyList() :
                dentists.stream().limit(3).collect(Collectors.toList());

            // Prepare suggested dates (next 3 days) and timeslots
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            List<String> suggestedDates = new ArrayList<>();
            LocalDate start = LocalDate.now().plusDays(1);
            for (int i = 0; i < 3; i++) {
                suggestedDates.add(start.plusDays(i).format(dateFmt));
            }
            List<String> suggestedTimes = Arrays.asList("09:00", "11:00", "14:00", "16:00");

            // Build quick booking templates
            List<Map<String, Object>> quickTemplates = new ArrayList<>();
            for (ServiceResponse s : suggestedServices) {
                if (suggestedDates == null || suggestedTimes == null || suggestedDates.isEmpty() || suggestedTimes.isEmpty()) break;
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

            // Generate friendly summary using AI
            String summaryPrompt = String.format(
                "User asked: \"%s\"\n" +
                "We found %d matching services. " +
                "Write a brief, friendly message (1-2 sentences in Vietnamese) telling them we found these services and they can choose one to book.\n\n" +
                "Response:",
                userMessage, suggestedServices.size()
            );
            String aiSummary = chatService.generate(summaryPrompt, null);

            result.put("messageSummary", aiSummary);
            result.put("suggestedServices", suggestedServices);
            result.put("suggestedDentists", suggestedDentists);
            result.put("suggestedDates", suggestedDates);
            result.put("suggestedTimes", suggestedTimes);
            result.put("quickBookingTemplates", quickTemplates);

            return ApiResponse.ok(result);
        } catch (Exception e) {
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
