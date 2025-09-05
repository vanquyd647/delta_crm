package dentalbackend.controller;


import dentalbackend.application.chat.ChatUseCase;
import dentalbackend.application.service.ServiceUseCase;
import dentalbackend.dto.ChatRequest;
import dentalbackend.dto.ChatResponse;
import dentalbackend.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatUseCase chatService;
    private final ServiceUseCase serviceUseCase;

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
