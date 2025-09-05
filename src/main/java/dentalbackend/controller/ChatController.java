package dentalbackend.controller;


import dentalbackend.application.chat.ChatUseCase;
import dentalbackend.dto.ChatRequest;
import dentalbackend.dto.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatUseCase chatService;

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse generate(@Valid @RequestBody ChatRequest req) {
        String reply = chatService.generate(req.getMessage(), req.getSystem());
        return new ChatResponse(reply);
    }
}
