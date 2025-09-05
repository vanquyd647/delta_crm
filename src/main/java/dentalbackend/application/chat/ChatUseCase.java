package dentalbackend.application.chat;

public interface ChatUseCase {
    String generate(String userMessage, String systemPrompt);
}

