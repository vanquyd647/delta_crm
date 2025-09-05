package dentalbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import dentalbackend.application.chat.ChatUseCase;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatUseCase {

    private final ChatClient chatClient;

    @Override
    public String generate(String userMessage, String systemPrompt) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            spec = spec.system(systemPrompt);
        }

        return spec
                .user(userMessage)
                .call()
                .content();
    }
}
