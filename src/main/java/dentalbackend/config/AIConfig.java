package dentalbackend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Bean
    public ChatClient chatClient(

            @Qualifier("openAiChatModel") ChatModel chatModel
    ) {
        return ChatClient.builder(chatModel).build();
    }
}

