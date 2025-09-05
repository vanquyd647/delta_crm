package dentalbackend.application.chat.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import dentalbackend.application.chat.ChatUseCase;
import dentalbackend.application.service.ServiceUseCase;
import dentalbackend.dto.ServiceResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatUseCase {

    private final ObjectMapper om = new ObjectMapper();
    private final ServiceUseCase serviceUseCase;

    // Read configuration keys. Support both spring.ai.openai.* and legacy OPENAI_* env vars
    @Value("${spring.ai.openai.base-url:${OPENAI_BASE_URL:}}")
    private String openAiBaseUrl;

    @Value("${spring.ai.openai.api-key:${OPENAI_API_KEY:}}")
    private String openAiKey;

    @Value("${spring.ai.openai.chat.options.model:${OPENAI_CHAT_MODEL:gpt-3.5-turbo}}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:${OPENAI_CHAT_TEMPERATURE:0.7}}")
    private double temperature;

    @Value("${spring.ai.openai.chat.max-tokens:512}")
    private int maxTokens;

    @Value("${spring.ai.openai.chat.completions-path:/v1/chat/completions}")
    private String completionsPath;

    // New properties to control default system prompt and whether to include services data
    @Value("${app.chat.default-system:}")
    private String defaultSystem;

    @Value("${app.chat.include-services:false}")
    private boolean includeServices;

    // new flag: include full services JSON in system prompt
    @Value("${app.chat.include-services-full:false}")
    private boolean includeServicesFull;

    private WebClient webClient() {
        String base = (openAiBaseUrl != null && !openAiBaseUrl.isBlank()) ? openAiBaseUrl : "https://api.openai.com";
        WebClient.Builder b = WebClient.builder().baseUrl(base).defaultHeader("Content-Type", "application/json");
        if (openAiKey != null && !openAiKey.isBlank()) {
            b.defaultHeader("Authorization", "Bearer " + openAiKey);
        }
        return b.build();
    }

    @Override
    public String generate(String userMessage, String systemPrompt) {
        try {
            // if client didn't provide systemPrompt, use default from config
            String sys = (systemPrompt != null && !systemPrompt.isBlank()) ? systemPrompt : defaultSystem;

            // optionally include a short summary of available services or full JSON
            if (includeServices) {
                try {
                    List<ServiceResponse> services = serviceUseCase.listAll();
                    if (services != null && !services.isEmpty()) {
                        if (includeServicesFull) {
                            // append full JSON representation
                            String json = om.writeValueAsString(Map.of("table", "services", "rows", services));
                            if (sys == null || sys.isBlank()) sys = "";
                            sys = sys + "\nServices JSON: " + json;
                        } else {
                            String summary = services.stream()
                                    .map(s -> s.getName() + " (" + s.getDurationMinutes() + "m)"
                                            + (s.getPrice() != null ? " - " + s.getPrice() : ""))
                                    .limit(12)
                                    .collect(Collectors.joining(", "));
                            if (sys == null || sys.isBlank()) sys = "";
                            sys = sys + "\nAvailable services: " + summary;
                        }
                    }
                } catch (Exception e) {
                    // ignore service-loading errors and proceed
                }
            }

            List<Map<String, String>> messages = new ArrayList<>();
            if (sys != null && !sys.isBlank()) {
                Map<String, String> sysMap = new HashMap<>();
                sysMap.put("role", "system");
                sysMap.put("content", sys);
                messages.add(sysMap);
            }

            Map<String, String> user = new HashMap<>();
            user.put("role", "user");
            user.put("content", userMessage);
            messages.add(user);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("max_tokens", maxTokens);
            body.put("temperature", temperature);

            String resp = webClient()
                    .post()
                    .uri(completionsPath)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (resp == null) return "";
            JsonNode root = om.readTree(resp);
            // handle OpenAI-style response
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                if (message.isMissingNode() || message.path("content").isMissingNode()) {
                    // some providers return choices[0].text
                    return choices.get(0).path("text").asText("").trim();
                }
                return message.path("content").asText("").trim();
            }
            // fallback: try provider-specific fields
            if (root.has("result") && root.path("result").has("output")) {
                return root.path("result").path("output").toString();
            }
            return "";
        } catch (Exception ex) {
            return "[Chatbot error: " + ex.getMessage() + "]";
        }
    }
}
